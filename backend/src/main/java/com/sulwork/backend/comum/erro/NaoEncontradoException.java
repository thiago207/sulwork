package com.sulwork.backend.comum.erro;

import org.springframework.http.HttpStatus;

public class NaoEncontradoException extends ExcecaoApi {

    public NaoEncontradoException(String mensagem) {
        super(HttpStatus.NOT_FOUND, mensagem);
    }
}
