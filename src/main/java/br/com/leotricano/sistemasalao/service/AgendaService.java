package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.model.*;
import br.com.leotricano.sistemasalao.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public Agenda buscarPorId(Long id) {
        return agendaRepository.findById(id).orElse(null);
    }

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

    public void deletar(Long id) {
        agendaRepository.deleteById(id);
    }

    public Agenda atualizarStatus(Long id, StatusAgendamento novoStatus) {
        Optional<Agenda> agendaOpt = agendaRepository.findById(id);
        if (agendaOpt.isEmpty()) {
            return null;
        }

        Agenda agenda = agendaOpt.get();
        agenda.setStatus(novoStatus);
        return agendaRepository.save(agenda);
    }

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

        boolean profissionalFazOServico = false;

        for (Servico s : profissional.getServicos() ) {
            if (s.getId().equals(servico.getId())) {
                profissionalFazOServico = true;
            }
        }

        if (!profissionalFazOServico) {
            return null;
        }
        ItemAgendamento item = new ItemAgendamento();
        item.setAgenda(agenda);
        item.setServico(servico);
        item.setProfissional(profissional);
        item.setPrecoCobrado(servico.getPreco());

        agenda.getItens().add(item);

        return agendaRepository.save(agenda);
    }

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
