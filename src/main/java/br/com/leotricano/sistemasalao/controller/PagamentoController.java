package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.model.FormaPagamento;
import br.com.leotricano.sistemasalao.model.Pagamento;
import br.com.leotricano.sistemasalao.service.PagamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/agendas/{agendaId}/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @GetMapping
    public List<Pagamento> listar(@PathVariable Long agendaId) {
        return pagamentoService.listarPorAgenda(agendaId);
    }

    @PostMapping
    public ResponseEntity<Pagamento> registrar(@PathVariable Long agendaId,
                                                @RequestParam BigDecimal valor,
                                                @RequestParam FormaPagamento formaPagamento) {
        Pagamento pagamento = pagamentoService.registrar(agendaId, valor, formaPagamento);
        if (pagamento == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pagamento);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        pagamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
