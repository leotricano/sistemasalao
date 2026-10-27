package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.model.Profissional;
import br.com.leotricano.sistemasalao.model.Servico;
import br.com.leotricano.sistemasalao.repository.ProfissionalRepository;
import br.com.leotricano.sistemasalao.repository.ServicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public Profissional vincularServico(Long profissionalId, Long servicoId) {
        Optional<Profissional> profissionalOpt = profissionalRepository.findById(profissionalId);
        Optional<Servico> servicoOpt = servicoRepository.findById(servicoId);

        if (profissionalOpt.isEmpty() || servicoOpt.isEmpty()) {
            return null;
        }

        Profissional profissional = profissionalOpt.get();
        profissional.getServicos().add(servicoOpt.get());

        return profissionalRepository.save(profissional);
    }

    public Profissional desvincularServico(Long profissionalId, Long servicoId) {
        Optional<Profissional> profissionalOpt = profissionalRepository.findById(profissionalId);

        if (profissionalOpt.isEmpty()) {
            return null;
        }

        Profissional profissional = profissionalOpt.get();
        profissional.getServicos().removeIf(servico -> servico.getId().equals(servicoId));

        return profissionalRepository.save(profissional);
    }
}
