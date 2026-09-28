package br.com.leotricano.sistemasalao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
public class ItemAgendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @NotNull
    private Agenda agenda;

    @ManyToOne
    @NotNull
    private Profissional profissional;

    @ManyToOne
    @NotNull
    private Servico servico;

    @NotNull
    private BigDecimal precoCobrado;

    protected ItemAgendamento(){}

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
