package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.dto.ProfissionalResponse;
import br.com.leotricano.sistemasalao.model.Profissional;
import br.com.leotricano.sistemasalao.service.ProfissionalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profissionais")
public class ProfissionalController {

    private final ProfissionalService profissionalService;

    public ProfissionalController(ProfissionalService profissionalService) {
        this.profissionalService = profissionalService;
    }

    @GetMapping
    public List<ProfissionalResponse> listar() {
        return profissionalService.listarTodos().stream()
                .map(ProfissionalResponse::from)
                .toList();
    }

    @PostMapping
    public ProfissionalResponse cadastrar(@Valid @RequestBody Profissional profissional) {
        return ProfissionalResponse.from(profissionalService.salvar(profissional));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfissionalResponse> atualizar(@PathVariable Long id, @Valid @RequestBody Profissional dadosNovos) {
        Profissional atualizado = profissionalService.atualizar(id, dadosNovos);
        if (atualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ProfissionalResponse.from(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        profissionalService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{profissionalId}/servicos/{servicoId}")
    public ResponseEntity<ProfissionalResponse> vincularServico(@PathVariable Long profissionalId, @PathVariable Long servicoId) {
        Profissional profissional = profissionalService.vincularServico(profissionalId, servicoId);
        if (profissional == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ProfissionalResponse.from(profissional));
    }

    @DeleteMapping("/{profissionalId}/servicos/{servicoId}")
    public ResponseEntity<ProfissionalResponse> desvincularServico(@PathVariable Long profissionalId, @PathVariable Long servicoId) {
        Profissional profissional = profissionalService.desvincularServico(profissionalId, servicoId);
        if (profissional == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ProfissionalResponse.from(profissional));
    }
}
