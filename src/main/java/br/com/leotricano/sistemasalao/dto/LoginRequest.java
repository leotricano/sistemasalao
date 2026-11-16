package br.com.leotricano.sistemasalao.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de {@code POST /auth/login} (#18): {@code {"login":"william","senha":"...","manterConectado":true}}.
 * {@code manterConectado} (#43) é opcional: {@code true} faz o login valer 30 dias neste aparelho.
 */
public record LoginRequest(@NotBlank String login, @NotBlank String senha, Boolean manterConectado) {
}
