package br.com.leotricano.sistemasalao.repository;

import br.com.leotricano.sistemasalao.model.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {
}
