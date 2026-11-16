package br.com.leotricano.sistemasalao.dto;

/**
 * Corpo de {@code PUT /auth/me} (#42): a pessoa logada cadastra ou troca o próprio e-mail. Vazio apaga.
 * O formato é conferido no UsuarioService depois de tirar os espaços, para "ana@x.com " ser aceito.
 */
public record AtualizarContaRequest(String email) {
}
