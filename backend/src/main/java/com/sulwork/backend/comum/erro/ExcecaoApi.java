package com.sulwork.backend.comum.erro;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ExcecaoApi extends RuntimeException {

    private final HttpStatus status;

    protected ExcecaoApi(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }
}
