package br.com.leotricano.sistemasalao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /auth/primeiro-acesso} (#46): o dono cria a própria conta quando o sistema ainda não tem usuários. */
public record PrimeiroAcessoRequest(@NotBlank String nome,
                                    @NotBlank String login,
                                    @NotBlank @Size(min = 6, message = "precisa ter pelo menos 6 caracteres") String senha) {
}
