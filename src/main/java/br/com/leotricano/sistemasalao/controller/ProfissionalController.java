package br.com.leotricano.sistemasalao.controller;

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
    public List<Profissional> listar() {
        return profissionalService.listarTodos();
    }

    @PostMapping
    public Profissional cadastrar(@Valid @RequestBody Profissional profissional) {
        return profissionalService.salvar(profissional);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Profissional> atualizar(@PathVariable Long id, @Valid @RequestBody Profissional dadosNovos) {
        Profissional atualizado = profissionalService.atualizar(id, dadosNovos);
        if (atualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        profissionalService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{profissionalId}/servicos/{servicoId}")
    public ResponseEntity<Profissional> vincularServico(@PathVariable Long profissionalId, @PathVariable Long servicoId) {
        Profissional profissional = profissionalService.vincularServico(profissionalId, servicoId);
        if (profissional == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profissional);
    }

    @DeleteMapping("/{profissionalId}/servicos/{servicoId}")
    public ResponseEntity<Profissional> desvincularServico(@PathVariable Long profissionalId, @PathVariable Long servicoId) {
        Profissional profissional = profissionalService.desvincularServico(profissionalId, servicoId);
        if (profissional == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profissional);
    }
}
