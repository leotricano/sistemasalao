package br.com.leotricano.sistemasalao.repository;

import br.com.leotricano.sistemasalao.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acesso à tabela {@code pagamento}. */
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    /**
     * Pagamentos de uma agenda. Não tem implementação: o Spring Data lê o nome do método
     * ("find By Agenda Id") e gera sozinho o SQL {@code where agenda_id = ?}. Isso se chama
     * <i>derived query</i>.
     */
    List<Pagamento> findByAgendaId(Long agendaId);
}
