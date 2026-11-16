package br.com.leotricano.sistemasalao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de {@code PUT /usuarios/{id}/senha} (#46): o administrador cria uma senha nova para alguém. */
public record NovaSenhaRequest(@NotBlank @Size(min = 6, message = "precisa ter pelo menos 6 caracteres") String novaSenha) {
}
