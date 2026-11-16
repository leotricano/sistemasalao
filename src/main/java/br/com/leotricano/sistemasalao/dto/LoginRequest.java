package br.com.leotricano.sistemasalao.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo de {@code POST /auth/login} (#18): {@code {"login":"william","senha":"..."}}. */
public record LoginRequest(@NotBlank String login, @NotBlank String senha) {
}
