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

    /**
     * Diz se a agenda tem algum pagamento. Também é uma derived query: o prefixo {@code exists}
     * faz o Spring gerar um SQL que só verifica se existe, sem carregar os pagamentos.
     * Usada para impedir o cancelamento de agenda já paga (#14).
     */
    boolean existsByAgendaId(Long agendaId);
}
