package com.sulwork.backend.comum.erro;

import java.time.OffsetDateTime;
import java.util.List;

public record ErroResposta(OffsetDateTime timestamp, int status, String erro, String mensagem, List<ErroCampo> campos) {

    public record ErroCampo(String campo, String mensagem) {
    }
}
