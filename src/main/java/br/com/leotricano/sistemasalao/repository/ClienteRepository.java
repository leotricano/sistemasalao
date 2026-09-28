package br.com.leotricano.sistemasalao.repository;

import br.com.leotricano.sistemasalao.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
