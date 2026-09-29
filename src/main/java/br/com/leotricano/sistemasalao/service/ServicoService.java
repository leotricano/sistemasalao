package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.model.Servico;
import br.com.leotricano.sistemasalao.repository.ServicoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public Servico atualizar(Long id, Servico dadosNovos) {
        Optional<Servico> servicoExistente = servicoRepository.findById(id);

        if (servicoExistente.isEmpty()) {
            return null;
        }

        Servico servico = servicoExistente.get();
        servico.setNome(dadosNovos.getNome());
        servico.setDuracao(dadosNovos.getDuracao());
        servico.setPreco(dadosNovos.getPreco());

        return servicoRepository.save(servico);
    }
}
