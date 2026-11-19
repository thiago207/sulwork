package com.sulwork.backend.comum.erro;

import org.springframework.http.HttpStatus;

public class RegraNegocioException extends ExcecaoApi {

    public RegraNegocioException(String mensagem) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, mensagem);
    }
}
