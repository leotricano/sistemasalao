package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.dto.RelatorioFaturamentoResponse;
import br.com.leotricano.sistemasalao.exception.RecursoNaoEncontradoException;
import br.com.leotricano.sistemasalao.exception.RegraNegocioException;
import br.com.leotricano.sistemasalao.model.*;
import br.com.leotricano.sistemasalao.repository.AgendaRepository;
import br.com.leotricano.sistemasalao.repository.PagamentoRepository;
import br.com.leotricano.sistemasalao.repository.ProfissionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes de unidade dos cálculos do relatório de faturamento (#17, RF07), com o banco trocado por mocks.
 *
 * <p>Cenário de novembro/2030 usado nos testes:
 * <pre>
 *   agenda A  Ana   William corte 80 + Carla manicure 40   pago 120 no Pix
 *   agenda B  Ana   William corte 80                       pago  80 no cartão
 *   agenda C  Bia   Carla manicure 40                      não pago
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    private static final LocalDate DE = LocalDate.of(2030, 11, 1);
    private static final LocalDate ATE = LocalDate.of(2030, 11, 30);

    @Mock private AgendaRepository agendaRepository;
    @Mock private PagamentoRepository pagamentoRepository;
    @Mock private ProfissionalRepository profissionalRepository;

    @InjectMocks private RelatorioService relatorioService;

    private Profissional william;
    private Agenda a;
    private Agenda b;
    private Agenda c;

    @BeforeEach
    void prepararCenario() {
        Servico corte = Fabrica.servico(10L, "Corte", 60, "80.00");
        Servico manicure = Fabrica.servico(11L, "Manicure", 45, "40.00");
        william = Fabrica.profissional(20L, "William", corte);
        Profissional carla = Fabrica.profissional(21L, "Carla", manicure);
        Cliente ana = Fabrica.cliente(1L, "Ana");
        Cliente bia = Fabrica.cliente(2L, "Bia");

        a = Fabrica.agenda(1L, ana, LocalDateTime.of(2030, 11, 5, 10, 0), StatusAgendamento.CONCLUIDO);
        Fabrica.item(100L, a, corte, william);
        Fabrica.item(101L, a, manicure, carla);

        b = Fabrica.agenda(2L, ana, LocalDateTime.of(2030, 11, 12, 10, 0), StatusAgendamento.CONCLUIDO);
        Fabrica.item(102L, b, corte, william);

        c = Fabrica.agenda(3L, bia, LocalDateTime.of(2030, 11, 20, 15, 0), StatusAgendamento.CONCLUIDO);
        Fabrica.item(103L, c, manicure, carla);
    }

    @Test
    @DisplayName("Calcula atendimentos, clientes, faturamento, ticket médio e recebido do salão")
    void calculaTotaisDoSalao() {
        when(agendaRepository.buscarComFiltros(any(), any(), isNull(), eq(StatusAgendamento.CONCLUIDO)))
                .thenReturn(List.of(a, b, c));
        when(pagamentoRepository.findByAgendaIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(
                Fabrica.pagamento(1L, a, "120.00", FormaPagamento.PIX),
                Fabrica.pagamento(2L, b, "80.00", FormaPagamento.CARTAO)));

        RelatorioFaturamentoResponse r = relatorioService.faturamento(DE, ATE, null);

        assertThat(r.atendimentos()).isEqualTo(3);
        assertThat(r.clientesAtendidos()).isEqualTo(2); // Ana veio duas vezes e conta uma
        assertThat(r.faturamento()).isEqualByComparingTo("240.00");
        assertThat(r.ticketMedio()).isEqualByComparingTo("80.00");
        assertThat(r.totalRecebido()).isEqualByComparingTo("200.00"); // C ainda não pagou
    }

    @Test
    @DisplayName("Separa o faturamento por profissional, do maior para o menor")
    void separaPorProfissional() {
        when(agendaRepository.buscarComFiltros(any(), any(), isNull(), eq(StatusAgendamento.CONCLUIDO)))
                .thenReturn(List.of(a, b, c));
        when(pagamentoRepository.findByAgendaIdIn(any())).thenReturn(List.of());

        List<RelatorioFaturamentoResponse.PorProfissional> linhas =
                relatorioService.faturamento(DE, ATE, null).porProfissional();

        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0).nome()).isEqualTo("William");
        assertThat(linhas.get(0).atendimentos()).isEqualTo(2);
        assertThat(linhas.get(0).faturamento()).isEqualByComparingTo("160.00");
        assertThat(linhas.get(1).nome()).isEqualTo("Carla");
        assertThat(linhas.get(1).faturamento()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("Totaliza por forma de pagamento, trazendo também as formas zeradas")
    void totalizaPorFormaDePagamento() {
        when(agendaRepository.buscarComFiltros(any(), any(), isNull(), eq(StatusAgendamento.CONCLUIDO)))
                .thenReturn(List.of(a, b, c));
        when(pagamentoRepository.findByAgendaIdIn(any())).thenReturn(List.of(
                Fabrica.pagamento(1L, a, "120.00", FormaPagamento.PIX),
                Fabrica.pagamento(2L, b, "80.00", FormaPagamento.CARTAO)));

        List<RelatorioFaturamentoResponse.PorFormaPagamento> formas =
                relatorioService.faturamento(DE, ATE, null).porFormaPagamento();

        assertThat(formas).extracting(RelatorioFaturamentoResponse.PorFormaPagamento::formaPagamento)
                .containsExactly(FormaPagamento.DINHEIRO, FormaPagamento.CARTAO, FormaPagamento.PIX);
        assertThat(formas.get(0).total()).isEqualByComparingTo("0");
        assertThat(formas.get(1).total()).isEqualByComparingTo("80.00");
        assertThat(formas.get(2).total()).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("Com filtro de profissional, conta só os serviços dele")
    void filtraPorProfissional() {
        when(profissionalRepository.existsById(20L)).thenReturn(true);
        // O banco já devolve só as agendas em que o William atendeu
        when(agendaRepository.buscarComFiltros(any(), any(), eq(20L), eq(StatusAgendamento.CONCLUIDO)))
                .thenReturn(List.of(a, b));
        when(pagamentoRepository.findByAgendaIdIn(any())).thenReturn(List.of());

        RelatorioFaturamentoResponse r = relatorioService.faturamento(DE, ATE, 20L);

        assertThat(r.atendimentos()).isEqualTo(2);
        assertThat(r.faturamento()).isEqualByComparingTo("160.00"); // a manicure da Carla na agenda A não entra
        assertThat(r.porProfissional()).hasSize(1);
        assertThat(r.porProfissional().get(0).profissionalId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Período sem atendimentos volta tudo zerado, sem erro")
    void periodoSemAtendimentos() {
        when(agendaRepository.buscarComFiltros(any(), any(), isNull(), eq(StatusAgendamento.CONCLUIDO)))
                .thenReturn(List.of());

        RelatorioFaturamentoResponse r = relatorioService.faturamento(DE, ATE, null);

        assertThat(r.atendimentos()).isZero();
        assertThat(r.faturamento()).isEqualByComparingTo("0");
        assertThat(r.ticketMedio()).isEqualByComparingTo("0");
        assertThat(r.porProfissional()).isEmpty();
        verifyNoInteractions(pagamentoRepository); // nem consulta pagamentos
    }

    @Test
    @DisplayName("Considera o período de dias inteiros: de 00:00 do primeiro dia até 00:00 do dia seguinte ao último")
    void periodoDeDiasInteiros() {
        when(agendaRepository.buscarComFiltros(any(), any(), any(), any())).thenReturn(List.of());

        relatorioService.faturamento(DE, ATE, null);

        verify(agendaRepository).buscarComFiltros(DE.atStartOfDay(), ATE.plusDays(1).atStartOfDay(),
                null, StatusAgendamento.CONCLUIDO);
    }

    @Test
    @DisplayName("Recusa período invertido, sem data ou maior que um ano (400)")
    void recusaPeriodoInvalido() {
        assertThatThrownBy(() -> relatorioService.faturamento(ATE, DE, null)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> relatorioService.faturamento(DE, null, null)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> relatorioService.faturamento(DE, DE.plusYears(2), null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("366");
        verifyNoInteractions(agendaRepository);
    }

    @Test
    @DisplayName("Profissional que não existe dá 404")
    void profissionalInexistente() {
        when(profissionalRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> relatorioService.faturamento(DE, ATE, 99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
