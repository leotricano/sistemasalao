package br.com.leotricano.sistemasalao.dto;

import br.com.leotricano.sistemasalao.model.Servico;

import java.math.BigDecimal;

public record ServicoResponse(Long id, String nome, String duracao, BigDecimal preco) {

    public static ServicoResponse from(Servico servico) {
        return new ServicoResponse(
                servico.getId(),
                servico.getNome(),
                servico.getDuracao(),
                servico.getPreco()
        );
    }
}
