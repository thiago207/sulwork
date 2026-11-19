package com.sulwork.backend.evento;

import java.time.LocalDateTime;

public final class EventosDeTeste {

    public static final LocalDateTime AGORA = LocalDateTime.of(2026, 12, 1, 10, 0);

    private EventosDeTeste() {
    }

    // leva um evento novo até o status pedido por um caminho válido
    public static Evento eventoEm(StatusEvento status) {
        var evento = new Evento("Café", null, TipoEvento.CAFE_DA_MANHA, AGORA.plusDays(10), AGORA.plusDays(8), AGORA);
        var caminho = switch (status) {
            case RASCUNHO -> new StatusEvento[] {};
            case ABERTO -> new StatusEvento[] {StatusEvento.ABERTO};
            case ENCERRADO -> new StatusEvento[] {StatusEvento.ABERTO, StatusEvento.ENCERRADO};
            case REALIZADO -> new StatusEvento[] {StatusEvento.ABERTO, StatusEvento.ENCERRADO, StatusEvento.REALIZADO};
            case CANCELADO -> new StatusEvento[] {StatusEvento.CANCELADO};
        };
        for (var passo : caminho) {
            evento.mudarStatus(passo, AGORA);
        }
        return evento;
    }
}
