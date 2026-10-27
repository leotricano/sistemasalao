package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.exception.RecursoNaoEncontradoException;
import br.com.leotricano.sistemasalao.model.Profissional;
import br.com.leotricano.sistemasalao.model.Servico;
import br.com.leotricano.sistemasalao.repository.ProfissionalRepository;
import br.com.leotricano.sistemasalao.repository.ServicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Regras do cadastro de profissionais e de quais serviços cada um faz.
 *
 * <p>TODO(#29): ao cadastrar, já vincular os serviços padrão da especialidade (RF09).
 */
@Service
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;

    public ProfissionalService(ProfissionalRepository profissionalRepository, ServicoRepository servicoRepository) {
        this.profissionalRepository = profissionalRepository;
        this.servicoRepository = servicoRepository;
    }

    public List<Profissional> listarTodos() {
        return profissionalRepository.findAll();
    }

    public Profissional salvar(Profissional profissional) {
        return profissionalRepository.save(profissional);
    }

    /**
     * Remove o profissional (e os vínculos dele em {@code profissional_servico}).
     *
     * @throws RecursoNaoEncontradoException se o id não existir (404)
     * @throws org.springframework.dao.DataIntegrityViolationException se ele já tiver atendimentos
     *         agendados (o TratadorDeErros responde 409)
     */
    public void deletar(Long id) {
        buscar(id);
        profissionalRepository.deleteById(id);
    }

    /**
     * Atualiza nome e especialidade. Os serviços vinculados não mudam aqui; para isso existem
     * {@link #vincularServico} e {@link #desvincularServico}.
     *
     * @throws RecursoNaoEncontradoException se o id não existir (404)
     */
    public Profissional atualizar(Long id, Profissional dadosNovos) {
        Profissional profissional = buscar(id);
        profissional.setNome(dadosNovos.getNome());
        profissional.setEspecialidade(dadosNovos.getEspecialidade());

        return profissionalRepository.save(profissional);
    }

    /**
     * Passa a permitir que o profissional faça o serviço. Grava uma linha na tabela
     * {@code profissional_servico}. Vincular de novo o mesmo serviço não duplica, porque a lista é um Set.
     *
     * @throws RecursoNaoEncontradoException se o profissional ou o serviço não existir (404)
     */
    public Profissional vincularServico(Long profissionalId, Long servicoId) {
        Profissional profissional = buscar(profissionalId);
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço " + servicoId + " não encontrado."));

        profissional.getServicos().add(servico);

        // Salva o profissional porque ele é o lado "dono" da relação N:N
        return profissionalRepository.save(profissional);
    }

    /**
     * Remove o serviço da lista do profissional (apaga a linha em {@code profissional_servico}).
     * O serviço continua existindo para os outros profissionais. Se ele não estava na lista,
     * nada muda.
     *
     * @throws RecursoNaoEncontradoException se o profissional não existir (404)
     */
    public Profissional desvincularServico(Long profissionalId, Long servicoId) {
        Profissional profissional = buscar(profissionalId);
        // removeIf: tira da lista todo serviço cujo id é igual ao informado
        profissional.getServicos().removeIf(servico -> servico.getId().equals(servicoId));

        return profissionalRepository.save(profissional);
    }

    /** Busca pelo id ou lança 404 (ver ClienteService.buscar). */
    private Profissional buscar(Long id) {
        return profissionalRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional " + id + " não encontrado."));
    }
}
