package com.sulwork.backend.evento.dto;

import com.sulwork.backend.evento.StatusEvento;
import com.sulwork.backend.evento.TipoEvento;
import java.time.LocalDateTime;
import java.util.Set;

public record EventoResposta(
        Long id,
        String nome,
        String descricao,
        TipoEvento tipo,
        LocalDateTime dataHora,
        LocalDateTime prazoAlteracoes,
        StatusEvento status,
        Set<StatusEvento> proximosStatus) {
}
