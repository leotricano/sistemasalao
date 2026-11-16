package br.com.leotricano.sistemasalao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code PUT /auth/senha} (#18): a pessoa logada troca a própria senha. */
public record TrocarSenhaRequest(@NotBlank String senhaAtual,
                                 @NotBlank @Size(min = 6, message = "precisa ter pelo menos 6 caracteres") String novaSenha) {
}
