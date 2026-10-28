package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.dto.HistoricoClienteResponse;
import br.com.leotricano.sistemasalao.model.Cliente;
import br.com.leotricano.sistemasalao.model.StatusAgendamento;
import br.com.leotricano.sistemasalao.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller = a porta de entrada HTTP. Recebe a requisição, chama o service e devolve a resposta.
 * Não tem regra de negócio: só traduz entre HTTP/JSON e Java. Endpoints de {@code /clientes} (RF01).
 *
 * <p>Erros (#13): os controllers não testam {@code null} nem fazem try/catch. Se o service lançar
 * uma exceção (ex.: cliente não encontrado), o {@code TratadorDeErros} monta a resposta de erro.
 *
 * <p>Este é o único controller que ainda devolve a entidade direto. Não dá loop porque o Cliente
 * não aponta para outras entidades. TODO: criar um ClienteResponse para manter o padrão.
 * O histórico (#16) já usa DTO: {@code HistoricoClienteResponse}.
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

    /** {@code DELETE /clientes/{id}} → remove e responde 204 (No Content); 404 se não existir, 409 se tiver agendamentos. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) { // @PathVariable pega o {id} da URL
        clienteService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code GET /clientes/{id}/historico} → histórico de atendimentos do cliente (#16, RF06):
     * resumo (atendimentos concluídos, último atendimento, total gasto) e as agendas da mais
     * recente para a mais antiga. {@code ?status=CONCLUIDO} deixa na lista só esse status; o
     * resumo não muda com o filtro.
     * <ul>
     *   <li>404 se o cliente não existir;</li>
     *   <li>400 se o status for inválido.</li>
     * </ul>
     */
    @GetMapping("/{id}/historico")
    public HistoricoClienteResponse historico(@PathVariable Long id,
                                              @RequestParam(required = false) StatusAgendamento status) {
        return clienteService.historico(id, status);
    }

    /** {@code PUT /clientes/{id}} → atualiza os dados; 404 se não existir. */
    @PutMapping("/{id}")
    public Cliente atualizar(@PathVariable Long id, @Valid @RequestBody Cliente dadosNovos) {
        return clienteService.atualizar(id, dadosNovos);
    }
}
