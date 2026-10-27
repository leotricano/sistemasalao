package br.com.leotricano.sistemasalao.model;

/**
 * Situação de um agendamento.
 * <ul>
 *   <li>{@code AGENDADO}: marcado, ainda não atendido.</li>
 *   <li>{@code CONCLUIDO}: atendimento feito.</li>
 *   <li>{@code CANCELADO}: desmarcado. O horário deve ficar livre para outro cliente.</li>
 * </ul>
 * TODO(#14): definir quais mudanças de status são permitidas (ex.: CANCELADO não volta para AGENDADO).
 */
public enum StatusAgendamento {
    AGENDADO, CONCLUIDO, CANCELADO
}
