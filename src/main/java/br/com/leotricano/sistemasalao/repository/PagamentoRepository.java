package br.com.leotricano.sistemasalao.repository;

import br.com.leotricano.sistemasalao.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findByAgendaId(Long agendaId);
}
