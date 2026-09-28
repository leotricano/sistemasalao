package br.com.leotricano.sistemasalao.repository;

import br.com.leotricano.sistemasalao.model.Agenda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgendaRepository extends JpaRepository<Agenda, Long> {
}
