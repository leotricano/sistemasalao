package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.exception.RecursoNaoEncontradoException;
import br.com.leotricano.sistemasalao.model.Servico;
import br.com.leotricano.sistemasalao.repository.ServicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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

    /**
     * Remove o serviço.
     *
     * @throws RecursoNaoEncontradoException se o id não existir (404)
     * @throws org.springframework.dao.DataIntegrityViolationException se o serviço estiver vinculado a
     *         algum profissional ou já tiver sido usado em agendamentos (o TratadorDeErros responde 409)
     */
    public void deletar(Long id) {
        buscar(id);
        servicoRepository.deleteById(id);
    }

    /**
     * Atualiza nome, duração e preço.
     * Mudar o preço aqui não altera agendamentos já feitos: eles guardam o próprio
     * {@code precoCobrado} (ver ItemAgendamento).
     *
     * @throws RecursoNaoEncontradoException se o id não existir (404)
     */
    public Servico atualizar(Long id, Servico dadosNovos) {
        Servico servico = buscar(id);
        servico.setNome(dadosNovos.getNome());
        servico.setDuracaoMinutos(dadosNovos.getDuracaoMinutos());
        servico.setPreco(dadosNovos.getPreco());

        return servicoRepository.save(servico);
    }

    /** Busca pelo id ou lança 404 (ver ClienteService.buscar). */
    private Servico buscar(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço " + id + " não encontrado."));
    }
}
