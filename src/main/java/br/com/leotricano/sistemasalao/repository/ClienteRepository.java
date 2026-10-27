package br.com.leotricano.sistemasalao.repository;

import br.com.leotricano.sistemasalao.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository = a conexão com o banco de dados para a tabela {@code cliente}.
 *
 * <p>É só uma interface: o Spring Data cria a implementação sozinho quando a aplicação sobe.
 * Ao estender {@code JpaRepository<Cliente, Long>} (entidade, tipo do id), ela já ganha prontos
 * {@code findAll()}, {@code findById()}, {@code save()}, {@code deleteById()} e outros.
 * Os repositórios das outras entidades seguem o mesmo padrão.
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
