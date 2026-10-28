package br.com.leotricano.sistemasalao.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Cliente do salão (RF01). Vira a tabela {@code cliente} no banco.
 *
 * <p>O histórico de atendimentos (RF06) não fica aqui: ele é montado a partir das {@link Agenda}s
 * que apontam para este cliente, em {@code ClienteService.historico} ({@code GET /clientes/{id}/historico}, #16).
 */
@Entity // esta classe é uma tabela; o Hibernate cria e atualiza a tabela a partir dos campos
public class Cliente {

    @Id // chave primária
    @GeneratedValue(strategy = GenerationType.IDENTITY) // o próprio PostgreSQL gera o número (1, 2, 3...)
    private Long id;

    // As anotações de validação só são checadas quando o controller usa @Valid.
    @NotBlank // não pode ser nulo, vazio nem só espaços
    private String nome;

    @Email // precisa ter formato de e-mail
    @NotBlank
    private String email;

    @NotBlank
    private String telefone;

    /** Construtor vazio exigido pelo JPA e pelo Jackson (que montam o objeto e depois preenchem os campos). */
    protected Cliente(){}

    // Getters e setters: usados pelo Hibernate para ler/gravar no banco e pelo Jackson para ler/gravar JSON.

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
