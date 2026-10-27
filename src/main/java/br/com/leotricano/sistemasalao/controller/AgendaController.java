package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.dto.AgendaResponse;
import br.com.leotricano.sistemasalao.dto.RemarcarRequest;
import br.com.leotricano.sistemasalao.model.Agenda;
import br.com.leotricano.sistemasalao.model.StatusAgendamento;
import br.com.leotricano.sistemasalao.service.AgendaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de {@code /agendas}: agendamentos e os serviços dentro de cada um (RF03), cancelar e
 * remarcar (RF08).
 * Todas as respostas usam {@link AgendaResponse}, que já traz itens, término, duração total e
 * valor total. Os erros (404, 400, 409) vêm do service e são montados pelo {@code TratadorDeErros}.
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
    public AgendaResponse buscarPorId(@PathVariable Long id) {
        return AgendaResponse.from(agendaService.buscarPorId(id));
    }

    /**
     * {@code POST /agendas} → cria a agenda, ainda sem serviços e sempre com status AGENDADO.
     * Exemplo: {@code {"cliente":{"id":1},"dataHora":"2026-11-03T14:00:00"}}.
     * Responde 400 se o cliente não for informado ou não existir.
     */
    @PostMapping
    public AgendaResponse criar(@Valid @RequestBody Agenda agenda) {
        return AgendaResponse.from(agendaService.criar(agenda));
    }

    /**
     * {@code PUT /agendas/{id}/status} → conclui, cancela ou reativa. O corpo é só o texto, com
     * aspas: {@code "CONCLUIDO"}, {@code "CANCELADO"} ou {@code "AGENDADO"}.
     * <ul>
     *   <li>404 se a agenda não existir;</li>
     *   <li>400 se o status for inválido ou a mudança não for permitida (ex.: cancelar agenda paga,
     *       mexer em atendimento concluído);</li>
     *   <li>409 se, ao reativar uma agenda cancelada, o horário já estiver ocupado.</li>
     * </ul>
     */
    @PutMapping("/{id}/status")
    public AgendaResponse atualizarStatus(@PathVariable Long id, @RequestBody StatusAgendamento novoStatus) {
        return AgendaResponse.from(agendaService.atualizarStatus(id, novoStatus));
    }

    /**
     * {@code PUT /agendas/{id}/remarcar} → muda a data e hora, mantendo os serviços (RF08).
     * Corpo: {@code {"dataHora":"2026-11-05T10:00:00"}} (ver {@link RemarcarRequest}).
     * <ul>
     *   <li>404 se a agenda não existir;</li>
     *   <li>400 se a agenda não estiver AGENDADA, se a data estiver no passado ou se faltar a {@code dataHora};</li>
     *   <li>409 se o novo horário estiver ocupado para algum profissional da agenda.</li>
     * </ul>
     */
    @PutMapping("/{id}/remarcar")
    public AgendaResponse remarcar(@PathVariable Long id, @Valid @RequestBody RemarcarRequest dados) {
        return AgendaResponse.from(agendaService.remarcar(id, dados.dataHora()));
    }

    /** {@code DELETE /agendas/{id}} → apaga a agenda e os itens dela; responde 204, 404 se não existir, 409 se tiver pagamentos. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        agendaService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code POST /agendas/{id}/itens?servicoId=1&profissionalId=2} → inclui um serviço na agenda.
     * {@code @RequestParam} lê os valores depois do "?" na URL.
     * <ul>
     *   <li>404 se a agenda, o serviço ou o profissional não existir;</li>
     *   <li>400 se o profissional não fizer o serviço ou se a agenda não estiver AGENDADA;</li>
     *   <li>409 se o profissional já tiver outro atendimento no horário (#12).</li>
     * </ul>
     */
    @PostMapping("/{agendaId}/itens")
    public AgendaResponse adicionarItem(@PathVariable Long agendaId,
                                        @RequestParam Long servicoId,
                                        @RequestParam Long profissionalId) {
        return AgendaResponse.from(agendaService.adicionarItem(agendaId, servicoId, profissionalId));
    }

    /** {@code DELETE /agendas/{id}/itens/{itemId}} → tira um serviço da agenda; 404 se a agenda não existir ou o item não for dela, 400 se ela não estiver AGENDADA. */
    @DeleteMapping("/{agendaId}/itens/{itemId}")
    public AgendaResponse removerItem(@PathVariable Long agendaId, @PathVariable Long itemId) {
        return AgendaResponse.from(agendaService.removerItem(agendaId, itemId));
    }
}
