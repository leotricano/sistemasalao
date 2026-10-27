package br.com.leotricano.sistemasalao.dto;

import br.com.leotricano.sistemasalao.model.FormaPagamento;
import br.com.leotricano.sistemasalao.model.Pagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JSON de um pagamento. Leva só o {@code agendaId}, e não a agenda inteira: a agenda traria os
 * itens, e os itens trariam a agenda de novo (recursão).
 */
public record PagamentoResponse(Long id, Long agendaId, BigDecimal valor, FormaPagamento formaPagamento, LocalDateTime dataHora) {

    public static PagamentoResponse from(Pagamento pagamento) {
        return new PagamentoResponse(
                pagamento.getId(),
                pagamento.getAgenda().getId(),
                pagamento.getValor(),
                pagamento.getFormaPagamento(),
                pagamento.getDataHora()
        );
    }
}
