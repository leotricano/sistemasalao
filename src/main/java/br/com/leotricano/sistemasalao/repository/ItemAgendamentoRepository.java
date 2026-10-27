package br.com.leotricano.sistemasalao.repository;

import br.com.leotricano.sistemasalao.model.ItemAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso à tabela {@code item_agendamento}.
 *
 * <p>Ainda não é usado por nenhum service: os itens são salvos e apagados em cascata pela
 * {@code Agenda} (ver {@code cascade} e {@code orphanRemoval} em Agenda.itens).
 */
public interface ItemAgendamentoRepository extends JpaRepository<ItemAgendamento, Long> {
}
