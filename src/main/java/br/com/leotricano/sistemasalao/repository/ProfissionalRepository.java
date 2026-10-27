package br.com.leotricano.sistemasalao.repository;

import br.com.leotricano.sistemasalao.model.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso à tabela {@code profissional}. Só usa os métodos prontos do {@code JpaRepository} (ver ClienteRepository). */
public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {
}
