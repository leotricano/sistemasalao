package br.com.leotricano.sistemasalao.repository;

import br.com.leotricano.sistemasalao.model.Agenda;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso à tabela {@code agenda}. Por enquanto só usa os métodos prontos do {@code JpaRepository}.
 *
 * <p>TODO(#12, #15): consultas por período e por profissional, para checar conflito de horário e
 * montar a agenda do dia/semana. Ex.: um método {@code findBy...Between(...)} (derived query).
 */
public interface AgendaRepository extends JpaRepository<Agenda, Long> {
}
