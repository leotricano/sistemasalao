package br.com.leotricano.sistemasalao.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo de {@code POST /auth/esqueci-senha} (#42): login ou e-mail de quem esqueceu a senha. */
public record EsqueciSenhaRequest(@NotBlank String loginOuEmail) {
}
