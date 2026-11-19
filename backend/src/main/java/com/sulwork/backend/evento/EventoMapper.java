package com.sulwork.backend.evento;

import com.sulwork.backend.evento.dto.EventoResposta;

public final class EventoMapper {

    private EventoMapper() {
    }

    public static EventoResposta paraResposta(Evento e) {
        return new EventoResposta(e.getId(), e.getNome(), e.getDescricao(), e.getTipo(),
                e.getDataHora(), e.getPrazoAlteracoes(), e.getStatus(), e.getStatus().destinosPermitidos());
    }
}
