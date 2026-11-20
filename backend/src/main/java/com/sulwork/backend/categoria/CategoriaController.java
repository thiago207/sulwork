package com.sulwork.backend.categoria;

import com.sulwork.backend.categoria.dto.CategoriaRequisicao;
import com.sulwork.backend.categoria.dto.CategoriaResposta;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Categorias")
@RestController
@RequestMapping("/api/eventos/{eventoId}/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService service;

    @Operation(summary = "Listar categorias do evento")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    @GetMapping
    public List<CategoriaResposta> listar(@PathVariable Long eventoId) {
        return service.listar(eventoId);
    }

    @Operation(summary = "Buscar categoria")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Categoria não encontrada neste evento")
    @GetMapping("/{id}")
    public CategoriaResposta buscar(@PathVariable Long eventoId, @PathVariable Long id) {
        return service.buscar(eventoId, id);
    }

    @Operation(summary = "Criar categoria", description = "Só com o evento em RASCUNHO ou ABERTO.")
    @ApiResponse(responseCode = "201", description = "Criada")
    @ApiResponse(responseCode = "400", description = "Campos inválidos")
    @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    @ApiResponse(responseCode = "409", description = "Já existe categoria com esse nome no evento")
    @ApiResponse(responseCode = "422", description = "Status do evento não permite edição")
    @PostMapping
    public ResponseEntity<CategoriaResposta> criar(@PathVariable Long eventoId, @Valid @RequestBody CategoriaRequisicao req) {
        var criada = service.criar(eventoId, req);
        return ResponseEntity.created(URI.create("/api/eventos/" + eventoId + "/categorias/" + criada.id())).body(criada);
    }

    @Operation(summary = "Editar categoria", description = "Só com o evento em RASCUNHO ou ABERTO.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Campos inválidos")
    @ApiResponse(responseCode = "404", description = "Evento ou categoria não encontrados")
    @ApiResponse(responseCode = "409", description = "Já existe categoria com esse nome no evento")
    @ApiResponse(responseCode = "422", description = "Status não permite edição ou a categoria tem contribuições e a unidade mudou")
    @PutMapping("/{id}")
    public CategoriaResposta editar(@PathVariable Long eventoId, @PathVariable Long id,
                                    @Valid @RequestBody CategoriaRequisicao req) {
        return service.editar(eventoId, id, req);
    }

    @Operation(summary = "Excluir categoria", description = "Só com o evento em RASCUNHO ou ABERTO e sem itens.")
    @ApiResponse(responseCode = "204", description = "Excluída")
    @ApiResponse(responseCode = "404", description = "Evento ou categoria não encontrados")
    @ApiResponse(responseCode = "422", description = "Status do evento não permite edição ou categoria tem itens")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long eventoId, @PathVariable Long id) {
        service.excluir(eventoId, id);
    }
}
