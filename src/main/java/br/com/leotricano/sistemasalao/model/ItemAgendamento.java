package br.com.leotricano.sistemasalao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Um serviço dentro de um agendamento: qual serviço, com qual profissional e por qual preço.
 * Tabela {@code item_agendamento}.
 *
 * <p>Exemplo: a agenda da Ana Paula às 14h pode ter dois itens, "Corte com William" e
 * "Coloração com William".
 */
@Entity
public class ItemAgendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Agenda a que este item pertence (coluna {@code agenda_id}). É o lado "dono" da relação com {@link Agenda#getItens()}. */
    @ManyToOne
    @NotNull
    private Agenda agenda;

    /** Quem faz este serviço. Precisa ser alguém que tenha o serviço na sua lista (validado no AgendaService). */
    @ManyToOne
    @NotNull
    private Profissional profissional;

    @ManyToOne
    @NotNull
    private Servico servico;

    /**
     * Preço copiado do serviço no momento do agendamento. Se o salão reajustar o preço depois,
     * os atendimentos antigos continuam com o valor que foi realmente cobrado.
     */
    @NotNull
    private BigDecimal precoCobrado;

    /** Construtor vazio. É público porque o AgendaService cria itens com {@code new ItemAgendamento()}. */
    public ItemAgendamento(){}

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

    public Profissional getProfissional() {
        return profissional;
    }

    public void setProfissional(Profissional profissional) {
        this.profissional = profissional;
    }

    public Servico getServico() {
        return servico;
    }

    public void setServico(Servico servico) {
        this.servico = servico;
    }

    public BigDecimal getPrecoCobrado() {
        return precoCobrado;
    }

    public void setPrecoCobrado(BigDecimal precoCobrado) {
        this.precoCobrado = precoCobrado;
    }
}
