package com.sulwork.backend.cobertura;

import com.sulwork.backend.cobertura.dto.CoberturaResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cobertura")
@RestController
@RequiredArgsConstructor
public class CoberturaController {

    private final CoberturaService service;

    @Operation(summary = "Cobertura do evento", description = """
            Geral: média ponderada das coberturas dos itens (obrigatório peso 2, opcional peso 1).
            Item: min(contribuído, necessário) / necessário. Categoria: contribuído / meta, limitado a 100%.
            Percentuais de 0 a 100. Só contam contribuições e participações ativas.""")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    @GetMapping("/api/eventos/{eventoId}/cobertura")
    public CoberturaResposta cobertura(@PathVariable Long eventoId) {
        return service.calcular(eventoId);
    }
}
