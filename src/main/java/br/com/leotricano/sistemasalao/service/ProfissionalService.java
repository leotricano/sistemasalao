package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.model.Profissional;
import br.com.leotricano.sistemasalao.model.Servico;
import br.com.leotricano.sistemasalao.repository.ProfissionalRepository;
import br.com.leotricano.sistemasalao.repository.ServicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public void deletar(Long id) {
        profissionalRepository.deleteById(id);
    }

    /**
     * Atualiza nome e especialidade. Os serviços vinculados não mudam aqui; para isso existem
     * {@link #vincularServico} e {@link #desvincularServico}.
     *
     * @return o profissional atualizado, ou {@code null} se o id não existir
     */
    public Profissional atualizar(Long id, Profissional dadosNovos) {
        Optional<Profissional> profissionalExistente = profissionalRepository.findById(id);

        if (profissionalExistente.isEmpty()) {
            return null;
        }

        Profissional profissional = profissionalExistente.get();
        profissional.setNome(dadosNovos.getNome());
        profissional.setEspecialidade(dadosNovos.getEspecialidade());

        return profissionalRepository.save(profissional);
    }

    /**
     * Passa a permitir que o profissional faça o serviço. Grava uma linha na tabela
     * {@code profissional_servico}. Vincular de novo o mesmo serviço não duplica, porque a lista é um Set.
     *
     * @return o profissional com a lista atualizada, ou {@code null} se o profissional ou o serviço não existir
     */
    public Profissional vincularServico(Long profissionalId, Long servicoId) {
        Optional<Profissional> profissionalOpt = profissionalRepository.findById(profissionalId);
        Optional<Servico> servicoOpt = servicoRepository.findById(servicoId);

        if (profissionalOpt.isEmpty() || servicoOpt.isEmpty()) {
            return null;
        }

        Profissional profissional = profissionalOpt.get();
        profissional.getServicos().add(servicoOpt.get());

        // Salva o profissional porque ele é o lado "dono" da relação N:N
        return profissionalRepository.save(profissional);
    }

    /**
     * Remove o serviço da lista do profissional (apaga a linha em {@code profissional_servico}).
     * O serviço continua existindo para os outros profissionais.
     *
     * @return o profissional com a lista atualizada, ou {@code null} se o profissional não existir
     */
    public Profissional desvincularServico(Long profissionalId, Long servicoId) {
        Optional<Profissional> profissionalOpt = profissionalRepository.findById(profissionalId);

        if (profissionalOpt.isEmpty()) {
            return null;
        }

        Profissional profissional = profissionalOpt.get();
        // removeIf: tira da lista todo serviço cujo id é igual ao informado
        profissional.getServicos().removeIf(servico -> servico.getId().equals(servicoId));

        return profissionalRepository.save(profissional);
    }
}
