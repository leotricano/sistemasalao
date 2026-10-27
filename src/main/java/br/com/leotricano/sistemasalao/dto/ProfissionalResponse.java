package br.com.leotricano.sistemasalao.dto;

import br.com.leotricano.sistemasalao.model.Profissional;

import java.util.Comparator;
import java.util.List;

/**
 * JSON de um profissional com os serviços que ele faz. Os serviços usam {@link ServicoResponse},
 * que não tem a lista de profissionais, e por isso o JSON não entra em loop.
 */
public record ProfissionalResponse(Long id, String nome, String especialidade, List<ServicoResponse> servicos) {

    public static ProfissionalResponse from(Profissional profissional) {
        // O Set da entidade não tem ordem garantida; ordenar por nome deixa a resposta previsível
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
