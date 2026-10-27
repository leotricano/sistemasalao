package br.com.leotricano.sistemasalao.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Corpo de {@code PUT /agendas/{id}/remarcar}: {@code {"dataHora": "2026-11-05T10:00:00"}}.
 *
 * <p>É o primeiro DTO de <b>entrada</b> do projeto. Os DTOs de resposta controlam o que sai da API;
 * este controla o que entra: para remarcar, a API só aceita a nova data e hora, e nada mais da
 * agenda pode ser alterado por esse caminho (cliente, status, itens).
 *
 * @param dataHora novo início do atendimento; precisa estar no futuro (conferido no AgendaService)
 */
public record RemarcarRequest(@NotNull LocalDateTime dataHora) {
}
