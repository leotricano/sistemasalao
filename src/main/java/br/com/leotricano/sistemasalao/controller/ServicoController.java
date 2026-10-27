package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.dto.ServicoResponse;
import br.com.leotricano.sistemasalao.model.Servico;
import br.com.leotricano.sistemasalao.service.ServicoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de {@code /servicos} (RF02).
 *
 * <p>Padrão dos controllers com DTO: a entrada ainda é a entidade ({@code @RequestBody Servico}),
 * mas a saída é sempre convertida para {@link ServicoResponse} com {@code ServicoResponse.from(...)}.
 * Erros são tratados pelo {@code TratadorDeErros} (ver ClienteController).
 */
@RestController
@RequestMapping("/servicos")
public class ServicoController {

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    /** {@code GET /servicos} → lista todos os serviços. */
    @GetMapping
    public List<ServicoResponse> listar() {
        // stream().map(...).toList(): transforma cada Servico da lista em um ServicoResponse
        return servicoService.listarTodos().stream()
                .map(ServicoResponse::from)
                .toList();
    }

    /** {@code POST /servicos} → cadastra. Exemplo: {@code {"nome":"Corte","duracaoMinutos":60,"preco":80.00}}. */
    @PostMapping
    public ServicoResponse cadastrar(@Valid @RequestBody Servico servico) {
        return ServicoResponse.from(servicoService.salvar(servico));
    }

    /** {@code PUT /servicos/{id}} → atualiza nome, duração e preço; 404 se não existir. */
    @PutMapping("/{id}")
    public ServicoResponse atualizar(@PathVariable Long id, @Valid @RequestBody Servico dadosNovos) {
        return ServicoResponse.from(servicoService.atualizar(id, dadosNovos));
    }

    /** {@code DELETE /servicos/{id}} → remove e responde 204; 404 se não existir, 409 se estiver em uso. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        servicoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
