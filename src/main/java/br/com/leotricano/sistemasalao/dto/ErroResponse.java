package br.com.leotricano.sistemasalao.dto;

/**
 * JSON devolvido quando uma regra de negócio recusa a requisição. Exemplo:
 * {@code {"status":409,"mensagem":"William já tem atendimento em 03/11/2026 das 14:00 às 17:00..."}}.
 * Montado pelo {@code TratadorDeErros}.
 *
 * @param status   o código HTTP, repetido no corpo para facilitar a leitura no frontend
 * @param mensagem explicação para mostrar ao usuário
 */
public record ErroResponse(int status, String mensagem) {
}
