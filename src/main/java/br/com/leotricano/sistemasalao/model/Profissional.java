package br.com.leotricano.sistemasalao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.HashSet;
import java.util.Set;

/**
 * Profissional que atende no salão: cabeleireiro, manicure etc. Tabela {@code profissional}.
 *
 * <p>Cada profissional tem a lista de serviços que sabe fazer. O {@code AgendaService} usa essa
 * lista para recusar um agendamento de serviço que o profissional não faz.
 */
@Entity
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    /**
     * Especialidade em texto livre (ex.: "Cabeleireiro").
     * TODO(#29): virar uma entidade Especialidade com pacote de serviços padrão (RF09).
     */
    @NotBlank
    private String especialidade;

    /**
     * Serviços que este profissional faz. Relação N:N: um profissional faz vários serviços e um
     * serviço é feito por vários profissionais. O {@code @JoinTable} cria a tabela intermediária
     * {@code profissional_servico}, com uma linha para cada par (profissional_id, servico_id).
     * Este é o lado "dono" da relação: alterar este Set e salvar o profissional grava nessa tabela.
     */
    @ManyToMany
    @JoinTable(
            name = "profissional_servico",
            joinColumns = @JoinColumn(name = "profissional_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    private Set<Servico> servicos = new HashSet<>(); // Set: o mesmo serviço não entra duas vezes

    /** Construtor vazio exigido pelo JPA e pelo Jackson. */
    protected Profissional(){}

    // Getters e setters

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public void setEspecialidade(String especialidade) {
        this.especialidade = especialidade;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /** Sem setter de propósito: os serviços são incluídos e removidos direto neste Set (ver ProfissionalService). */
    public Set<Servico> getServicos() {
        return servicos;
    }
}
