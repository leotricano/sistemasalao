package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.exception.RecursoNaoEncontradoException;
import br.com.leotricano.sistemasalao.model.Cliente;
import br.com.leotricano.sistemasalao.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service = o tratamento dos dados. Fica entre o controller (HTTP) e o repository (banco), e é
 * onde moram as regras de negócio. Este cuida do cadastro de clientes (RF01).
 *
 * <p>Convenção de erros de todos os services (#13): quando algo dá errado, o método lança uma
 * exceção do pacote {@code exception} (nunca devolve {@code null}). O {@code TratadorDeErros}
 * transforma a exceção na resposta HTTP certa.
 */
@Service // registra a classe no Spring, que cria uma instância e a injeta onde for pedida
public class ClienteService {

    private final ClienteRepository clienteRepository;

    /**
     * Injeção de dependência pelo construtor: o Spring vê que o service precisa de um
     * ClienteRepository e passa a instância que ele mesmo criou. Ninguém faz {@code new} aqui.
     */
    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    /** Cria o cliente. O {@code save} faz INSERT quando o id é nulo e UPDATE quando já existe. */
    public Cliente salvar(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    /**
     * Remove o cliente.
     *
     * @throws RecursoNaoEncontradoException se o id não existir (404)
     * @throws org.springframework.dao.DataIntegrityViolationException se o cliente tiver agendamentos;
     *         o banco recusa por causa da chave estrangeira e o TratadorDeErros responde 409
     */
    public void deletar(Long id) {
        buscar(id); // garante o 404; o deleteById sozinho não avisa quando o id não existe
        clienteRepository.deleteById(id);
    }

    /**
     * Atualiza nome, e-mail e telefone de um cliente existente.
     * Busca o registro do banco e copia os campos novos para ele, em vez de salvar o objeto que
     * veio na requisição, para não depender de o JSON trazer o id certo.
     *
     * @throws RecursoNaoEncontradoException se o id não existir (404)
     */
    public Cliente atualizar(Long id, Cliente dadosNovos) {
        Cliente cliente = buscar(id);
        cliente.setNome(dadosNovos.getNome());
        cliente.setEmail(dadosNovos.getEmail());
        cliente.setTelefone(dadosNovos.getTelefone());

        return clienteRepository.save(cliente);
    }

    /**
     * Busca pelo id ou lança 404. {@code orElseThrow} abre o Optional: se tiver o cliente, devolve;
     * se estiver vazio, lança a exceção criada pela função passada.
     */
    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente " + id + " não encontrado."));
    }
}
