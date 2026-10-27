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
 */
public interface AgendaRepository extends JpaRepository<Agenda, Long> {

    /**
     * Consulta da tela de agenda (#15, RF04). Cada filtro é opcional: quando o parâmetro chega
     * {@code null}, a condição {@code :x is null or ...} vira verdadeira e o filtro é ignorado.
     * Assim uma única consulta atende "o dia", "a semana", "só o William", "só os agendados" e
     * qualquer combinação deles.
     * <ul>
     *   <li>{@code inicio} / {@code fim}: agendas que começam a partir de {@code inicio} e antes de
     *       {@code fim} ({@code fim} não entra: para o dia 03/11, o service manda
     *       03/11 00:00 até 04/11 00:00);</li>
     *   <li>{@code profissionalId}: agendas em que <b>algum</b> serviço é com esse profissional.
     *       O {@code exists} com subconsulta evita repetir a agenda quando ele faz dois serviços
     *       nela (o que aconteceria com {@code join});</li>
     *   <li>{@code status}: só agendas nesse status.</li>
     * </ul>
     * Por que o {@code cast}: no SQL, {@code ? is null} sozinho não diz ao PostgreSQL de que tipo
     * é o parâmetro, e ele recusa a consulta ("could not determine data type of parameter").
     * O {@code cast} informa o tipo.
     *
     * <p>O resultado vem em ordem de horário, que é a ordem da grade da agenda; o {@code a.id}
     * desempata duas agendas no mesmo horário, para a ordem não mudar entre uma chamada e outra.
     */
    @Query("""
            select a from Agenda a
            where (cast(:inicio as LocalDateTime) is null or a.dataHora >= :inicio)
              and (cast(:fim as LocalDateTime) is null or a.dataHora < :fim)
              and (cast(:status as String) is null or a.status = :status)
              and (cast(:profissionalId as Long) is null or exists (
                    select 1 from ItemAgendamento i
                    where i.agenda = a and i.profissional.id = :profissionalId))
            order by a.dataHora, a.id
            """)
    List<Agenda> buscarComFiltros(@Param("inicio") LocalDateTime inicio,
                                  @Param("fim") LocalDateTime fim,
                                  @Param("profissionalId") Long profissionalId,
                                  @Param("status") StatusAgendamento status);

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
