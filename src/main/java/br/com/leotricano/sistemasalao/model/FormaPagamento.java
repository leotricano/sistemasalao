package br.com.leotricano.sistemasalao.model;

/**
 * Formas de pagamento aceitas pelo salão (RF05).
 *
 * <p>Um {@code enum} é uma lista fechada de valores: a API recusa qualquer coisa fora desta lista.
 * No JSON e no banco aparecem pelo nome: {@code "PIX"}, {@code "CARTAO"}, {@code "DINHEIRO"}.
 */
public enum FormaPagamento {
    DINHEIRO, CARTAO, PIX
}
