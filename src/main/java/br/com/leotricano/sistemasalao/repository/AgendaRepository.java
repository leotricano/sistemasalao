package br.com.leotricano.sistemasalao.repository;

import br.com.leotricano.sistemasalao.model.Agenda;
import br.com.leotricano.sistemasalao.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Acesso à tabela {@code agenda}.
 *
 * <p>TODO(#15): consultas por dia, semana e profissional para montar a tela de agenda (RF04).
 */
public interface AgendaRepository extends JpaRepository<Agenda, Long> {

    /**
     * Agendas que <i>podem</i> conflitar com um horário (#12). Traz as agendas que:
     * <ul>
     *   <li>têm pelo menos um item com algum dos profissionais informados;</li>
     *   <li>não estão com o status informado (o service passa CANCELADO: cancelado libera o horário);</li>
     *   <li>não são a própria agenda que está sendo alterada;</li>
     *   <li>começam antes de {@code fim} e depois de {@code inicioJanela}.</li>
     * </ul>
     * O banco não sabe quando cada agenda termina (o término é calculado em Java, somando as
     * durações), então a consulta só faz o primeiro corte. O AgendaService confere a sobreposição
     * exata em seguida.
     *
     * <p>Esta é uma consulta JPQL escrita à mão com {@code @Query}: ela fala em entidades e campos
     * Java ({@code Agenda a}, {@code a.itens}), não em tabelas e colunas. {@code :nome} são
     * parâmetros preenchidos pelos argumentos com {@code @Param}. O {@code distinct} evita repetir
     * a mesma agenda quando ela tem dois itens com o mesmo profissional.
     */
    @Query("""
            select distinct a from Agenda a
            join a.itens i
            where i.profissional.id in :profissionalIds
              and a.status <> :statusIgnorado
              and a.id <> :agendaId
              and a.dataHora < :fim
              and a.dataHora > :inicioJanela
            """)
    List<Agenda> buscarPossiveisConflitos(@Param("profissionalIds") Collection<Long> profissionalIds,
                                          @Param("statusIgnorado") StatusAgendamento statusIgnorado,
                                          @Param("agendaId") Long agendaId,
                                          @Param("fim") LocalDateTime fim,
                                          @Param("inicioJanela") LocalDateTime inicioJanela);
}
