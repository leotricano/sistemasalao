package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.dto.ProfissionalResponse;
import br.com.leotricano.sistemasalao.model.Profissional;
import br.com.leotricano.sistemasalao.service.ProfissionalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de {@code /profissionais}: cadastro e vínculo com os serviços que cada um faz.
 * As respostas usam {@link ProfissionalResponse}; erros são tratados pelo {@code TratadorDeErros}.
 */
@RestController
@RequestMapping("/profissionais")
public class ProfissionalController {

    private final ProfissionalService profissionalService;

    public ProfissionalController(ProfissionalService profissionalService) {
        this.profissionalService = profissionalService;
    }

    /** {@code GET /profissionais} → lista os profissionais com os serviços de cada um. */
    @GetMapping
    public List<ProfissionalResponse> listar() {
        return profissionalService.listarTodos().stream()
                .map(ProfissionalResponse::from)
                .toList();
    }

    /** {@code POST /profissionais} → cadastra. Exemplo: {@code {"nome":"William","especialidade":"Cabeleireiro"}}. */
    @PostMapping
    public ProfissionalResponse cadastrar(@Valid @RequestBody Profissional profissional) {
        return ProfissionalResponse.from(profissionalService.salvar(profissional));
    }

    /** {@code PUT /profissionais/{id}} → atualiza nome e especialidade; 404 se não existir. */
    @PutMapping("/{id}")
    public ProfissionalResponse atualizar(@PathVariable Long id, @Valid @RequestBody Profissional dadosNovos) {
        return ProfissionalResponse.from(profissionalService.atualizar(id, dadosNovos));
    }

    /** {@code DELETE /profissionais/{id}} → remove e responde 204; 404 se não existir, 409 se tiver atendimentos. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        profissionalService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    /** {@code POST /profissionais/{id}/servicos/{servicoId}} → o profissional passa a fazer o serviço. Sem corpo; 404 se algum não existir. */
    @PostMapping("/{profissionalId}/servicos/{servicoId}")
    public ProfissionalResponse vincularServico(@PathVariable Long profissionalId, @PathVariable Long servicoId) {
        return ProfissionalResponse.from(profissionalService.vincularServico(profissionalId, servicoId));
    }

    /** {@code DELETE /profissionais/{id}/servicos/{servicoId}} → o profissional deixa de fazer o serviço; 404 se o profissional não existir. */
    @DeleteMapping("/{profissionalId}/servicos/{servicoId}")
    public ProfissionalResponse desvincularServico(@PathVariable Long profissionalId, @PathVariable Long servicoId) {
        return ProfissionalResponse.from(profissionalService.desvincularServico(profissionalId, servicoId));
    }
}
