package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.exception.ConflitoHorarioException;
import br.com.leotricano.sistemasalao.exception.RecursoNaoEncontradoException;
import br.com.leotricano.sistemasalao.exception.RegraNegocioException;
import br.com.leotricano.sistemasalao.model.*;
import br.com.leotricano.sistemasalao.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Regras de agendamento (RF03): criar a agenda, mudar o status e incluir ou remover serviços.
 *
 * <p>Fluxo de uso hoje: primeiro {@link #criar} a agenda (cliente + data/hora), depois chamar
 * {@link #adicionarItem} uma vez para cada serviço.
 *
 * <p><b>Regra de conflito de horário (#12):</b> uma agenda ocupa todos os profissionais dela do
 * início ({@code dataHora}) até o fim (início + soma das durações dos serviços). Um profissional não
 * pode estar em duas agendas não canceladas cujos intervalos se sobreponham. Ver {@link #verificarConflito}.
 *
 * <p>Erros (#13): os métodos lançam {@link RecursoNaoEncontradoException} (404),
 * {@link RegraNegocioException} (400) ou {@link ConflitoHorarioException} (409); nunca devolvem {@code null}.
 *
 * <p>Pendências deste service:
 * <ul>
 *   <li>TODO(#14): regras de cancelar e remarcar (RF08). Remarcar e voltar de CANCELADO para
 *       AGENDADO também precisam chamar {@link #verificarConflito}.</li>
 *   <li>TODO(#28): criar o agendamento começando pelo profissional, com vários serviços de uma vez,
 *       e tratar serviços de profissionais diferentes em horários diferentes na mesma visita.</li>
 * </ul>
 */
@Service
public class AgendaService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final AgendaRepository agendaRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final ProfissionalRepository profissionalRepository;

    public AgendaService(AgendaRepository agendaRepository,
                          ClienteRepository clienteRepository,
                          ServicoRepository servicoRepository,
                          ProfissionalRepository profissionalRepository) {
        this.agendaRepository = agendaRepository;
        this.clienteRepository = clienteRepository;
        this.servicoRepository = servicoRepository;
        this.profissionalRepository = profissionalRepository;
    }

    public List<Agenda> listarTodas() {
        return agendaRepository.findAll();
    }

    /**
     * Busca pelo id ou lança 404. {@code orElseThrow} abre o Optional: se tiver a agenda, devolve;
     * se estiver vazio, lança a exceção criada pela função passada.
     *
     * @throws RecursoNaoEncontradoException se o id não existir (404)
     */
    public Agenda buscarPorId(Long id) {
        return agendaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agenda " + id + " não encontrada."));
    }

    /**
     * Cria uma agenda sem serviços. O JSON só traz o id do cliente ({@code {"cliente":{"id":1}}}),
     * então o cliente completo é buscado no banco e colocado na agenda antes de salvar.
     * Não há checagem de conflito aqui: sem itens, a agenda ainda não ocupa nenhum profissional.
     *
     * <p>TODO: o status inicial deveria ser sempre AGENDADO, definido aqui, e não vir do JSON.
     *
     * @throws RegraNegocioException se o cliente não foi informado ou não existe (400). É 400, e não
     *         404, porque o problema está no corpo enviado, não no endereço chamado.
     */
    public Agenda criar(Agenda agenda) {
        if (agenda.getCliente() == null || agenda.getCliente().getId() == null) {
            throw new RegraNegocioException("Informe o cliente do agendamento: {\"cliente\": {\"id\": ...}}.");
        }

        Long clienteId = agenda.getCliente().getId();
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RegraNegocioException("Cliente " + clienteId + " não encontrado."));

        agenda.setCliente(cliente);
        return agendaRepository.save(agenda);
    }

    /**
     * Apaga a agenda e, por causa do cascade, todos os itens dela.
     *
     * @throws RecursoNaoEncontradoException se o id não existir (404)
     * @throws org.springframework.dao.DataIntegrityViolationException se houver pagamentos registrados
     *         para ela (o TratadorDeErros responde 409)
     */
    public void deletar(Long id) {
        buscarPorId(id); // garante o 404; o deleteById sozinho não avisa quando o id não existe
        agendaRepository.deleteById(id);
    }

    /**
     * Troca o status (AGENDADO, CONCLUIDO, CANCELADO). Hoje qualquer troca é aceita.
     *
     * @throws RecursoNaoEncontradoException se o id não existir (404)
     */
    public Agenda atualizarStatus(Long id, StatusAgendamento novoStatus) {
        Agenda agenda = buscarPorId(id);
        agenda.setStatus(novoStatus);
        return agendaRepository.save(agenda);
    }

    /**
     * Inclui um serviço na agenda, feito por um profissional.
     *
     * <p>Regras aplicadas:
     * <ol>
     *   <li>agenda, serviço e profissional precisam existir (senão 404);</li>
     *   <li>o profissional precisa ter esse serviço na lista dele, na tabela profissional_servico (senão 400);</li>
     *   <li>com o serviço novo, a agenda não pode passar a sobrepor outro atendimento de nenhum dos
     *       profissionais dela (senão 409, #12);</li>
     *   <li>o preço atual do serviço é copiado para {@code precoCobrado}.</li>
     * </ol>
     *
     * @return a agenda com o novo item
     */
    public Agenda adicionarItem(Long agendaId, Long servicoId, Long profissionalId) {
        // Regra 1
        Agenda agenda = buscarPorId(agendaId);
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço " + servicoId + " não encontrado."));
        Profissional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional " + profissionalId + " não encontrado."));

        // Regra 2: procura o serviço na lista do profissional
        boolean profissionalFazOServico = false;

        for (Servico s : profissional.getServicos() ) {
            if (s.getId().equals(servico.getId())) {
                profissionalFazOServico = true;
            }
        }

        if (!profissionalFazOServico) {
            throw new RegraNegocioException(
                    profissional.getNome() + " não faz o serviço " + servico.getNome() + ".");
        }

        // Regra 3: como fica o intervalo da agenda depois de incluir este serviço.
        // Ex.: agenda das 13:00 com corte (60) termina 14:00; incluindo coloração (120), passa a terminar 16:00.
        LocalDateTime inicio = agenda.getDataHora();
        LocalDateTime fim = inicio.plusMinutes(agenda.calcularDuracaoTotalMinutos() + servico.getDuracaoMinutos());

        // Todos os profissionais da agenda são afetados, não só o do serviço novo: aumentar a duração
        // também estende o horário de quem já estava nela.
        Set<Long> profissionalIds = new HashSet<>();
        for (ItemAgendamento item : agenda.getItens()) {
            profissionalIds.add(item.getProfissional().getId());
        }
        profissionalIds.add(profissional.getId());

        verificarConflito(agenda.getId(), profissionalIds, inicio, fim);

        ItemAgendamento item = new ItemAgendamento();
        item.setAgenda(agenda);
        item.setServico(servico);
        item.setProfissional(profissional);
        item.setPrecoCobrado(servico.getPreco()); // Regra 4: congela o preço do dia

        agenda.getItens().add(item);

        // Salvar a agenda salva o item novo junto (cascade = ALL em Agenda.itens)
        return agendaRepository.save(agenda);
    }

    /**
     * Tira um serviço da agenda. Como Agenda.itens tem {@code orphanRemoval = true}, o item
     * removido da lista também é apagado do banco ao salvar. Não precisa checar conflito:
     * remover um serviço só encurta a agenda.
     *
     * @throws RecursoNaoEncontradoException se a agenda não existir, ou se o item não for dela (404)
     */
    public Agenda removerItem(Long agendaId, Long itemId) {
        Agenda agenda = buscarPorId(agendaId);

        // removeIf devolve true se tirou algum item; false quer dizer que o item não é desta agenda
        boolean removeu = agenda.getItens().removeIf(item -> item.getId().equals(itemId));
        if (!removeu) {
            throw new RecursoNaoEncontradoException("Item " + itemId + " não encontrado na agenda " + agendaId + ".");
        }

        return agendaRepository.save(agenda);
    }

    /**
     * Regra de conflito de horário (#12): lança exceção se algum dos profissionais já tiver outro
     * atendimento, não cancelado, que se sobreponha ao intervalo [inicio, fim).
     *
     * <p><b>Quando dois horários se sobrepõem?</b> Quando A começa antes de B terminar <i>e</i> B
     * começa antes de A terminar. Encostar não é conflito: um atendimento que termina às 14:00 e
     * outro que começa às 14:00 podem conviver.
     * <pre>
     *   existente:  14:00 |=========| 17:00
     *   novo:             15:00 |====| 16:00     → conflito
     *   novo:  13:00 |====| 14:00                → ok (só encosta)
     *   novo:                           17:00 |====| 18:00 → ok
     * </pre>
     *
     * <p>Funciona em duas etapas: o banco devolve só as agendas candidatas (mesmos profissionais,
     * não canceladas, começando antes de {@code fim}) e aqui se calcula o término de cada uma para
     * conferir a sobreposição. A janela de um dia antes do início limita a busca e supõe que nenhum
     * atendimento dura mais de 24 horas.
     *
     * <p>Fica separado em um método próprio para ser reaproveitado ao remarcar (#14).
     *
     * @param agendaIdIgnorado a agenda que está sendo alterada, que não conflita com ela mesma
     * @throws ConflitoHorarioException no primeiro conflito encontrado, com o nome do profissional e o horário
     */
    private void verificarConflito(Long agendaIdIgnorado, Set<Long> profissionalIds,
                                   LocalDateTime inicio, LocalDateTime fim) {
        List<Agenda> candidatas = agendaRepository.buscarPossiveisConflitos(
                profissionalIds, StatusAgendamento.CANCELADO, agendaIdIgnorado, fim, inicio.minusDays(1));

        for (Agenda outra : candidatas) {
            LocalDateTime outraInicio = outra.getDataHora();
            LocalDateTime outraFim = outra.calcularFim();

            boolean sobrepoe = inicio.isBefore(outraFim) && outraInicio.isBefore(fim);
            if (!sobrepoe) {
                continue;
            }

            // Acha qual dos profissionais está nas duas agendas, para a mensagem dizer quem é
            for (ItemAgendamento item : outra.getItens()) {
                Profissional ocupado = item.getProfissional();
                if (profissionalIds.contains(ocupado.getId())) {
                    throw new ConflitoHorarioException(String.format(
                            "%s já tem atendimento em %s das %s às %s (agenda %d, cliente %s).",
                            ocupado.getNome(),
                            outraInicio.format(DATA),
                            outraInicio.format(HORA),
                            outraFim.format(HORA),
                            outra.getId(),
                            outra.getCliente().getNome()));
                }
            }
        }
    }
}
