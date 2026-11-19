package com.sulwork.backend.item;

import com.sulwork.backend.item.dto.ItemRequisicao;
import com.sulwork.backend.item.dto.ItemResposta;
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

@Tag(name = "Itens")
@RestController
@RequestMapping("/api/eventos/{eventoId}/itens")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService service;

    @Operation(summary = "Listar itens do evento", description = "Ordenados por categoria e nome. A unidade vem da categoria.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    @GetMapping
    public List<ItemResposta> listar(@PathVariable Long eventoId) {
        return service.listar(eventoId);
    }

    @Operation(summary = "Buscar item")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Item não encontrado neste evento")
    @GetMapping("/{id}")
    public ItemResposta buscar(@PathVariable Long eventoId, @PathVariable Long id) {
        return service.buscar(eventoId, id);
    }

    @Operation(summary = "Criar item", description = "Só com o evento em RASCUNHO ou ABERTO. A categoria deve ser do mesmo evento.")
    @ApiResponse(responseCode = "201", description = "Criado")
    @ApiResponse(responseCode = "400", description = "Campos inválidos")
    @ApiResponse(responseCode = "404", description = "Evento ou categoria não encontrados")
    @ApiResponse(responseCode = "422", description = "Status do evento não permite edição")
    @PostMapping
    public ResponseEntity<ItemResposta> criar(@PathVariable Long eventoId, @Valid @RequestBody ItemRequisicao req) {
        var criado = service.criar(eventoId, req);
        return ResponseEntity.created(URI.create("/api/eventos/" + eventoId + "/itens/" + criado.id())).body(criado);
    }

    @Operation(summary = "Editar item",
            description = "Só com o evento em RASCUNHO ou ABERTO. A quantidade necessária não pode ficar abaixo do já contribuído.")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Campos inválidos")
    @ApiResponse(responseCode = "404", description = "Evento, item ou categoria não encontrados")
    @ApiResponse(responseCode = "422", description = "Status do evento não permite edição ou quantidade abaixo do contribuído")
    @PutMapping("/{id}")
    public ItemResposta editar(@PathVariable Long eventoId, @PathVariable Long id, @Valid @RequestBody ItemRequisicao req) {
        return service.editar(eventoId, id, req);
    }

    @Operation(summary = "Excluir item", description = "Só com o evento em RASCUNHO ou ABERTO e sem contribuições.")
    @ApiResponse(responseCode = "204", description = "Excluído")
    @ApiResponse(responseCode = "404", description = "Evento ou item não encontrados")
    @ApiResponse(responseCode = "422", description = "Status do evento não permite edição ou item tem contribuições")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long eventoId, @PathVariable Long id) {
        service.excluir(eventoId, id);
    }
}
