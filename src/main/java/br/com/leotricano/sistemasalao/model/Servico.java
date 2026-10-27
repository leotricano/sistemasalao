package br.com.leotricano.sistemasalao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Serviço oferecido pelo salão, como corte, coloração ou manicure (RF02). Tabela {@code servico}.
 *
 * <p>Quem pode fazer cada serviço fica na relação com {@link Profissional}.
 */
@Entity
public class Servico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    /**
     * Duração em minutos (coluna {@code duracao_minutos}). É número, e não texto, para permitir
     * calcular o horário de término de um atendimento e somar a duração de vários serviços (#11).
     */
    @NotNull
    @Positive // recusa 0 e valores negativos
    private Integer duracaoMinutos;

    /** Preço atual. {@code BigDecimal} porque {@code double} erra em contas com dinheiro. */
    @NotNull
    private BigDecimal preco;

    /**
     * Profissionais que fazem este serviço. É o "outro lado" da relação N:N: o {@code mappedBy}
     * diz que quem manda na tabela {@code profissional_servico} é o campo {@code servicos} de
     * {@link Profissional}. Por isso, para vincular, altera-se o profissional, não o serviço.
     */
    @ManyToMany(mappedBy = "servicos")
    private Set<Profissional> profissionais = new HashSet<>();

    /** Construtor vazio exigido pelo JPA e pelo Jackson. */
    protected Servico() {}

    // Getters e setters

    public String getNome() {
        return nome;
    }

    public Set<Profissional> getProfissionais() {
        return profissionais;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public void setDuracaoMinutos(Integer duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
