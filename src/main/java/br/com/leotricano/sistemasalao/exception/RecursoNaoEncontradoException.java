package br.com.leotricano.sistemasalao.exception;

/**
 * Lançada quando algo pedido pelo id não existe: cliente, serviço, profissional, agenda, item ou
 * pagamento. O {@link TratadorDeErros} transforma em HTTP 404 (Not Found).
 *
 * <p>Substitui o antigo padrão de os services devolverem {@code null} (#13).
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    /** @param mensagem ex.: "Cliente 99 não encontrado." */
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
