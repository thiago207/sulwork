package com.sulwork.backend.item;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Alergeno {
    GLUTEN("glúten"),
    LACTOSE("lactose"),
    OVO("ovo"),
    CARNE("carne"),
    PEIXE("peixe"),
    FRUTOS_DO_MAR("frutos do mar"),
    MEL("mel"),
    AMENDOIM("amendoim");

    // texto usado nas mensagens para o usuário
    private final String rotulo;
}
