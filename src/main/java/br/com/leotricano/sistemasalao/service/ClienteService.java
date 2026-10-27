package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.model.Cliente;
import br.com.leotricano.sistemasalao.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service = o tratamento dos dados. Fica entre o controller (HTTP) e o repository (banco), e é
 * onde moram as regras de negócio. Este cuida do cadastro de clientes (RF01).
 *
 * <p>Convenção atual de todos os services: quando algo não é encontrado, o método devolve
 * {@code null} e o controller transforma isso em 404.
 * TODO(#13): trocar os {@code return null} por exceções tratadas num {@code @RestControllerAdvice}.
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

    public void deletar(Long id) {
        clienteRepository.deleteById(id);
    }

    /**
     * Atualiza nome, e-mail e telefone de um cliente existente.
     * Busca o registro do banco e copia os campos novos para ele, em vez de salvar o objeto que
     * veio na requisição, para não depender de o JSON trazer o id certo.
     *
     * @return o cliente atualizado, ou {@code null} se o id não existir
     */
    public Cliente atualizar(Long id, Cliente dadosNovos) {
        // Optional = "caixa" que pode ter o cliente ou estar vazia, evitando NullPointerException
        Optional<Cliente> clienteExistente = clienteRepository.findById(id);

        if (clienteExistente.isEmpty()) {
            return null;
        }

        Cliente cliente = clienteExistente.get();
        cliente.setNome(dadosNovos.getNome());
        cliente.setEmail(dadosNovos.getEmail());
        cliente.setTelefone(dadosNovos.getTelefone());

        return clienteRepository.save(cliente);
    }
}
