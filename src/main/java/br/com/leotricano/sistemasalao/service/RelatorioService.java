package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.dto.RelatorioFaturamentoResponse;
import br.com.leotricano.sistemasalao.dto.RelatorioFaturamentoResponse.PorFormaPagamento;
import br.com.leotricano.sistemasalao.dto.RelatorioFaturamentoResponse.PorProfissional;
import br.com.leotricano.sistemasalao.exception.RecursoNaoEncontradoException;
import br.com.leotricano.sistemasalao.exception.RegraNegocioException;
import br.com.leotricano.sistemasalao.model.*;
import br.com.leotricano.sistemasalao.repository.AgendaRepository;
import br.com.leotricano.sistemasalao.repository.PagamentoRepository;
import br.com.leotricano.sistemasalao.repository.ProfissionalRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Relatório de faturamento por período e profissional (#17, RF07).
 *
 * <p><b>O que entra:</b> só agendas CONCLUÍDAS que começaram no período. Agenda ainda AGENDADA
 * não é dinheiro garantido, e CANCELADA não aconteceu.
 *
 * <p><b>Faturamento x recebido:</b> o faturamento soma o {@code precoCobrado} dos serviços feitos
 * (o valor do dia, ver ItemAgendamento). O recebido soma os {@link Pagamento}s dessas agendas. Os
 * dois podem ser diferentes: o cliente pode pagar depois (concluir sem pagamento é permitido).
 *
 * <p><b>Com filtro de profissional:</b> faturamento, atendimentos e a lista por profissional contam
 * só os serviços dele. Os pagamentos não são divididos por profissional (um Pix paga a visita
 * inteira), então o recebido mostra os pagamentos das agendas em que ele atendeu.
 */
@Service
public class RelatorioService {

    /** Maior período aceito (um ano), para o relatório anual caber e ninguém pedir décadas de uma vez. */
    private static final int MAXIMO_DIAS_RELATORIO = 366;

    private final AgendaRepository agendaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ProfissionalRepository profissionalRepository;

    public RelatorioService(AgendaRepository agendaRepository,
                            PagamentoRepository pagamentoRepository,
                            ProfissionalRepository profissionalRepository) {
        this.agendaRepository = agendaRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.profissionalRepository = profissionalRepository;
    }

    /**
     * Monta o relatório do período. Período sem atendimento não é erro: volta tudo zerado.
     *
     * @param de             primeiro dia (inclusive), obrigatório
     * @param ate            último dia (inclusive), obrigatório
     * @param profissionalId só os serviços desse profissional, ou {@code null} para o salão inteiro
     * @throws RegraNegocioException (400) se faltar data, se {@code de} for depois de {@code ate}
     *         ou se o período passar de {@value #MAXIMO_DIAS_RELATORIO} dias
     * @throws RecursoNaoEncontradoException (404) se o profissional não existir
     */
    public RelatorioFaturamentoResponse faturamento(LocalDate de, LocalDate ate, Long profissionalId) {
        validarPeriodo(de, ate);

        if (profissionalId != null && !profissionalRepository.existsById(profissionalId)) {
            throw new RecursoNaoEncontradoException("Profissional " + profissionalId + " não encontrado.");
        }

        // Reaproveita a consulta da agenda (#15): período, profissional e status CONCLUIDO.
        // O fim é 00:00 do dia seguinte ao último, sem incluir esse instante.
        List<Agenda> agendas = agendaRepository.buscarComFiltros(
                de.atStartOfDay(), ate.plusDays(1).atStartOfDay(), profissionalId, StatusAgendamento.CONCLUIDO);

        // Itens que contam: todos, ou só os do profissional filtrado
        List<ItemAgendamento> itens = new ArrayList<>();
        for (Agenda agenda : agendas) {
            for (ItemAgendamento item : agenda.getItens()) {
                if (profissionalId == null || item.getProfissional().getId().equals(profissionalId)) {
                    itens.add(item);
                }
            }
        }

        BigDecimal faturamento = somarPrecos(itens);

        // Set não guarda repetidos: o mesmo cliente atendido 3 vezes conta 1
        Set<Long> clientes = new HashSet<>();
        for (Agenda agenda : agendas) {
            clientes.add(agenda.getCliente().getId());
        }

        // Ticket médio = quanto cada atendimento rendeu, em média. Sem atendimento, divide por zero: fica 0.
        BigDecimal ticketMedio = agendas.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : faturamento.divide(BigDecimal.valueOf(agendas.size()), 2, RoundingMode.HALF_UP);

        List<Pagamento> pagamentos = agendas.isEmpty()
                ? List.of() // "in ()" vazio não é aceito em SQL
                : pagamentoRepository.findByAgendaIdIn(agendas.stream().map(Agenda::getId).toList());

        BigDecimal totalRecebido = pagamentos.stream()
                .map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RelatorioFaturamentoResponse(de, ate, profissionalId, agendas.size(), clientes.size(),
                faturamento, ticketMedio, totalRecebido, porProfissional(itens), porFormaPagamento(pagamentos));
    }

    private void validarPeriodo(LocalDate de, LocalDate ate) {
        if (de == null || ate == null) {
            throw new RegraNegocioException("Informe as duas datas do período: 'de' e 'ate'.");
        }
        if (de.isAfter(ate)) {
            throw new RegraNegocioException("A data 'de' precisa ser igual ou anterior à data 'ate'.");
        }
        long dias = ChronoUnit.DAYS.between(de, ate) + 1; // +1 porque os dois dias contam
        if (dias > MAXIMO_DIAS_RELATORIO) {
            throw new RegraNegocioException("O período pode ter no máximo " + MAXIMO_DIAS_RELATORIO
                    + " dias (foram pedidos " + dias + ").");
        }
    }

    /**
     * Agrupa os itens por profissional: o mapa guarda, para cada id de profissional, a lista dos
     * itens dele. Depois cada grupo vira uma linha, e as linhas são ordenadas pelo faturamento.
     * A chave é o id (e não a entidade) porque a entidade não tem {@code equals} próprio.
     */
    private List<PorProfissional> porProfissional(List<ItemAgendamento> itens) {
        Map<Long, List<ItemAgendamento>> grupos = new LinkedHashMap<>();
        for (ItemAgendamento item : itens) {
            // computeIfAbsent: cria a lista na primeira vez que o profissional aparece, depois só adiciona
            grupos.computeIfAbsent(item.getProfissional().getId(), id -> new ArrayList<>()).add(item);
        }

        List<PorProfissional> linhas = new ArrayList<>();
        for (List<ItemAgendamento> grupo : grupos.values()) {
            Profissional profissional = grupo.get(0).getProfissional();
            // Um profissional pode fazer dois serviços na mesma agenda: conta 1 atendimento
            long agendasDistintas = grupo.stream().map(i -> i.getAgenda().getId()).distinct().count();
            linhas.add(new PorProfissional(profissional.getId(), profissional.getNome(),
                    (int) agendasDistintas, somarPrecos(grupo)));
        }

        linhas.sort(Comparator.comparing(PorProfissional::faturamento).reversed());
        return linhas;
    }

    /** Uma linha para cada forma de pagamento, na ordem do enum, mesmo as que não tiveram pagamento. */
    private List<PorFormaPagamento> porFormaPagamento(List<Pagamento> pagamentos) {
        List<PorFormaPagamento> linhas = new ArrayList<>();
        for (FormaPagamento forma : FormaPagamento.values()) {
            BigDecimal total = pagamentos.stream()
                    .filter(p -> p.getFormaPagamento() == forma)
                    .map(Pagamento::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            linhas.add(new PorFormaPagamento(forma, total));
        }
        return linhas;
    }

    private BigDecimal somarPrecos(List<ItemAgendamento> itens) {
        return itens.stream()
                .map(ItemAgendamento::getPrecoCobrado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
