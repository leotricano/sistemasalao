package br.com.leotricano.sistemasalao.dto;

import br.com.leotricano.sistemasalao.model.FormaPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * JSON de {@code GET /relatorios/faturamento} (#17, RF07): o fechamento de um período, como o
 * relatório do fim do mês que o dono do salão pediu.
 *
 * <p>Só entram agendas <b>CONCLUÍDAS</b> que começaram dentro do período: agendado ainda não é
 * faturamento e cancelado não aconteceu. As regras ficam no {@code RelatorioService}.
 *
 * @param de                 primeiro dia do período (inclusive)
 * @param ate                último dia do período (inclusive)
 * @param profissionalId     profissional filtrado, ou {@code null} para o salão inteiro
 * @param atendimentos       quantas agendas concluídas entraram no relatório
 * @param clientesAtendidos  quantos clientes diferentes foram atendidos
 * @param faturamento        soma do {@code precoCobrado} dos serviços feitos (com filtro, só os do profissional)
 * @param ticketMedio        faturamento ÷ atendimentos, com 2 casas; zero se não houve atendimento
 * @param totalRecebido      soma dos pagamentos registrados para essas agendas
 * @param porProfissional    faturamento de cada profissional, do maior para o menor
 * @param porFormaPagamento  quanto entrou em cada forma de pagamento (todas aparecem, mesmo zeradas)
 */
public record RelatorioFaturamentoResponse(LocalDate de, LocalDate ate, Long profissionalId,
                                           int atendimentos, int clientesAtendidos,
                                           BigDecimal faturamento, BigDecimal ticketMedio,
                                           BigDecimal totalRecebido,
                                           List<PorProfissional> porProfissional,
                                           List<PorFormaPagamento> porFormaPagamento) {

    /**
     * Uma linha da tabela "por profissional".
     *
     * @param atendimentos em quantas agendas concluídas esse profissional fez pelo menos um serviço
     * @param faturamento  soma do {@code precoCobrado} dos serviços que ele fez
     */
    public record PorProfissional(Long profissionalId, String nome, int atendimentos, BigDecimal faturamento) {
    }

    /** Uma linha da tabela "por forma de pagamento". */
    public record PorFormaPagamento(FormaPagamento formaPagamento, BigDecimal total) {
    }
}
