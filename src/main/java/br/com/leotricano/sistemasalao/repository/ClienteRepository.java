package br.com.leotricano.sistemasalao.repository;

import br.com.leotricano.sistemasalao.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
//Repository = a conexão pro banco de dados.
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
