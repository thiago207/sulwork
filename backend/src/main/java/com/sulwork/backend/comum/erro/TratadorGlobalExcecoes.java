package com.sulwork.backend.comum.erro;

import com.sulwork.backend.comum.config.RelogioConfig;
import com.sulwork.backend.comum.erro.ErroResposta.ErroCampo;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestControllerAdvice
public class TratadorGlobalExcecoes {

    @ExceptionHandler(ExcecaoApi.class)
    public ResponseEntity<ErroResposta> excecaoApi(ExcecaoApi ex) {
        return responder(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException ex) {
        List<ErroCampo> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErroCampo(e.getField(), e.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoIlegivel(HttpMessageNotReadableException ex) {
        log.debug("Corpo da requisição ilegível", ex);
        return responder(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> tipoInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> integridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade no banco", ex);
        return responder(HttpStatus.CONFLICT, "Operação conflita com dados já existentes.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> inesperado(Exception ex) {
        // exceções do próprio Spring (rota inexistente, método não suportado...) já trazem o status
        if (ex instanceof ErrorResponse spring) {
            return responder(spring.getStatusCode(), titulo(spring.getStatusCode()) + ".");
        }
        log.error("Erro inesperado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado.");
    }

    private ResponseEntity<ErroResposta> responder(HttpStatusCode status, String mensagem) {
        return responder(status, mensagem, List.of());
    }

    private ResponseEntity<ErroResposta> responder(HttpStatusCode status, String mensagem, List<ErroCampo> campos) {
        var corpo = new ErroResposta(OffsetDateTime.now(RelogioConfig.FUSO), status.value(), titulo(status), mensagem, campos);
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
}
