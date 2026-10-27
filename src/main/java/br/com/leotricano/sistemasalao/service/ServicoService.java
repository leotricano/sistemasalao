package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.model.Servico;
import br.com.leotricano.sistemasalao.repository.ServicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Regras do cadastro de serviços do salão (RF02). Mesmo padrão do ClienteService.
 */
@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    public List<Servico> listarTodos() {
        return servicoRepository.findAll();
    }

    public Servico salvar(Servico servico) {
        return servicoRepository.save(servico);
    }

    public void deletar(Long id) {
        servicoRepository.deleteById(id);
    }

    /**
     * Atualiza nome, duração e preço.
     * Mudar o preço aqui não altera agendamentos já feitos: eles guardam o próprio
     * {@code precoCobrado} (ver ItemAgendamento).
     *
     * @return o serviço atualizado, ou {@code null} se o id não existir
     */
    public Servico atualizar(Long id, Servico dadosNovos) {
        Optional<Servico> servicoExistente = servicoRepository.findById(id);

        if (servicoExistente.isEmpty()) {
            return null;
        }

        Servico servico = servicoExistente.get();
        servico.setNome(dadosNovos.getNome());
        servico.setDuracaoMinutos(dadosNovos.getDuracaoMinutos());
        servico.setPreco(dadosNovos.getPreco());

        return servicoRepository.save(servico);
    }
}
