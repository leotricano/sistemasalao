package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.model.*;
import br.com.leotricano.sistemasalao.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Regras de agendamento (RF03): criar a agenda, mudar o status e incluir ou remover serviços.
 *
 * <p>Fluxo de uso hoje: primeiro {@link #criar} a agenda (cliente + data/hora), depois chamar
 * {@link #adicionarItem} uma vez para cada serviço.
 *
 * <p>Pendências deste service:
 * <ul>
 *   <li>TODO(#12): recusar item que faça o profissional ter dois atendimentos ao mesmo tempo.</li>
 *   <li>TODO(#14): regras de cancelar e remarcar (RF08).</li>
 *   <li>TODO(#28): criar o agendamento começando pelo profissional, com vários serviços de uma vez.</li>
 * </ul>
 */
@Service
public class AgendaService {

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

    /** @return a agenda, ou {@code null} se o id não existir */
    public Agenda buscarPorId(Long id) {
        // orElse(null): abre o Optional; se estiver vazio, devolve null
        return agendaRepository.findById(id).orElse(null);
    }

    /**
     * Cria uma agenda sem serviços. O JSON só traz o id do cliente ({@code {"cliente":{"id":1}}}),
     * então o cliente completo é buscado no banco e colocado na agenda antes de salvar.
     *
     * <p>TODO: o status inicial deveria ser sempre AGENDADO, definido aqui, e não vir do JSON.
     *
     * @return a agenda criada, ou {@code null} se o cliente não foi informado ou não existe
     */
    public Agenda criar(Agenda agenda) {
        if (agenda.getCliente() == null || agenda.getCliente().getId() == null) {
            return null;
        }

        Optional<Cliente> clienteOpt = clienteRepository.findById(agenda.getCliente().getId());
        if (clienteOpt.isEmpty()) {
            return null;
        }

        agenda.setCliente(clienteOpt.get());
        return agendaRepository.save(agenda);
    }

    /** Apaga a agenda e, por causa do cascade, todos os itens dela. */
    public void deletar(Long id) {
        agendaRepository.deleteById(id);
    }

    /**
     * Troca o status (AGENDADO, CONCLUIDO, CANCELADO). Hoje qualquer troca é aceita.
     *
     * @return a agenda atualizada, ou {@code null} se o id não existir
     */
    public Agenda atualizarStatus(Long id, StatusAgendamento novoStatus) {
        Optional<Agenda> agendaOpt = agendaRepository.findById(id);
        if (agendaOpt.isEmpty()) {
            return null;
        }

        Agenda agenda = agendaOpt.get();
        agenda.setStatus(novoStatus);
        return agendaRepository.save(agenda);
    }

    /**
     * Inclui um serviço na agenda, feito por um profissional.
     *
     * <p>Regras aplicadas:
     * <ol>
     *   <li>agenda, serviço e profissional precisam existir;</li>
     *   <li>o profissional precisa ter esse serviço na lista dele (tabela profissional_servico);</li>
     *   <li>o preço atual do serviço é copiado para {@code precoCobrado}.</li>
     * </ol>
     *
     * @return a agenda com o novo item, ou {@code null} se alguma regra falhar
     */
    public Agenda adicionarItem(Long agendaId, Long servicoId, Long profissionalId) {
        Optional<Agenda> agendaOpt = agendaRepository.findById(agendaId);
        Optional<Servico> servicoOpt = servicoRepository.findById(servicoId);
        Optional<Profissional> profissionalOpt = profissionalRepository.findById(profissionalId);

        if (agendaOpt.isEmpty() || servicoOpt.isEmpty() || profissionalOpt.isEmpty()) {
            return null;
        }

        Agenda agenda = agendaOpt.get();
        Servico servico = servicoOpt.get();
        Profissional profissional = profissionalOpt.get();

        // Regra 2: procura o serviço na lista do profissional
        boolean profissionalFazOServico = false;

        for (Servico s : profissional.getServicos() ) {
            if (s.getId().equals(servico.getId())) {
                profissionalFazOServico = true;
            }
        }

        if (!profissionalFazOServico) {
            return null;
        }

        // TODO(#12): antes de incluir, checar se o profissional já tem outro atendimento que se
        // sobreponha a [dataHora, dataHora + duração total] desta agenda.

        ItemAgendamento item = new ItemAgendamento();
        item.setAgenda(agenda);
        item.setServico(servico);
        item.setProfissional(profissional);
        item.setPrecoCobrado(servico.getPreco()); // Regra 3: congela o preço do dia

        agenda.getItens().add(item);

        // Salvar a agenda salva o item novo junto (cascade = ALL em Agenda.itens)
        return agendaRepository.save(agenda);
    }

    /**
     * Tira um serviço da agenda. Como Agenda.itens tem {@code orphanRemoval = true}, o item
     * removido da lista também é apagado do banco ao salvar.
     *
     * @return a agenda atualizada, ou {@code null} se a agenda não existir
     */
    public Agenda removerItem(Long agendaId, Long itemId) {
        Optional<Agenda> agendaOpt = agendaRepository.findById(agendaId);
        if (agendaOpt.isEmpty()) {
            return null;
        }

        Agenda agenda = agendaOpt.get();
        agenda.getItens().removeIf(item -> item.getId().equals(itemId));

        return agendaRepository.save(agenda);
    }
}
