package br.com.leotricano.sistemasalao.exception;

import br.com.leotricano.sistemasalao.dto.ErroResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Ponto único que transforma exceções em respostas HTTP com uma mensagem legível (#13).
 *
 * <p>{@code @RestControllerAdvice} faz esta classe "vigiar" todos os controllers: quando um deles
 * (ou um service chamado por ele) lança uma exceção, o Spring procura aqui o método
 * {@code @ExceptionHandler} daquele tipo e usa o retorno dele como resposta. Assim os services só
 * fazem {@code throw}, e nenhum controller precisa de {@code if} ou try/catch para erros.
 *
 * <p>Toda resposta de erro da API tem o mesmo formato, {@link ErroResponse}:
 * {@code {"status": 404, "mensagem": "Agenda 99 não encontrada."}}. O frontend só precisa mostrar
 * a {@code mensagem}.
 *
 * <table>
 *   <caption>Mapa de erros</caption>
 *   <tr><th>Exceção</th><th>HTTP</th><th>Quando</th></tr>
 *   <tr><td>RecursoNaoEncontradoException</td><td>404</td><td>id que não existe</td></tr>
 *   <tr><td>RegraNegocioException</td><td>400</td><td>regra do salão violada</td></tr>
 *   <tr><td>MethodArgumentNotValidException</td><td>400</td><td>falhou um {@code @Valid} (@NotBlank, @Positive...)</td></tr>
 *   <tr><td>HttpMessageNotReadableException</td><td>400</td><td>JSON malformado ou tipo errado</td></tr>
 *   <tr><td>MethodArgumentTypeMismatchException</td><td>400</td><td>parâmetro de URL com valor inválido</td></tr>
 *   <tr><td>MissingServletRequestParameterException</td><td>400</td><td>parâmetro de URL obrigatório ausente</td></tr>
 *   <tr><td>ConflitoHorarioException</td><td>409</td><td>profissional já ocupado no horário</td></tr>
 *   <tr><td>DataIntegrityViolationException</td><td>409</td><td>apagar algo que está em uso</td></tr>
 *   <tr><td>qualquer outra</td><td>500</td><td>erro inesperado (detalhe só no log)</td></tr>
 * </table>
 */
@RestControllerAdvice
public class TratadorDeErros {

    /** Logger: escreve no console da aplicação. Usado para registrar os erros 500, que precisam ser investigados. */
    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    /** Id inexistente → 404. */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(RecursoNaoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Regra do salão violada → 400. */
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> tratarRegraNegocio(RegraNegocioException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Conflito de horário (#12) → 409. */
    @ExceptionHandler(ConflitoHorarioException.class)
    public ResponseEntity<ErroResponse> tratarConflitoHorario(ConflitoHorarioException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * Falha nas anotações de validação da entidade, checadas pelo {@code @Valid} do controller → 400.
     * Junta todos os campos com problema numa mensagem só.
     * Ex.: "nome: não deve estar em branco; duracaoMinutos: deve ser maior que 0".
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .sorted() // ordem fixa, para a mesma requisição sempre dar a mesma mensagem
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, mensagem);
    }

    /**
     * O corpo não pôde ser convertido de JSON para objeto → 400. Acontece com JSON malformado,
     * texto num campo numérico ({@code "duracaoMinutos":"abc"}) ou valor fora de um enum ({@code "BOLETO"}).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoInvalido(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido: confira o formato do JSON e o tipo de cada campo.");
    }

    /** Parâmetro da URL com valor que não serve para o tipo esperado (ex.: {@code formaPagamento=BOLETO}, {@code /agendas/abc}) → 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarParametroInvalido(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro '" + e.getName() + "': " + e.getValue() + ".");
    }

    /** Faltou um {@code @RequestParam} obrigatório (ex.: chamar /itens sem {@code servicoId}) → 400. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> tratarParametroAusente(MissingServletRequestParameterException e) {
        return responder(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: " + e.getParameterName() + ".");
    }

    /**
     * O banco recusou a operação por causa de uma chave estrangeira → 409. O caso típico é apagar
     * um cliente que tem agendamentos, ou um serviço que já foi usado em algum.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarIntegridade(DataIntegrityViolationException e) {
        return responder(HttpStatus.CONFLICT,
                "Não é possível concluir: o registro está ligado a outros dados (por exemplo, agendamentos ou serviços vinculados).");
    }

    /**
     * Qualquer outra exceção.
     * <ul>
     *   <li>Erros do próprio Spring que já sabem seu status (URL inexistente → 404, método HTTP
     *       errado → 405...) implementam {@code org.springframework.web.ErrorResponse}; o status
     *       deles é mantido.</li>
     *   <li>O resto é um erro inesperado → 500 com mensagem genérica. O detalhe vai só para o log:
     *       mandar o stack trace para quem chamou a API exporia o funcionamento interno.</li>
     * </ul>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarInesperado(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse erroSpring) {
            HttpStatusCode status = erroSpring.getStatusCode();
            return ResponseEntity.status(status)
                    .body(new ErroResponse(status.value(), "Requisição não atendida (HTTP " + status.value() + ")."));
        }
        log.error("Erro inesperado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado no servidor.");
    }

    /** Monta a resposta no formato padrão {@link ErroResponse}. */
    private ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroResponse(status.value(), mensagem));
    }
}
