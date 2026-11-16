package br.com.leotricano.sistemasalao.dto;

import java.time.Instant;

/**
 * Resposta do login (#18). O frontend guarda o {@code token} e o envia em toda requisição, no
 * cabeçalho {@code Authorization: Bearer <token>}. Depois de {@code expiraEm}, é preciso entrar de novo.
 */
public record LoginResponse(String token, Instant expiraEm, UsuarioResponse usuario) {
}
