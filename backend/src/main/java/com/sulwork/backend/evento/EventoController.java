package com.sulwork.backend.evento;

import com.sulwork.backend.evento.dto.EventoRequisicao;
import com.sulwork.backend.evento.dto.EventoResposta;
import com.sulwork.backend.evento.dto.MudancaStatusRequisicao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Eventos")
@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService service;

    @Operation(summary = "Criar evento", description = "Cria em RASCUNHO. Datas no futuro e prazo antes da data do evento.")
    @ApiResponse(responseCode = "201", description = "Criado")
    @ApiResponse(responseCode = "400", description = "Campos inválidos")
    @ApiResponse(responseCode = "422", description = "Datas inválidas")
    @PostMapping
    public ResponseEntity<EventoResposta> criar(@Valid @RequestBody EventoRequisicao req) {
        var criado = service.criar(req);
        return ResponseEntity.created(URI.create("/api/eventos/" + criado.id())).body(criado);
    }

    @Operation(summary = "Listar eventos", description = "Ordenados pela data. Filtro opcional por status.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Status inválido")
    @GetMapping
    public List<EventoResposta> listar(@RequestParam(required = false) StatusEvento status) {
        return service.listar(status);
    }

    @Operation(summary = "Buscar evento")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    @GetMapping("/{id}")
    public EventoResposta buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "Editar evento",
            description = "Só em RASCUNHO ou ABERTO. Data alterada deve estar no futuro; prazo sempre antes da data.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Campos inválidos")
    @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    @ApiResponse(responseCode = "422", description = "Status não permite edição ou datas inválidas")
    @PutMapping("/{id}")
    public EventoResposta editar(@PathVariable Long id, @Valid @RequestBody EventoRequisicao req) {
        return service.editar(id, req);
    }

    @Operation(summary = "Excluir evento", description = "Só em RASCUNHO.")
    @ApiResponse(responseCode = "204", description = "Excluído")
    @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    @ApiResponse(responseCode = "422", description = "Evento fora de RASCUNHO")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    @Operation(summary = "Mudar status", description = """
            RASCUNHO -> ABERTO -> ENCERRADO -> REALIZADO. ENCERRADO -> ABERTO só antes do prazo.
            CANCELADO a partir de RASCUNHO, ABERTO ou ENCERRADO. REALIZADO e CANCELADO são finais.""")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Status ausente ou inválido")
    @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    @ApiResponse(responseCode = "422", description = "Transição não permitida")
    @PatchMapping("/{id}/status")
    public EventoResposta mudarStatus(@PathVariable Long id, @Valid @RequestBody MudancaStatusRequisicao req) {
        return service.mudarStatus(id, req.status());
    }
}
