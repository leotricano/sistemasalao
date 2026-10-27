package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.dto.AgendaResponse;
import br.com.leotricano.sistemasalao.model.Agenda;
import br.com.leotricano.sistemasalao.model.StatusAgendamento;
import br.com.leotricano.sistemasalao.service.AgendaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de {@code /agendas} (RF03): agendamentos e os serviços dentro de cada um.
 * Todas as respostas usam {@link AgendaResponse}, que já traz itens, duração total e valor total.
 */
@RestController
@RequestMapping("/agendas")
public class AgendaController {

    private final AgendaService agendaService;

    public AgendaController(AgendaService agendaService) {
        this.agendaService = agendaService;
    }

    /**
     * {@code GET /agendas} → lista todos os agendamentos.
     * TODO(#15): filtros por dia, semana e profissional (RF04).
     */
    @GetMapping
    public List<AgendaResponse> listar() {
        return agendaService.listarTodas().stream()
                .map(AgendaResponse::from)
                .toList();
    }

    /** {@code GET /agendas/{id}} → detalhe de um agendamento; 404 se não existir. */
    @GetMapping("/{id}")
    public ResponseEntity<AgendaResponse> buscarPorId(@PathVariable Long id) {
        Agenda agenda = agendaService.buscarPorId(id);
        if (agenda == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(AgendaResponse.from(agenda));
    }

    /**
     * {@code POST /agendas} → cria a agenda, ainda sem serviços.
     * Exemplo: {@code {"cliente":{"id":1},"dataHora":"2026-11-03T14:00:00","status":"AGENDADO"}}.
     * Responde 400 se o cliente não for informado ou não existir.
     */
    @PostMapping
    public ResponseEntity<AgendaResponse> criar(@Valid @RequestBody Agenda agenda) {
        Agenda criada = agendaService.criar(agenda);
        if (criada == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(AgendaResponse.from(criada));
    }

    /** {@code PUT /agendas/{id}/status} → troca o status. O corpo é só o texto, com aspas: {@code "CONCLUIDO"}. */
    @PutMapping("/{id}/status")
    public ResponseEntity<AgendaResponse> atualizarStatus(@PathVariable Long id, @RequestBody StatusAgendamento novoStatus) {
        Agenda atualizada = agendaService.atualizarStatus(id, novoStatus);
        if (atualizada == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(AgendaResponse.from(atualizada));
    }

    /** {@code DELETE /agendas/{id}} → apaga a agenda e os itens dela; responde 204. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        agendaService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code POST /agendas/{id}/itens?servicoId=1&profissionalId=2} → inclui um serviço na agenda.
     * {@code @RequestParam} lê os valores depois do "?" na URL.
     * <ul>
     *   <li>404 se algo não existir ou se o profissional não fizer o serviço;</li>
     *   <li>409 se o profissional já tiver outro atendimento no horário (#12). Esse caso não passa
     *       pelo {@code if} abaixo: o service lança ConflitoHorarioException e o TratadorDeErros
     *       monta a resposta.</li>
     * </ul>
     */
    @PostMapping("/{agendaId}/itens")
    public ResponseEntity<AgendaResponse> adicionarItem(@PathVariable Long agendaId,
                                                         @RequestParam Long servicoId,
                                                         @RequestParam Long profissionalId) {
        Agenda agenda = agendaService.adicionarItem(agendaId, servicoId, profissionalId);
        if (agenda == null) {
            // TODO(#13): diferenciar "não encontrado" (404) de "profissional não faz o serviço" (400)
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(AgendaResponse.from(agenda));
    }

    /** {@code DELETE /agendas/{id}/itens/{itemId}} → tira um serviço da agenda. */
    @DeleteMapping("/{agendaId}/itens/{itemId}")
    public ResponseEntity<AgendaResponse> removerItem(@PathVariable Long agendaId, @PathVariable Long itemId) {
        Agenda agenda = agendaService.removerItem(agendaId, itemId);
        if (agenda == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(AgendaResponse.from(agenda));
    }
}
