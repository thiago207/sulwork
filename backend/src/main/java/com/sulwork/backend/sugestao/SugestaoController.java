package com.sulwork.backend.sugestao;

import com.sulwork.backend.sugestao.dto.SugestoesResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sugestões")
@RestController
@RequiredArgsConstructor
public class SugestaoController {

    private final SugestaoService service;

    @Operation(summary = "O que eu posso levar", description = """
            Itens com quantidade pendente: obrigatórios primeiro, depois menor cobertura, maior faltante e nome.
            Itens que conflitam com todos os participantes ativos que têm restrição ficam de fora (contados em
            ocultosPorConflito); os que conflitam com alguns aparecem com o campo conflitos preenchido.""")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Evento ou participante não encontrado")
    @ApiResponse(responseCode = "422", description = "Evento não aceita contribuições ou participante não está ativo no evento")
    @GetMapping("/api/eventos/{eventoId}/participantes/{participanteId}/sugestoes")
    public SugestoesResposta sugestoes(@PathVariable Long eventoId, @PathVariable Long participanteId) {
        return service.sugerir(eventoId, participanteId);
    }
}
