package com.sulwork.backend.comum.erro;

/** Violação de regra de negócio. Vira HTTP 422. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
