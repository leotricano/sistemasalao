package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.dto.ServicoResponse;
import br.com.leotricano.sistemasalao.model.Servico;
import br.com.leotricano.sistemasalao.service.ServicoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/servicos")
public class ServicoController {

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @GetMapping
    public List<ServicoResponse> listar() {
        return servicoService.listarTodos().stream()
                .map(ServicoResponse::from)
                .toList();
    }

    @PostMapping
    public ServicoResponse cadastrar(@Valid @RequestBody Servico servico) {
        return ServicoResponse.from(servicoService.salvar(servico));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody Servico dadosNovos) {
        Servico atualizado = servicoService.atualizar(id, dadosNovos);
        if (atualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ServicoResponse.from(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        servicoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
