package br.com.leotricano.sistemasalao.dto;

import br.com.leotricano.sistemasalao.model.Perfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code POST /usuarios} (#18), usado pelo dono para criar o acesso de alguém.
 * {@code profissionalId} é obrigatório só para o perfil PROFISSIONAL. O {@code email} é opcional e
 * serve para a própria pessoa recuperar a senha (#42).
 */
public record NovoUsuarioRequest(@NotBlank String nome,
                                 @NotBlank String login,
                                 String email, // formato conferido no UsuarioService, depois de tirar os espaços
                                 @NotBlank @Size(min = 6, message = "precisa ter pelo menos 6 caracteres") String senha,
                                 @NotNull Perfil perfil,
                                 Long profissionalId) {
}
