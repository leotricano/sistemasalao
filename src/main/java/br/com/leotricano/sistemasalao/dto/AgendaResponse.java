package br.com.leotricano.sistemasalao.dto;

import br.com.leotricano.sistemasalao.model.Agenda;
import br.com.leotricano.sistemasalao.model.StatusAgendamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JSON de um agendamento, com o que a tela "Detalhe do agendamento" do protótipo mostra:
 * cliente, início e término, status, serviços, duração total e valor total.
 *
 * <p>O cliente vira só id + nome, e os itens viram {@link ItemAgendamentoResponse}, que não aponta
 * de volta para a agenda. Assim o JSON tem fim.
 */
public record AgendaResponse(Long id, Long clienteId, String nomeCliente, LocalDateTime dataHora,
                             LocalDateTime dataHoraFim, StatusAgendamento status, List<ItemAgendamentoResponse> itens,
                             Integer duracaoTotalMinutos, BigDecimal total) {

    public static AgendaResponse from(Agenda agenda) {
        // Converte cada ItemAgendamento da lista em ItemAgendamentoResponse
        List<ItemAgendamentoResponse> itens = agenda.getItens().stream()
                .map(ItemAgendamentoResponse::from)
                .toList();

        // Soma a duração de todos os serviços. Ex.: corte 60 + coloração 120 = 180 minutos.
        // O término é dataHora + esse total; é o mesmo cálculo usado na regra de conflito (#12).
        int duracaoTotalMinutos = agenda.calcularDuracaoTotalMinutos();

        // Soma os preços cobrados. reduce começa em ZERO e vai somando (BigDecimal não usa "+").
        BigDecimal total = itens.stream()
                .map(ItemAgendamentoResponse::precoCobrado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AgendaResponse(
                agenda.getId(),
                agenda.getCliente().getId(),
                agenda.getCliente().getNome(),
                agenda.getDataHora(),
                agenda.calcularFim(),
                agenda.getStatus(),
                itens,
                duracaoTotalMinutos,
                total
        );
    }
}
