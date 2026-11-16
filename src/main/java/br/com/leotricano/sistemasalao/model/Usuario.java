package br.com.leotricano.sistemasalao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Uma pessoa que entra no sistema com login e senha (#18). Tabela {@code usuario}.
 *
 * <p>A senha <b>nunca</b> é guardada como foi digitada: o {@code UsuarioService} grava só o
 * <i>hash</i> BCrypt dela. Um hash não pode ser desfeito; no login, o BCrypt calcula o hash da
 * senha digitada e compara com o guardado. Assim, nem quem tiver acesso ao banco descobre as senhas.
 */
@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    /** Nome de usuário para entrar (ex.: "william"). {@code unique}: dois usuários não podem ter o mesmo. */
    @NotBlank
    @Column(unique = true)
    private String login;

    /** Hash BCrypt da senha, algo como "$2a$10$N9qo8uLO...". */
    @NotBlank
    private String senhaHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Perfil perfil;

    /**
     * Para o perfil PROFISSIONAL: qual profissional do salão é esta pessoa. É o que permite mostrar
     * a ela só a própria agenda. Fica {@code null} para ADMIN e RECEPCAO.
     */
    @ManyToOne
    private Profissional profissional;

    /** E-mail para receber o código de "esqueci minha senha" (#42). Opcional. */
    @Email
    @Column(unique = true)
    private String email;

    /**
     * Código de recuperação de senha (#42), guardado só como hash BCrypt, igual à senha: quem
     * olhar o banco não consegue usar o código. Fica {@code null} quando não há pedido em aberto.
     */
    private String codigoRecuperacaoHash;

    /** Até quando o código vale (15 minutos depois do pedido). */
    private Instant codigoRecuperacaoExpiraEm;

    /** Tentativas erradas com o código atual; na 5ª o código é descartado. */
    private Integer codigoRecuperacaoTentativas;

    /** Construtor vazio exigido pelo JPA. Público porque o UsuarioService cria usuários. */
    public Usuario() {}

    // Getters e setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public Profissional getProfissional() {
        return profissional;
    }

    public void setProfissional(Profissional profissional) {
        this.profissional = profissional;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCodigoRecuperacaoHash() {
        return codigoRecuperacaoHash;
    }

    public void setCodigoRecuperacaoHash(String codigoRecuperacaoHash) {
        this.codigoRecuperacaoHash = codigoRecuperacaoHash;
    }

    public Instant getCodigoRecuperacaoExpiraEm() {
        return codigoRecuperacaoExpiraEm;
    }

    public void setCodigoRecuperacaoExpiraEm(Instant codigoRecuperacaoExpiraEm) {
        this.codigoRecuperacaoExpiraEm = codigoRecuperacaoExpiraEm;
    }

    public Integer getCodigoRecuperacaoTentativas() {
        return codigoRecuperacaoTentativas;
    }

    public void setCodigoRecuperacaoTentativas(Integer codigoRecuperacaoTentativas) {
        this.codigoRecuperacaoTentativas = codigoRecuperacaoTentativas;
    }
}
