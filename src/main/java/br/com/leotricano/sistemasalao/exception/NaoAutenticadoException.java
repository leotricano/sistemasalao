package br.com.leotricano.sistemasalao.exception;

/** Login ou senha errados (#18). O {@link TratadorDeErros} transforma em HTTP 401 (Unauthorized). */
public class NaoAutenticadoException extends RuntimeException {

    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
