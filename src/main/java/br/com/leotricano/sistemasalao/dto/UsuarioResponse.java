package br.com.leotricano.sistemasalao.dto;

import br.com.leotricano.sistemasalao.model.Perfil;
import br.com.leotricano.sistemasalao.model.Usuario;

/** JSON de um usuário (#18). Nunca leva a senha nem o hash dela. */
public record UsuarioResponse(Long id, String nome, String login, Perfil perfil,
                              Long profissionalId, String nomeProfissional) {

    public static UsuarioResponse from(Usuario usuario) {
        var profissional = usuario.getProfissional();
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getLogin(), usuario.getPerfil(),
                profissional == null ? null : profissional.getId(),
                profissional == null ? null : profissional.getNome());
    }
}
