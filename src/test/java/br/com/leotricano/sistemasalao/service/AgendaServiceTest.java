package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.exception.ConflitoHorarioException;
import br.com.leotricano.sistemasalao.exception.RegraNegocioException;
import br.com.leotricano.sistemasalao.model.*;
import br.com.leotricano.sistemasalao.repository.*;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes de unidade das regras do {@link AgendaService} (#26).
 *
 * <p><b>Teste de unidade</b> testa uma classe sozinha. Os repositórios são trocados por
 * <i>mocks</i> do Mockito ({@code @Mock}): objetos falsos que devolvem o que o teste mandar
 * ({@code when(...).thenReturn(...)}), sem banco de dados. O {@code @InjectMocks} cria o service
 * real passando esses mocks no construtor. Cada teste segue o padrão <i>dado / quando / então</i>.
 */
@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    private static final LocalDateTime DIA_10_14H = LocalDateTime.of(2030, 3, 10, 14, 0);

    @Mock private AgendaRepository agendaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ServicoRepository servicoRepository;
    @Mock private ProfissionalRepository profissionalRepository;
    @Mock private PagamentoRepository pagamentoRepository;

    @InjectMocks private AgendaService agendaService;

    private Cliente ana;
    private Servico corte;
    private Profissional william;

    @BeforeEach
    void prepararDados() {
        ana = Fabrica.cliente(1L, "Ana Paula");
        corte = Fabrica.servico(10L, "Corte", 60, "80.00");
        william = Fabrica.profissional(20L, "William", corte);
    }

    /** Faz o save do mock devolver o próprio objeto salvo, como o JPA faria. */
    private void saveDevolveOProprioObjeto() {
        when(agendaRepository.save(any(Agenda.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    private void cadastrados(Agenda agenda) {
        when(agendaRepository.findById(agenda.getId())).thenReturn(Optional.of(agenda));
        when(servicoRepository.findById(corte.getId())).thenReturn(Optional.of(corte));
        when(profissionalRepository.findById(william.getId())).thenReturn(Optional.of(william));
    }

    @Test
    @DisplayName("Conflito de horário: recusa serviço que sobrepõe outro atendimento do profissional (409)")
    void recusaServicoQueSobrepoeOutroAtendimento() {
        // dado: William já atende das 14:30 às 15:30
        Agenda existente = Fabrica.agenda(2L, Fabrica.cliente(2L, "Bia"), DIA_10_14H.plusMinutes(30), StatusAgendamento.AGENDADO);
        Fabrica.item(200L, existente, corte, william);

        Agenda nova = Fabrica.agenda(1L, ana, DIA_10_14H, StatusAgendamento.AGENDADO);
        cadastrados(nova);
        when(agendaRepository.buscarPossiveisConflitos(any(), any(), any(), any(), any())).thenReturn(List.of(existente));

        // quando / então: a nova agenda iria das 14:00 às 15:00
        assertThatThrownBy(() -> agendaService.adicionarItem(1L, corte.getId(), william.getId()))
                .isInstanceOf(ConflitoHorarioException.class)
                .hasMessageContaining("William")
                .hasMessageContaining("14:30");

        verify(agendaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Conflito de horário: atendimento que só encosta no outro é permitido")
    void permiteHorarioQueSoEncosta() {
        // dado: William atende das 13:00 às 14:00
        Agenda anterior = Fabrica.agenda(2L, Fabrica.cliente(2L, "Bia"), DIA_10_14H.minusHours(1), StatusAgendamento.AGENDADO);
        Fabrica.item(200L, anterior, corte, william);

        Agenda nova = Fabrica.agenda(1L, ana, DIA_10_14H, StatusAgendamento.AGENDADO);
        cadastrados(nova);
        when(agendaRepository.buscarPossiveisConflitos(any(), any(), any(), any(), any())).thenReturn(List.of(anterior));
        saveDevolveOProprioObjeto();

        // quando: a nova começa às 14:00
        Agenda salva = agendaService.adicionarItem(1L, corte.getId(), william.getId());

        // então: o item entrou, com o preço do dia copiado
        assertThat(salva.getItens()).hasSize(1);
        assertThat(salva.getItens().get(0).getPrecoCobrado()).isEqualByComparingTo("80.00");
        assertThat(salva.calcularFim()).isEqualTo(DIA_10_14H.plusHours(1));
    }

    @Test
    @DisplayName("Recusa serviço que o profissional não faz (400)")
    void recusaServicoQueOProfissionalNaoFaz() {
        Servico manicure = Fabrica.servico(11L, "Manicure", 45, "40.00");
        Agenda agenda = Fabrica.agenda(1L, ana, DIA_10_14H, StatusAgendamento.AGENDADO);
        when(agendaRepository.findById(1L)).thenReturn(Optional.of(agenda));
        when(servicoRepository.findById(11L)).thenReturn(Optional.of(manicure));
        when(profissionalRepository.findById(william.getId())).thenReturn(Optional.of(william));

        assertThatThrownBy(() -> agendaService.adicionarItem(1L, 11L, william.getId()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não faz o serviço Manicure");
    }

    @Test
    @DisplayName("Atendimento concluído não muda mais de status (400)")
    void concluidoNaoMudaDeStatus() {
        Agenda concluida = Fabrica.agenda(1L, ana, DIA_10_14H, StatusAgendamento.CONCLUIDO);
        when(agendaRepository.findById(1L)).thenReturn(Optional.of(concluida));

        assertThatThrownBy(() -> agendaService.atualizarStatus(1L, StatusAgendamento.CANCELADO))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(concluida.getStatus()).isEqualTo(StatusAgendamento.CONCLUIDO);
    }

    @Test
    @DisplayName("Não cancela agendamento que já tem pagamento (400)")
    void naoCancelaAgendaPaga() {
        Agenda agenda = Fabrica.agenda(1L, ana, DIA_10_14H, StatusAgendamento.AGENDADO);
        when(agendaRepository.findById(1L)).thenReturn(Optional.of(agenda));
        when(pagamentoRepository.existsByAgendaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> agendaService.atualizarStatus(1L, StatusAgendamento.CANCELADO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("pagamentos");
    }

    @Test
    @DisplayName("Não conclui agendamento sem serviços (400)")
    void naoConcluiSemServicos() {
        Agenda vazia = Fabrica.agenda(1L, ana, DIA_10_14H, StatusAgendamento.AGENDADO);
        when(agendaRepository.findById(1L)).thenReturn(Optional.of(vazia));

        assertThatThrownBy(() -> agendaService.atualizarStatus(1L, StatusAgendamento.CONCLUIDO))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("Conclui agendamento com serviço")
    void concluiAgendaComServico() {
        Agenda agenda = Fabrica.agenda(1L, ana, DIA_10_14H, StatusAgendamento.AGENDADO);
        Fabrica.item(100L, agenda, corte, william);
        when(agendaRepository.findById(1L)).thenReturn(Optional.of(agenda));
        saveDevolveOProprioObjeto();

        Agenda salva = agendaService.atualizarStatus(1L, StatusAgendamento.CONCLUIDO);

        assertThat(salva.getStatus()).isEqualTo(StatusAgendamento.CONCLUIDO);
    }

    @Test
    @DisplayName("Remarcar: recusa data no passado (400)")
    void remarcarRecusaDataNoPassado() {
        Agenda agenda = Fabrica.agenda(1L, ana, DIA_10_14H, StatusAgendamento.AGENDADO);
        when(agendaRepository.findById(1L)).thenReturn(Optional.of(agenda));

        assertThatThrownBy(() -> agendaService.remarcar(1L, LocalDateTime.now().minusDays(1)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    @DisplayName("Remarcar: recusa agendamento cancelado (400)")
    void remarcarRecusaAgendaCancelada() {
        Agenda cancelada = Fabrica.agenda(1L, ana, DIA_10_14H, StatusAgendamento.CANCELADO);
        when(agendaRepository.findById(1L)).thenReturn(Optional.of(cancelada));

        assertThatThrownBy(() -> agendaService.remarcar(1L, DIA_10_14H.plusDays(1)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("AGENDADO");
    }

    @Test
    @DisplayName("Consulta da agenda: exige as duas datas do período (400)")
    void listarExigeAsDuasDatas() {
        assertThatThrownBy(() -> agendaService.listar(LocalDate.of(2030, 3, 1), null, null, null))
                .isInstanceOf(RegraNegocioException.class);
        verifyNoInteractions(agendaRepository);
    }

    @Test
    @DisplayName("Consulta da agenda: recusa período maior que 62 dias (400)")
    void listarRecusaPeriodoLongo() {
        assertThatThrownBy(() -> agendaService.listar(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 6, 1), null, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("62");
    }
}
