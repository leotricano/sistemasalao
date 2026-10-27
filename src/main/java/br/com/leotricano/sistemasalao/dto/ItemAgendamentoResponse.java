package br.com.leotricano.sistemasalao.dto;

import br.com.leotricano.sistemasalao.model.ItemAgendamento;

import java.math.BigDecimal;

public record ItemAgendamentoResponse(Long id, String nomeServico, String nomeProfissional, BigDecimal precoCobrado) {

    public static ItemAgendamentoResponse from(ItemAgendamento itemAgendamento) {
        return new ItemAgendamentoResponse(
                itemAgendamento.getId(),
                itemAgendamento.getServico().getNome(),
                itemAgendamento.getProfissional().getNome(),
                itemAgendamento.getPrecoCobrado()
        );
    }
}
