package br.com.leotricano.sistemasalao.dto;

import br.com.leotricano.sistemasalao.model.Profissional;

import java.util.Comparator;
import java.util.List;

public record ProfissionalResponse(Long id, String nome, String especialidade, List<ServicoResponse> servicos) {

    public static ProfissionalResponse from(Profissional profissional) {
        List<ServicoResponse> servicos = profissional.getServicos().stream()
                .map(ServicoResponse::from)
                .sorted(Comparator.comparing(ServicoResponse::nome))
                .toList();

        return new ProfissionalResponse(
                profissional.getId(),
                profissional.getNome(),
                profissional.getEspecialidade(),
                servicos
        );
    }
}
