package br.com.leotricano.sistemasalao.repository;

import br.com.leotricano.sistemasalao.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Acesso à tabela {@code usuario} (#18). */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Usado no login: acha o usuário pelo nome de usuário digitado. */
    Optional<Usuario> findByLogin(String login);

    /** Usado ao cadastrar: impede dois usuários com o mesmo login. */
    boolean existsByLogin(String login);

    /** "Esqueci minha senha" (#42): a pessoa pode informar o e-mail em vez do login. */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    /** Impede dois usuários com o mesmo e-mail (#42). */
    boolean existsByEmailIgnoreCase(String email);
}
