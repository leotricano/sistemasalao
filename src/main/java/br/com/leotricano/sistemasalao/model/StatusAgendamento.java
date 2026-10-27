package br.com.leotricano.sistemasalao.model;

/**
 * Situação de um agendamento.
 * <ul>
 *   <li>{@code AGENDADO}: marcado, ainda não atendido. É o status de toda agenda nova.</li>
 *   <li>{@code CONCLUIDO}: atendimento feito. É final: não muda mais.</li>
 *   <li>{@code CANCELADO}: desmarcado. O horário fica livre para outro cliente.</li>
 * </ul>
 *
 * <p>Mudanças permitidas (#14, regras no AgendaService.atualizarStatus):
 * <pre>
 *   AGENDADO  → CONCLUIDO   (precisa ter pelo menos um serviço)
 *   AGENDADO  → CANCELADO   (não pode ter pagamento registrado)
 *   CANCELADO → AGENDADO    (reativar: só se o horário ainda estiver livre)
 *   CONCLUIDO → qualquer    ✗
 *   CANCELADO → CONCLUIDO   ✗
 * </pre>
 */
public enum StatusAgendamento {
    AGENDADO, CONCLUIDO, CANCELADO
}
