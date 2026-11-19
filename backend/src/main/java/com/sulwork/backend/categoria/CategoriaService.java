package com.sulwork.backend.categoria;

import com.sulwork.backend.categoria.dto.CategoriaRequisicao;
import com.sulwork.backend.categoria.dto.CategoriaResposta;
import com.sulwork.backend.comum.erro.NaoEncontradoException;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import com.sulwork.backend.evento.EventoService;
import com.sulwork.backend.item.ItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repository;
    private final ItemRepository itemRepository;
    private final EventoService eventoService;

    @Transactional(readOnly = true)
    public List<CategoriaResposta> listar(Long eventoId) {
        eventoService.buscarEntidade(eventoId);
        return repository.findByEventoIdOrderByNome(eventoId).stream().map(CategoriaMapper::paraResposta).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResposta buscar(Long eventoId, Long id) {
        return CategoriaMapper.paraResposta(buscarEntidade(eventoId, id));
    }

    @Transactional
    public CategoriaResposta criar(Long eventoId, CategoriaRequisicao req) {
        var evento = eventoService.buscarEntidade(eventoId);
        evento.exigirEdicaoPermitida();
        var categoria = new Categoria(evento, req.nome(), req.meta(), req.unidade());
        return CategoriaMapper.paraResposta(repository.save(categoria));
    }

    @Transactional
    public CategoriaResposta editar(Long eventoId, Long id, CategoriaRequisicao req) {
        eventoService.buscarEntidade(eventoId).exigirEdicaoPermitida();
        var categoria = buscarEntidade(eventoId, id);
        categoria.atualizar(req.nome(), req.meta(), req.unidade());
        return CategoriaMapper.paraResposta(categoria);
    }

    @Transactional
    public void excluir(Long eventoId, Long id) {
        eventoService.buscarEntidade(eventoId).exigirEdicaoPermitida();
        var categoria = buscarEntidade(eventoId, id);
        if (itemRepository.existsByCategoriaId(id)) {
            throw new RegraNegocioException("Remova os itens da categoria antes de excluí-la.");
        }
        repository.delete(categoria);
    }

    public Categoria buscarEntidade(Long eventoId, Long id) {
        return repository.findByIdAndEventoId(id, eventoId)
                .orElseThrow(() -> new NaoEncontradoException("Categoria " + id + " não encontrada neste evento."));
    }
}
