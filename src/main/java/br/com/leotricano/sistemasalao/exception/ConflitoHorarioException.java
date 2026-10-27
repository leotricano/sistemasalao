package br.com.leotricano.sistemasalao.exception;

/**
 * Lançada quando um agendamento faria um profissional ter dois atendimentos ao mesmo tempo (#12).
 *
 * <p>Estende {@code RuntimeException} ("exceção não verificada"): quem chama não é obrigado a
 * tratar com try/catch. Ela sobe do service até o {@link TratadorDeErros}, que a transforma numa
 * resposta HTTP 409 (Conflict) com esta mensagem no corpo.
 */
public class ConflitoHorarioException extends RuntimeException {

    /** @param mensagem texto para quem usa a API, dizendo com quem e em que horário está o conflito */
    public ConflitoHorarioException(String mensagem) {
        super(mensagem);
    }
}
