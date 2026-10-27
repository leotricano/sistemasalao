package br.com.leotricano.sistemasalao.dto;

import br.com.leotricano.sistemasalao.model.Agenda;
import br.com.leotricano.sistemasalao.model.StatusAgendamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AgendaResponse(Long id, Long clienteId, String nomeCliente, LocalDateTime dataHora,
                             StatusAgendamento status, List<ItemAgendamentoResponse> itens, BigDecimal total) {

    public static AgendaResponse from(Agenda agenda) {
        List<ItemAgendamentoResponse> itens = agenda.getItens().stream()
                .map(ItemAgendamentoResponse::from)
                .toList();

        BigDecimal total = itens.stream()
                .map(ItemAgendamentoResponse::precoCobrado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AgendaResponse(
                agenda.getId(),
                agenda.getCliente().getId(),
                agenda.getCliente().getNome(),
                agenda.getDataHora(),
                agenda.getStatus(),
                itens,
                total
        );
    }
}
