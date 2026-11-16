package br.com.leotricano.sistemasalao.config;

import br.com.leotricano.sistemasalao.model.Perfil;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Quem está fazendo a requisição, lido do token JWT (#18). Os controllers recebem o token com
 * {@code @AuthenticationPrincipal Jwt jwt} e chamam {@link #de(Jwt)}.
 *
 * @param profissionalId para o perfil PROFISSIONAL, qual profissional do salão é esta pessoa
 */
public record UsuarioLogado(Long usuarioId, String login, Perfil perfil, Long profissionalId) {

    public static UsuarioLogado de(Jwt jwt) {
        return new UsuarioLogado(numero(jwt.getClaim("uid")), jwt.getSubject(),
                Perfil.valueOf(jwt.getClaimAsString("perfil")), numero(jwt.getClaim("profissionalId")));
    }

    public boolean ehProfissional() {
        return perfil == Perfil.PROFISSIONAL;
    }

    /** No token, números podem voltar como Integer ou Long; Number cobre os dois. */
    private static Long numero(Object valor) {
        return valor instanceof Number n ? n.longValue() : null;
    }
}
