package br.com.leotricano.sistemasalao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Um pagamento recebido por um agendamento (RF05). Tabela {@code pagamento}.
 *
 * <p>Uma agenda pode ter vários pagamentos, por exemplo metade no Pix e metade no cartão.
 * TODO: impedir que a soma dos pagamentos passe do total da agenda (hoje a API aceita).
 */
@Entity
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Agenda paga (coluna {@code agenda_id}). A ligação é só deste lado: a Agenda não tem lista de pagamentos. */
    @ManyToOne
    @NotNull
    private Agenda agenda;

    @NotNull
    private BigDecimal valor;

    /** Gravado como texto ("PIX") pelo mesmo motivo do status da Agenda. */
    @Enumerated(EnumType.STRING)
    @NotNull
    private FormaPagamento formaPagamento;

    /** Momento do registro. Preenchido pelo PagamentoService com a hora atual, não pelo cliente da API. */
    @NotNull
    private LocalDateTime dataHora;

    /** Construtor vazio. É público porque o PagamentoService cria pagamentos com {@code new Pagamento()}. */
    public Pagamento() {}

    // Getters e setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Agenda getAgenda() {
        return agenda;
    }

    public void setAgenda(Agenda agenda) {
        this.agenda = agenda;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }
}
