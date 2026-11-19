package com.sulwork.backend.comum.erro;

/** Conflito com um registro existente (duplicidade). Vira HTTP 409. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
