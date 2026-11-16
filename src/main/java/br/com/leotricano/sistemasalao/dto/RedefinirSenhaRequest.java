package br.com.leotricano.sistemasalao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code POST /auth/redefinir-senha} (#42): o código recebido por e-mail e a senha nova. */
public record RedefinirSenhaRequest(@NotBlank String loginOuEmail,
                                    @NotBlank String codigo,
                                    @NotBlank @Size(min = 6, message = "precisa ter pelo menos 6 caracteres") String novaSenha) {
}
