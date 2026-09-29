package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.model.Agenda;
import br.com.leotricano.sistemasalao.model.StatusAgendamento;
import br.com.leotricano.sistemasalao.service.AgendaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agendas")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    @GetMapping
    public List<Agenda> listar() {
        return agendaService.listarTodas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Agenda> buscarPorId(@PathVariable Long id) {
        Agenda agenda = agendaService.buscarPorId(id);
        if (agenda == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(agenda);
    }

    @PostMapping
    public ResponseEntity<Agenda> criar(@Valid @RequestBody Agenda agenda) {
        Agenda criada = agendaService.criar(agenda);
        if (criada == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(criada);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Agenda> atualizarStatus(@PathVariable Long id, @RequestBody StatusAgendamento novoStatus) {
        Agenda atualizada = agendaService.atualizarStatus(id, novoStatus);
        if (atualizada == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(atualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        agendaService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{agendaId}/itens")
    public ResponseEntity<Agenda> adicionarItem(@PathVariable Long agendaId,
                                                 @RequestParam Long servicoId,
                                                 @RequestParam Long profissionalId) {
        Agenda agenda = agendaService.adicionarItem(agendaId, servicoId, profissionalId);
        if (agenda == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(agenda);
    }

    @DeleteMapping("/{agendaId}/itens/{itemId}")
    public ResponseEntity<Agenda> removerItem(@PathVariable Long agendaId, @PathVariable Long itemId) {
        Agenda agenda = agendaService.removerItem(agendaId, itemId);
        if (agenda == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(agenda);
    }
}
