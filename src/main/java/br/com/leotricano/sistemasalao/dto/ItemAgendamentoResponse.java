package br.com.leotricano.sistemasalao.dto;

import br.com.leotricano.sistemasalao.model.ItemAgendamento;

import java.math.BigDecimal;

/**
 * DTO = Data Transfer Object: o formato do JSON que a API devolve, separado da entidade do banco.
 * Este representa um serviço dentro de um agendamento.
 *
 * <p>Não tem o campo {@code agenda} de propósito: o item apontar de volta para a agenda era o que
 * causava a recursão infinita no JSON (#10). Profissional e serviço viram só o nome, que é o que a
 * tela mostra.
 *
 * <p>{@code record}: classe imutável só de dados. O Java gera sozinho o construtor, os métodos de
 * leitura ({@code nomeServico()}, {@code precoCobrado()}...), {@code equals} e {@code toString}.
 */
public record ItemAgendamentoResponse(Long id, String nomeServico, String nomeProfissional,
                                      Integer duracaoMinutos, BigDecimal precoCobrado) {

    /** Monta o DTO a partir da entidade. Os DTOs deste pacote seguem esse padrão de método {@code from}. */
    public static ItemAgendamentoResponse from(ItemAgendamento itemAgendamento) {
        return new ItemAgendamentoResponse(
                itemAgendamento.getId(),
                itemAgendamento.getServico().getNome(),
                itemAgendamento.getProfissional().getNome(),
                itemAgendamento.getServico().getDuracaoMinutos(),
                itemAgendamento.getPrecoCobrado()
        );
    }
}
