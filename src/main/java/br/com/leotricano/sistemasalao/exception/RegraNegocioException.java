package br.com.leotricano.sistemasalao.exception;

/**
 * Lançada quando a requisição está bem formada, mas fere uma regra do salão. Exemplos: o
 * profissional não faz aquele serviço, agenda criada sem cliente, pagamento com valor zero.
 * O {@link TratadorDeErros} transforma em HTTP 400 (Bad Request).
 */
public class RegraNegocioException extends RuntimeException {

    /** @param mensagem explicação para o usuário, ex.: "William não faz o serviço Manicure." */
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
