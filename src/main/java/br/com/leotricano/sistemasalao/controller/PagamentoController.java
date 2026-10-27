package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.dto.PagamentoResponse;
import br.com.leotricano.sistemasalao.model.FormaPagamento;
import br.com.leotricano.sistemasalao.service.PagamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Endpoints de pagamento (RF05). Ficam "dentro" da agenda na URL
 * ({@code /agendas/{agendaId}/pagamentos}) porque todo pagamento pertence a um agendamento.
 * Erros são tratados pelo {@code TratadorDeErros}.
 */
@RestController
@RequestMapping("/agendas/{agendaId}/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    /** {@code GET /agendas/{id}/pagamentos} → pagamentos já registrados para a agenda; 404 se a agenda não existir. */
    @GetMapping
    public List<PagamentoResponse> listar(@PathVariable Long agendaId) {
        return pagamentoService.listarPorAgenda(agendaId).stream()
                .map(PagamentoResponse::from)
                .toList();
    }

    /**
     * {@code POST /agendas/{id}/pagamentos?valor=150&formaPagamento=PIX} → registra um pagamento.
     * 404 se a agenda não existir; 400 se o valor não for positivo ou a forma de pagamento for inválida.
     */
    @PostMapping
    public PagamentoResponse registrar(@PathVariable Long agendaId,
                                       @RequestParam BigDecimal valor,
                                       @RequestParam FormaPagamento formaPagamento) {
        return PagamentoResponse.from(pagamentoService.registrar(agendaId, valor, formaPagamento));
    }

    /** {@code DELETE /agendas/{agendaId}/pagamentos/{id}} → remove um pagamento lançado errado; 404 se ele não for dessa agenda. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long agendaId, @PathVariable Long id) {
        pagamentoService.deletar(agendaId, id);
        return ResponseEntity.noContent().build();
    }
}
