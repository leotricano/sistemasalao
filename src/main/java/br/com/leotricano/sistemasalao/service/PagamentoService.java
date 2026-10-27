package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.model.Agenda;
import br.com.leotricano.sistemasalao.model.FormaPagamento;
import br.com.leotricano.sistemasalao.model.Pagamento;
import br.com.leotricano.sistemasalao.repository.AgendaRepository;
import br.com.leotricano.sistemasalao.repository.PagamentoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Registro dos pagamentos de cada agendamento (RF05). Estes registros são a base do
 * relatório de faturamento. TODO(#17): relatório por período e profissional (RF07).
 */
@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final AgendaRepository agendaRepository;

    public PagamentoService(PagamentoRepository pagamentoRepository, AgendaRepository agendaRepository) {
        this.pagamentoRepository = pagamentoRepository;
        this.agendaRepository = agendaRepository;
    }

    public List<Pagamento> listarPorAgenda(Long agendaId) {
        return pagamentoRepository.findByAgendaId(agendaId);
    }

    /**
     * Registra um pagamento para a agenda, com a data e hora atuais.
     * Pode ser chamado várias vezes para a mesma agenda (pagamento dividido).
     *
     * <p>TODO: recusar valor que faça a soma dos pagamentos passar do total da agenda.
     *
     * @return o pagamento salvo, ou {@code null} se a agenda não existir
     */
    public Pagamento registrar(Long agendaId, BigDecimal valor, FormaPagamento formaPagamento) {
        Optional<Agenda> agendaOpt = agendaRepository.findById(agendaId);
        if (agendaOpt.isEmpty()) {
            return null;
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setAgenda(agendaOpt.get());
        pagamento.setValor(valor);
        pagamento.setFormaPagamento(formaPagamento);
        pagamento.setDataHora(LocalDateTime.now()); // o momento é decidido pelo servidor, não pelo cliente

        return pagamentoRepository.save(pagamento);
    }

    public void deletar(Long id) {
        pagamentoRepository.deleteById(id);
    }
}
