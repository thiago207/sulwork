package com.sulwork.backend.comum.erro;

import com.sulwork.backend.comum.erro.ErroResposta.ErroCampo;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class TratadorGlobalExcecoes {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalExcecoes.class);
    private static final ZoneId FUSO = ZoneId.of("America/Recife");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException ex) {
        List<ErroCampo> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErroCampo(e.getField(), e.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoIlegivel(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado.", List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> tipoInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro '" + ex.getName() + "'.", List.of());
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<ErroResposta> naoEncontrado(NaoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResposta> conflito(ConflitoException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> integridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade no banco", ex);
        return responder(HttpStatus.CONFLICT, "Operação conflita com dados já existentes.", List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResposta> regraNegocio(RegraNegocioException ex) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> inesperado(Exception ex) {
        // Exceções do próprio Spring (rota inexistente, método não suportado...) já sabem seu status.
        if (ex instanceof ErrorResponse respostaSpring) {
            HttpStatusCode status = respostaSpring.getStatusCode();
            return responder(status, mensagemPadrao(status), List.of());
        }
        log.error("Erro inesperado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado.", List.of());
    }

    private ResponseEntity<ErroResposta> responder(HttpStatusCode status, String mensagem, List<ErroCampo> campos) {
        ErroResposta corpo = new ErroResposta(
                OffsetDateTime.now(FUSO), status.value(), titulo(status), mensagem, campos);
        return ResponseEntity.status(status).body(corpo);
    }

    private static String titulo(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Requisição inválida";
            case 404 -> "Não encontrado";
            case 405 -> "Método não permitido";
            case 409 -> "Conflito";
            case 415 -> "Tipo de mídia não suportado";
            case 422 -> "Regra de negócio violada";
            case 500 -> "Erro interno";
            default -> "Erro";
        };
    }

    private static String mensagemPadrao(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "Recurso não encontrado.";
            case 405 -> "Método HTTP não suportado para este recurso.";
            case 415 -> "Tipo de conteúdo não suportado.";
            default -> "Não foi possível processar a requisição.";
        };
    }
}
