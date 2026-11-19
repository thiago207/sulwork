package com.sulwork.backend.participacao;

import com.sulwork.backend.participacao.dto.ParticipacaoResposta;
import com.sulwork.backend.participante.dto.ParticipanteResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Participações")
@RestController
@RequestMapping("/api/eventos/{eventoId}/participantes")
@RequiredArgsConstructor
public class ParticipacaoController {

    private final ParticipacaoService service;

    @Operation(summary = "Listar participantes ativos do evento")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    @GetMapping
    public List<ParticipanteResposta> listar(@PathVariable Long eventoId) {
        return service.listarParticipantesAtivos(eventoId);
    }

    @Operation(summary = "Entrar no evento",
            description = "Só com o evento ABERTO e antes do prazo. Quem já saiu tem a participação antiga reativada.")
    @ApiResponse(responseCode = "201", description = "Participação criada ou reativada")
    @ApiResponse(responseCode = "404", description = "Evento ou participante não encontrado")
    @ApiResponse(responseCode = "409", description = "Participante já está no evento")
    @ApiResponse(responseCode = "422", description = "Evento não está ABERTO ou prazo vencido")
    @PostMapping("/{participanteId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipacaoResposta entrar(@PathVariable Long eventoId, @PathVariable Long participanteId) {
        return service.entrar(eventoId, participanteId);
    }

    @Operation(summary = "Sair do evento", description = "Só com o evento ABERTO e antes do prazo. A participação fica inativa.")
    @ApiResponse(responseCode = "204", description = "Saiu do evento")
    @ApiResponse(responseCode = "404", description = "Evento não encontrado ou participante não está no evento")
    @ApiResponse(responseCode = "422", description = "Evento não está ABERTO ou prazo vencido")
    @DeleteMapping("/{participanteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sair(@PathVariable Long eventoId, @PathVariable Long participanteId) {
        service.sair(eventoId, participanteId);
    }
}
