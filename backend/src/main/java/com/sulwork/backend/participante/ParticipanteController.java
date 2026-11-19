package com.sulwork.backend.participante;

import com.sulwork.backend.participante.dto.ParticipanteRequisicao;
import com.sulwork.backend.participante.dto.ParticipanteResposta;
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

@Tag(name = "Participantes")
@RestController
@RequestMapping("/api/participantes")
@RequiredArgsConstructor
public class ParticipanteController {

    private final ParticipanteService service;

    @Operation(summary = "Cadastrar participante", description = "CPF com ou sem máscara; é salvo só com números.")
    @ApiResponse(responseCode = "201", description = "Criado")
    @ApiResponse(responseCode = "400", description = "Campos inválidos ou CPF inválido")
    @ApiResponse(responseCode = "409", description = "CPF já cadastrado")
    @PostMapping
    public ResponseEntity<ParticipanteResposta> criar(@Valid @RequestBody ParticipanteRequisicao req) {
        var criado = service.criar(req);
        return ResponseEntity.created(URI.create("/api/participantes/" + criado.id())).body(criado);
    }

    @Operation(summary = "Listar participantes ativos")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping
    public List<ParticipanteResposta> listar() {
        return service.listar();
    }

    @Operation(summary = "Buscar participante")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Participante não encontrado ou inativo")
    @GetMapping("/{id}")
    public ParticipanteResposta buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "Editar participante")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Campos inválidos ou CPF inválido")
    @ApiResponse(responseCode = "404", description = "Participante não encontrado ou inativo")
    @ApiResponse(responseCode = "409", description = "CPF já usado por outro participante")
    @PutMapping("/{id}")
    public ParticipanteResposta editar(@PathVariable Long id, @Valid @RequestBody ParticipanteRequisicao req) {
        return service.editar(id, req);
    }

    @Operation(summary = "Excluir participante", description = "Exclusão lógica: o participante fica inativo.")
    @ApiResponse(responseCode = "204", description = "Excluído")
    @ApiResponse(responseCode = "404", description = "Participante não encontrado ou inativo")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
