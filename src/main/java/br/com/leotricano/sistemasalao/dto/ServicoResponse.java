package br.com.leotricano.sistemasalao.dto;

import br.com.leotricano.sistemasalao.model.Servico;

import java.math.BigDecimal;

/**
 * JSON de um serviço. Deixa de fora a lista de profissionais da entidade, que levaria de volta ao
 * serviço e geraria recursão (Servico → Profissional → Servico...).
 */
public record ServicoResponse(Long id, String nome, Integer duracaoMinutos, BigDecimal preco) {

    public static ServicoResponse from(Servico servico) {
        return new ServicoResponse(
                servico.getId(),
                servico.getNome(),
                servico.getDuracaoMinutos(),
                servico.getPreco()
        );
    }
}
