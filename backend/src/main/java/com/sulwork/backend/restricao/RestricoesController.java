package com.sulwork.backend.restricao;

import com.sulwork.backend.restricao.dto.RestricoesResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Restrições")
@RestController
@RequiredArgsConstructor
public class RestricoesController {

    private final RestricoesService service;

    @Operation(summary = "Restrições alimentares do evento", description = """
            Resumo das restrições dos participantes ativos e, por item, os alérgenos
            e quantos participantes ativos têm restrição em conflito com eles.""")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    @GetMapping("/api/eventos/{eventoId}/restricoes")
    public RestricoesResposta restricoes(@PathVariable Long eventoId) {
        return service.resumir(eventoId);
    }
}
