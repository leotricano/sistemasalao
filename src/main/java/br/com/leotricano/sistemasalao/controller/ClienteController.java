package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.model.Cliente;
import br.com.leotricano.sistemasalao.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller = a porta de entrada HTTP. Recebe a requisição, chama o service e devolve a resposta.
 * Não tem regra de negócio: só traduz entre HTTP/JSON e Java. Endpoints de {@code /clientes} (RF01).
 *
 * <p>Este é o único controller que ainda devolve a entidade direto. Não dá loop porque o Cliente
 * não aponta para outras entidades. TODO: criar um ClienteResponse para manter o padrão.
 */
@RestController // cada método devolve dados (JSON), não uma página HTML
@RequestMapping("/clientes") // prefixo de todas as rotas desta classe
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    /** {@code GET /clientes} → lista todos os clientes. */
    @GetMapping
    public List<Cliente> listar() {
        return clienteService.listarTodos();
    }

    /**
     * {@code POST /clientes} → cadastra um cliente.
     * {@code @RequestBody} converte o JSON do corpo em um Cliente; {@code @Valid} aplica as
     * validações da entidade (@NotBlank, @Email) e responde 400 se alguma falhar.
     */
    @PostMapping
    public Cliente cadastrar(@Valid @RequestBody Cliente cliente) {
        return clienteService.salvar(cliente);
    }

    /** {@code DELETE /clientes/{id}} → remove e responde 204 (No Content). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) { // @PathVariable pega o {id} da URL
        clienteService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code PUT /clientes/{id}} → atualiza os dados.
     * {@code ResponseEntity} permite escolher o status HTTP: 200 com o cliente, ou 404 se não existir.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> atualizar(@PathVariable Long id, @Valid @RequestBody Cliente dadosNovos) {
        Cliente clienteAtualizado = clienteService.atualizar(id, dadosNovos);
        if (clienteAtualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(clienteAtualizado);
    }
}
