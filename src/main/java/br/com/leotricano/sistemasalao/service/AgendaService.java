package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.exception.ConflitoHorarioException;
import br.com.leotricano.sistemasalao.exception.RecursoNaoEncontradoException;
import br.com.leotricano.sistemasalao.exception.RegraNegocioException;
import br.com.leotricano.sistemasalao.model.*;
import br.com.leotricano.sistemasalao.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Regras de agendamento: consultar por período e profissional (RF04), criar (RF03),
 * incluir/remover serviços, mudar status, cancelar e remarcar (RF08).
 *
 * <p>Fluxo de uso hoje: primeiro {@link #criar} a agenda (cliente + data/hora), depois chamar
 * {@link #adicionarItem} uma vez para cada serviço.
 *
 * <p><b>Regra de conflito de horário (#12):</b> uma agenda ocupa todos os profissionais dela do
 * início ({@code dataHora}) até o fim (início + soma das durações dos serviços). Um profissional não
 * pode estar em duas agendas não canceladas cujos intervalos se sobreponham. É checada ao incluir
 * serviço, ao remarcar e ao reativar uma agenda cancelada. Ver {@link #verificarConflito}.
 *
 * <p><b>Ciclo de vida (#14):</b> toda agenda nasce AGENDADO. Só uma agenda AGENDADA pode ter
 * serviços alterados ou ser remarcada. As mudanças de status permitidas estão em
 * {@link #atualizarStatus} e no Javadoc de {@link StatusAgendamento}.
 *
 * <p>Erros (#13): os métodos lançam {@link RecursoNaoEncontradoException} (404),
 * {@link RegraNegocioException} (400) ou {@link ConflitoHorarioException} (409); nunca devolvem {@code null}.
 *
 * <p>TODO(#28): criar o agendamento começando pelo profissional, com vários serviços de uma vez,
 * e tratar serviços de profissionais diferentes em horários diferentes na mesma visita.
 */
@Service
public class AgendaService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /** Maior período aceito em {@link #listar} (dois meses), para ninguém pedir anos de agenda de uma vez. */
    private static final int MAXIMO_DIAS_CONSULTA = 62;

    private final AgendaRepository agendaRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final PagamentoRepository pagamentoRepository;

    public AgendaService(AgendaRepository agendaRepository,
                          ClienteRepository clienteRepository,
                          ServicoRepository servicoRepository,
                          ProfissionalRepository profissionalRepository,
                          PagamentoRepository pagamentoRepository) {
        this.agendaRepository = agendaRepository;
        this.clienteRepository = clienteRepository;
        this.servicoRepository = servicoRepository;
        this.profissionalRepository = profissionalRepository;
        this.pagamentoRepository = pagamentoRepository;
    }

    /**
     * Lista as agendas para a tela de agenda (#15, RF04). Todos os filtros são opcionais e podem
     * ser combinados; sem nenhum, devolve todas as agendas. O resultado vem ordenado por horário.
     *
     * <p>O período é de dias inteiros: {@code de=03/11, ate=03/11} é o dia 03/11 todo, e
     * {@code de=02/11, ate=08/11} é a semana. A API não decide o que é "dia" ou "semana";
     * quem chama manda o período, e o mesmo método serve para o mês do relatório depois.
     *
     * @param de             primeiro dia do período (inclusive) ou {@code null}
     * @param ate            último dia do período (inclusive) ou {@code null}
     * @param profissionalId só agendas com algum serviço desse profissional, ou {@code null}
     * @param status         só agendas nesse status, ou {@code null}
     * @throws RegraNegocioException (400) se só uma das datas for informada, se {@code de} for
     *         depois de {@code ate} ou se o período passar de {@value #MAXIMO_DIAS_CONSULTA} dias
     * @throws RecursoNaoEncontradoException (404) se o profissional informado não existir
     */
    public List<Agenda> listar(LocalDate de, LocalDate ate, Long profissionalId, StatusAgendamento status) {
        // Só uma das datas não define um período; melhor avisar do que adivinhar
        if ((de == null) != (ate == null)) {
            throw new RegraNegocioException("Informe as duas datas do período: 'de' e 'ate'.");
        }

        LocalDateTime inicio = null;
        LocalDateTime fim = null;
        if (de != null) {
            if (de.isAfter(ate)) {
                throw new RegraNegocioException("A data 'de' precisa ser igual ou anterior à data 'ate'.");
            }
            // +1 porque os dois dias contam: de 03/11 até 03/11 é 1 dia, não 0
            long dias = ChronoUnit.DAYS.between(de, ate) + 1;
            if (dias > MAXIMO_DIAS_CONSULTA) {
                throw new RegraNegocioException("O período pode ter no máximo " + MAXIMO_DIAS_CONSULTA
                        + " dias (foram pedidos " + dias + ").");
            }
            // De 00:00 do primeiro dia até 00:00 do dia seguinte ao último (sem incluir esse instante)
            inicio = de.atStartOfDay();
            fim = ate.plusDays(1).atStartOfDay();
        }

        // Profissional inexistente dá 404, em vez de uma lista vazia que pareceria "dia livre"
        if (profissionalId != null && !profissionalRepository.existsById(profissionalId)) {
            throw new RecursoNaoEncontradoException("Profissional " + profissionalId + " não encontrado.");
        }

        return agendaRepository.buscarComFiltros(inicio, fim, profissionalId, status);
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
     * Cria uma agenda sem serviços, sempre com status AGENDADO. O JSON só traz o id do cliente
     * ({@code {"cliente":{"id":1}}}), então o cliente completo é buscado no banco e colocado na
     * agenda antes de salvar. Não há checagem de conflito aqui: sem itens, a agenda ainda não
     * ocupa nenhum profissional.
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
        // Ignora o status que veio no JSON: criar já como CONCLUIDO ou CANCELADO pularia as regras do #14
        agenda.setStatus(StatusAgendamento.AGENDADO);
        return agendaRepository.save(agenda);
    }

    /**
     * Apaga a agenda e, por causa do cascade, todos os itens dela. Serve para corrigir um lançamento
     * errado; para desmarcar um atendimento, o certo é cancelar (o histórico fica guardado).
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
     * Muda o status seguindo o ciclo de vida do agendamento (#14):
     * <pre>
     *   AGENDADO  → CONCLUIDO   precisa ter pelo menos um serviço (pagamento pode vir depois)
     *   AGENDADO  → CANCELADO   não pode ter pagamento registrado
     *   CANCELADO → AGENDADO    reativar: o horário precisa continuar livre (#12)
     *   CONCLUIDO → qualquer    recusado: atendimento feito não se desfaz
     *   CANCELADO → CONCLUIDO   recusado: precisa reativar antes
     * </pre>
     * Pedir o mesmo status que a agenda já tem não muda nada e não dá erro.
     *
     * @throws RecursoNaoEncontradoException se o id não existir (404)
     * @throws RegraNegocioException se a mudança não for permitida (400)
     * @throws ConflitoHorarioException se, ao reativar, o horário já estiver ocupado (409)
     */
    public Agenda atualizarStatus(Long id, StatusAgendamento novoStatus) {
        Agenda agenda = buscarPorId(id);
        StatusAgendamento atual = agenda.getStatus();

        if (novoStatus == atual) {
            return agenda;
        }

        // enum pode ser comparado com == (cada valor existe uma única vez na memória)
        if (atual == StatusAgendamento.CONCLUIDO) {
            throw new RegraNegocioException("Atendimento concluído não pode mudar de status.");
        }

        if (atual == StatusAgendamento.CANCELADO && novoStatus == StatusAgendamento.CONCLUIDO) {
            throw new RegraNegocioException(
                    "Agendamento cancelado não pode ser concluído. Reative-o (status AGENDADO) antes.");
        }

        if (novoStatus == StatusAgendamento.CONCLUIDO && agenda.getItens().isEmpty()) {
            throw new RegraNegocioException("Não é possível concluir um agendamento sem serviços.");
        }

        if (novoStatus == StatusAgendamento.CANCELADO && pagamentoRepository.existsByAgendaId(id)) {
            throw new RegraNegocioException(
                    "Este agendamento tem pagamentos registrados e não pode ser cancelado. Remova os pagamentos antes.");
        }

        // Reativar: enquanto estava cancelada, outra cliente pode ter ocupado o horário
        if (atual == StatusAgendamento.CANCELADO && novoStatus == StatusAgendamento.AGENDADO) {
            verificarConflito(agenda.getId(), profissionaisDa(agenda), agenda.getDataHora(), agenda.calcularFim());
        }

        agenda.setStatus(novoStatus);
        return agendaRepository.save(agenda);
    }

    /**
     * Remarca o atendimento para outra data e hora, mantendo cliente, serviços e profissionais (RF08).
     *
     * <p>Regras:
     * <ol>
     *   <li>só agendas AGENDADAS podem ser remarcadas;</li>
     *   <li>a nova data e hora precisa estar no futuro;</li>
     *   <li>o novo horário precisa estar livre para todos os profissionais da agenda (#12).</li>
     * </ol>
     *
     * @throws RecursoNaoEncontradoException se o id não existir (404)
     * @throws RegraNegocioException se a regra 1 ou 2 falhar (400)
     * @throws ConflitoHorarioException se a regra 3 falhar (409)
     */
    public Agenda remarcar(Long id, LocalDateTime novaDataHora) {
        Agenda agenda = buscarPorId(id);

        exigirAgendado(agenda, "remarcar");

        if (!novaDataHora.isAfter(LocalDateTime.now())) {
            throw new RegraNegocioException("A nova data e hora precisa ser no futuro.");
        }

        LocalDateTime novoFim = novaDataHora.plusMinutes(agenda.calcularDuracaoTotalMinutos());
        verificarConflito(agenda.getId(), profissionaisDa(agenda), novaDataHora, novoFim);

        agenda.setDataHora(novaDataHora);
        return agendaRepository.save(agenda);
    }

    /**
     * Inclui um serviço na agenda, feito por um profissional.
     *
     * <p>Regras aplicadas:
     * <ol>
     *   <li>agenda, serviço e profissional precisam existir (senão 404);</li>
     *   <li>a agenda precisa estar AGENDADA (senão 400, #14);</li>
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

        // Regra 2
        exigirAgendado(agenda, "alterar os serviços de");

        // Regra 3: procura o serviço na lista do profissional
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

        // Regra 4: como fica o intervalo da agenda depois de incluir este serviço.
        // Ex.: agenda das 13:00 com corte (60) termina 14:00; incluindo coloração (120), passa a terminar 16:00.
        LocalDateTime inicio = agenda.getDataHora();
        LocalDateTime fim = inicio.plusMinutes(agenda.calcularDuracaoTotalMinutos() + servico.getDuracaoMinutos());

        // Todos os profissionais da agenda são afetados, não só o do serviço novo: aumentar a duração
        // também estende o horário de quem já estava nela.
        Set<Long> profissionalIds = profissionaisDa(agenda);
        profissionalIds.add(profissional.getId());

        verificarConflito(agenda.getId(), profissionalIds, inicio, fim);

        ItemAgendamento item = new ItemAgendamento();
        item.setAgenda(agenda);
        item.setServico(servico);
        item.setProfissional(profissional);
        item.setPrecoCobrado(servico.getPreco()); // Regra 5: congela o preço do dia

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
     * @throws RegraNegocioException se a agenda não estiver AGENDADA (400)
     */
    public Agenda removerItem(Long agendaId, Long itemId) {
        Agenda agenda = buscarPorId(agendaId);
        exigirAgendado(agenda, "alterar os serviços de");

        // removeIf devolve true se tirou algum item; false quer dizer que o item não é desta agenda
        boolean removeu = agenda.getItens().removeIf(item -> item.getId().equals(itemId));
        if (!removeu) {
            throw new RecursoNaoEncontradoException("Item " + itemId + " não encontrado na agenda " + agendaId + ".");
        }

        return agendaRepository.save(agenda);
    }

    /**
     * Garante que a agenda está AGENDADA antes de uma alteração. Agenda concluída é histórico e
     * agenda cancelada precisa ser reativada antes de mudar.
     *
     * @param acao o que se tentou fazer, para a mensagem (ex.: "remarcar")
     * @throws RegraNegocioException se o status for outro (400)
     */
    private void exigirAgendado(Agenda agenda, String acao) {
        if (agenda.getStatus() != StatusAgendamento.AGENDADO) {
            throw new RegraNegocioException("Só é possível " + acao + " um agendamento com status AGENDADO"
                    + " (status atual: " + agenda.getStatus() + ").");
        }
    }

    /** Ids dos profissionais que atendem nesta agenda (sem repetição, por ser um Set). */
    private Set<Long> profissionaisDa(Agenda agenda) {
        Set<Long> ids = new HashSet<>();
        for (ItemAgendamento item : agenda.getItens()) {
            ids.add(item.getProfissional().getId());
        }
        return ids;
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
     * <p>Usado ao incluir serviço, ao remarcar e ao reativar uma agenda cancelada.
     *
     * @param agendaIdIgnorado a agenda que está sendo alterada, que não conflita com ela mesma
     * @throws ConflitoHorarioException no primeiro conflito encontrado, com o nome do profissional e o horário
     */
    private void verificarConflito(Long agendaIdIgnorado, Set<Long> profissionalIds,
                                   LocalDateTime inicio, LocalDateTime fim) {
        // Agenda sem serviços não ocupa ninguém (e "in ()" vazio não é aceito em SQL)
        if (profissionalIds.isEmpty()) {
            return;
        }

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
