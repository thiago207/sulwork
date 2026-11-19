package com.sulwork.backend.comum.erro;

import org.springframework.http.HttpStatus;

public class ConflitoException extends ExcecaoApi {

    public ConflitoException(String mensagem) {
        super(HttpStatus.CONFLICT, mensagem);
    }
}
