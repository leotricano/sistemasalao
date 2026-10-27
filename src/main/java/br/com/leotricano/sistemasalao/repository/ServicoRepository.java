package br.com.leotricano.sistemasalao.repository;

import br.com.leotricano.sistemasalao.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso à tabela {@code servico}. Só usa os métodos prontos do {@code JpaRepository} (ver ClienteRepository). */
public interface ServicoRepository extends JpaRepository<Servico, Long> {

}
