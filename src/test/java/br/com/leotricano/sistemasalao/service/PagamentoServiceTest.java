package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.exception.RecursoNaoEncontradoException;
import br.com.leotricano.sistemasalao.exception.RegraNegocioException;
import br.com.leotricano.sistemasalao.model.*;
import br.com.leotricano.sistemasalao.repository.AgendaRepository;
import br.com.leotricano.sistemasalao.repository.PagamentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Testes de unidade das regras do {@link PagamentoService} (RF05, #26). */
@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock private PagamentoRepository pagamentoRepository;
    @Mock private AgendaRepository agendaRepository;

    @InjectMocks private PagamentoService pagamentoService;

    private final Agenda agenda = Fabrica.agenda(1L, Fabrica.cliente(1L, "Ana"),
            LocalDateTime.of(2030, 3, 10, 14, 0), StatusAgendamento.AGENDADO);

    @Test
    @DisplayName("Registra pagamento com a data e hora do servidor")
    void registraPagamento() {
        when(agendaRepository.findById(1L)).thenReturn(Optional.of(agenda));
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Pagamento pagamento = pagamentoService.registrar(1L, new BigDecimal("80.00"), FormaPagamento.PIX);

        assertThat(pagamento.getAgenda()).isSameAs(agenda);
        assertThat(pagamento.getFormaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(pagamento.getDataHora()).isNotNull();
    }

    @Test
    @DisplayName("Recusa pagamento com valor zero ou negativo (400)")
    void recusaValorNaoPositivo() {
        when(agendaRepository.findById(1L)).thenReturn(Optional.of(agenda));

        assertThatThrownBy(() -> pagamentoService.registrar(1L, BigDecimal.ZERO, FormaPagamento.PIX))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> pagamentoService.registrar(1L, new BigDecimal("-10"), FormaPagamento.PIX))
                .isInstanceOf(RegraNegocioException.class);
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não apaga pagamento de outra agenda (404)")
    void naoApagaPagamentoDeOutraAgenda() {
        Pagamento daAgenda1 = Fabrica.pagamento(7L, agenda, "80.00", FormaPagamento.PIX);
        when(pagamentoRepository.findById(7L)).thenReturn(Optional.of(daAgenda1));

        assertThatThrownBy(() -> pagamentoService.deletar(2L, 7L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(pagamentoRepository, never()).delete(any());
    }
}
