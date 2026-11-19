package com.sulwork.backend.contribuicao;

import com.sulwork.backend.contribuicao.dto.AlteracaoContribuicaoRequisicao;
import com.sulwork.backend.contribuicao.dto.ContribuicaoRequisicao;
import com.sulwork.backend.contribuicao.dto.ContribuicaoResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Contribuições")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContribuicaoController {

    private final ContribuicaoService service;

    @Operation(summary = "Listar contribuições ativas do evento")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    @GetMapping("/eventos/{eventoId}/contribuicoes")
    public List<ContribuicaoResposta> listarDoEvento(@PathVariable Long eventoId) {
        return service.listarDoEvento(eventoId);
    }

    @Operation(summary = "Listar contribuições ativas do participante", description = "Em todos os eventos.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Participante não encontrado")
    @GetMapping("/participantes/{participanteId}/contribuicoes")
    public List<ContribuicaoResposta> listarDoParticipante(@PathVariable Long participanteId) {
        return service.listarDoParticipante(participanteId);
    }

    @Operation(summary = "Incluir contribuição", description = """
            Só com o evento ABERTO, antes do prazo e com participação ativa. A soma não pode passar do necessário.
            O campo avisos indica alérgenos que conflitam com a restrição do participante (não bloqueia).""")
    @ApiResponse(responseCode = "201", description = "Criada")
    @ApiResponse(responseCode = "400", description = "Campos inválidos")
    @ApiResponse(responseCode = "404", description = "Evento ou item não encontrados")
    @ApiResponse(responseCode = "409", description = "Participante já contribui com este item")
    @ApiResponse(responseCode = "422", description = "Evento fechado, prazo vencido, sem participação ou quantidade acima do necessário")
    @PostMapping("/eventos/{eventoId}/contribuicoes")
    @ResponseStatus(HttpStatus.CREATED)
    public ContribuicaoResposta criar(@PathVariable Long eventoId, @Valid @RequestBody ContribuicaoRequisicao req) {
        return service.criar(eventoId, req);
    }

    @Operation(summary = "Alterar quantidade da contribuição")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Campos inválidos")
    @ApiResponse(responseCode = "404", description = "Evento ou contribuição não encontrados")
    @ApiResponse(responseCode = "422", description = "Evento fechado, prazo vencido ou quantidade acima do necessário")
    @PutMapping("/eventos/{eventoId}/contribuicoes/{id}")
    public ContribuicaoResposta alterar(@PathVariable Long eventoId, @PathVariable Long id,
                                        @Valid @RequestBody AlteracaoContribuicaoRequisicao req) {
        return service.alterar(eventoId, id, req.quantidade());
    }

    @Operation(summary = "Remover contribuição", description = "Exclusão lógica.")
    @ApiResponse(responseCode = "204", description = "Removida")
    @ApiResponse(responseCode = "404", description = "Evento ou contribuição não encontrados")
    @ApiResponse(responseCode = "422", description = "Evento fechado ou prazo vencido")
    @DeleteMapping("/eventos/{eventoId}/contribuicoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long eventoId, @PathVariable Long id) {
        service.remover(eventoId, id);
    }
}
