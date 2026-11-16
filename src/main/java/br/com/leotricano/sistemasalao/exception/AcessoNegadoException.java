package br.com.leotricano.sistemasalao.exception;

/** Ação proibida por quem ou de onde foi pedida (#46). O {@link TratadorDeErros} transforma em HTTP 403 (Forbidden). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
