package br.com.leotricano.sistemasalao.exception;

import br.com.leotricano.sistemasalao.dto.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Ponto único que transforma exceções em respostas HTTP com uma mensagem legível.
 *
 * <p>{@code @RestControllerAdvice} faz esta classe "vigiar" todos os controllers: quando um deles
 * (ou um service chamado por ele) lança uma exceção, o Spring procura aqui um método
 * {@code @ExceptionHandler} para aquele tipo e usa o retorno dele como resposta. Assim nenhum
 * controller precisa de try/catch.
 *
 * <p>Hoje só trata o conflito de horário (#12).
 * TODO(#13): tratar também "não encontrado" (404) e erros de validação (400), e tirar os
 * {@code return null} dos services.
 */
@RestControllerAdvice
public class TratadorDeErros {

    /** Conflito de horário → 409 Conflict, com a mensagem da exceção no corpo. */
    @ExceptionHandler(ConflitoHorarioException.class)
    public ResponseEntity<ErroResponse> tratarConflitoHorario(ConflitoHorarioException e) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(new ErroResponse(status.value(), e.getMessage()));
    }
}
