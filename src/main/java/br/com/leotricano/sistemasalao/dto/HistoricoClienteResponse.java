package br.com.leotricano.sistemasalao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JSON de {@code GET /clientes/{id}/historico} (#16, RF06): um resumo para o topo da tela e,
 * abaixo, as agendas do cliente da mais recente para a mais antiga.
 *
 * <p>Os três números do resumo contam <b>só as agendas CONCLUÍDAS</b>, mesmo quando a lista vem
 * filtrada por outro status. Agendas canceladas ou ainda por acontecer aparecem na lista, com o
 * status delas, mas não são atendimento feito. As regras ficam no {@code ClienteService.historico}.
 *
 * @param totalAtendimentos  quantas agendas concluídas o cliente tem
 * @param ultimoAtendimento  início da agenda concluída mais recente, ou {@code null} se não houver nenhuma
 * @param totalGasto         soma dos valores dos serviços ({@code precoCobrado}) das agendas concluídas;
 *                           não usa os pagamentos, que hoje podem passar do total
 * @param agendas            agendas do cliente (todas, ou só as do status pedido), mais recente primeiro
 */
public record HistoricoClienteResponse(Long clienteId, String nomeCliente, int totalAtendimentos,
                                       LocalDateTime ultimoAtendimento, BigDecimal totalGasto,
                                       List<AgendaResponse> agendas) {
}
