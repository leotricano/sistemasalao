package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.dto.PagamentoResponse;
import br.com.leotricano.sistemasalao.model.FormaPagamento;
import br.com.leotricano.sistemasalao.model.Pagamento;
import br.com.leotricano.sistemasalao.service.PagamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Endpoints de pagamento (RF05). Ficam "dentro" da agenda na URL
 * ({@code /agendas/{agendaId}/pagamentos}) porque todo pagamento pertence a um agendamento.
 */
@RestController
@RequestMapping("/agendas/{agendaId}/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    /** {@code GET /agendas/{id}/pagamentos} → pagamentos já registrados para a agenda. */
    @GetMapping
    public List<PagamentoResponse> listar(@PathVariable Long agendaId) {
        return pagamentoService.listarPorAgenda(agendaId).stream()
                .map(PagamentoResponse::from)
                .toList();
    }

    /**
     * {@code POST /agendas/{id}/pagamentos?valor=150&formaPagamento=PIX} → registra um pagamento.
     * Responde 404 se a agenda não existir.
     */
    @PostMapping
    public ResponseEntity<PagamentoResponse> registrar(@PathVariable Long agendaId,
                                                        @RequestParam BigDecimal valor,
                                                        @RequestParam FormaPagamento formaPagamento) {
        Pagamento pagamento = pagamentoService.registrar(agendaId, valor, formaPagamento);
        if (pagamento == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(PagamentoResponse.from(pagamento));
    }

    /**
     * {@code DELETE /agendas/{agendaId}/pagamentos/{id}} → remove um pagamento lançado errado.
     * Hoje o {@code agendaId} da URL não é conferido: apaga o pagamento pelo id.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        pagamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
