package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.exception.RecursoNaoEncontradoException;
import br.com.leotricano.sistemasalao.exception.RegraNegocioException;
import br.com.leotricano.sistemasalao.model.Agenda;
import br.com.leotricano.sistemasalao.model.FormaPagamento;
import br.com.leotricano.sistemasalao.model.Pagamento;
import br.com.leotricano.sistemasalao.repository.AgendaRepository;
import br.com.leotricano.sistemasalao.repository.PagamentoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    /** @throws RecursoNaoEncontradoException se a agenda não existir (404), em vez de devolver lista vazia */
    public List<Pagamento> listarPorAgenda(Long agendaId) {
        buscarAgenda(agendaId);
        return pagamentoRepository.findByAgendaId(agendaId);
    }

    /**
     * Registra um pagamento para a agenda, com a data e hora atuais.
     * Pode ser chamado várias vezes para a mesma agenda (pagamento dividido).
     *
     * <p>TODO: recusar valor que faça a soma dos pagamentos passar do total da agenda.
     *
     * @throws RecursoNaoEncontradoException se a agenda não existir (404)
     * @throws RegraNegocioException se o valor for zero ou negativo (400)
     */
    public Pagamento registrar(Long agendaId, BigDecimal valor, FormaPagamento formaPagamento) {
        Agenda agenda = buscarAgenda(agendaId);

        // compareTo em vez de "<=": BigDecimal é objeto e não aceita os operadores de comparação
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O valor do pagamento precisa ser maior que zero.");
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setAgenda(agenda);
        pagamento.setValor(valor);
        pagamento.setFormaPagamento(formaPagamento);
        pagamento.setDataHora(LocalDateTime.now()); // o momento é decidido pelo servidor, não pelo cliente

        return pagamentoRepository.save(pagamento);
    }

    /**
     * Remove um pagamento lançado errado. Confere se o pagamento é mesmo da agenda informada na URL,
     * para que {@code DELETE /agendas/1/pagamentos/7} não apague um pagamento da agenda 2.
     *
     * @throws RecursoNaoEncontradoException se o pagamento não existir ou for de outra agenda (404)
     */
    public void deletar(Long agendaId, Long id) {
        Pagamento pagamento = pagamentoRepository.findById(id)
                .filter(p -> p.getAgenda().getId().equals(agendaId))
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pagamento " + id + " não encontrado na agenda " + agendaId + "."));
        pagamentoRepository.delete(pagamento);
    }

    private Agenda buscarAgenda(Long agendaId) {
        return agendaRepository.findById(agendaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agenda " + agendaId + " não encontrada."));
    }
}
