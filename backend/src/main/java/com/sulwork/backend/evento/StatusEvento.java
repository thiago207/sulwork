package com.sulwork.backend.evento;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum StatusEvento {
    RASCUNHO,
    ABERTO,
    ENCERRADO,
    REALIZADO,
    CANCELADO;

    // reabrir (ENCERRADO -> ABERTO) ainda depende do prazo, checado em Evento.mudarStatus
    private static final Map<StatusEvento, Set<StatusEvento>> TRANSICOES = Map.of(
            RASCUNHO, EnumSet.of(ABERTO, CANCELADO),
            ABERTO, EnumSet.of(ENCERRADO, CANCELADO),
            ENCERRADO, EnumSet.of(REALIZADO, ABERTO, CANCELADO),
            REALIZADO, EnumSet.noneOf(StatusEvento.class),
            CANCELADO, EnumSet.noneOf(StatusEvento.class));

    public boolean podeIrPara(StatusEvento destino) {
        return TRANSICOES.get(this).contains(destino);
    }

    public Set<StatusEvento> destinosPermitidos() {
        return EnumSet.copyOf(TRANSICOES.get(this));
    }
}
