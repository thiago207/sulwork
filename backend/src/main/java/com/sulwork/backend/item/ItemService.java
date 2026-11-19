package com.sulwork.backend.item;

import com.sulwork.backend.categoria.CategoriaService;
import com.sulwork.backend.comum.erro.NaoEncontradoException;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import com.sulwork.backend.evento.EventoService;
import com.sulwork.backend.item.dto.ItemRequisicao;
import com.sulwork.backend.item.dto.ItemResposta;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository repository;
    private final CategoriaService categoriaService;
    private final EventoService eventoService;

    @Transactional(readOnly = true)
    public List<ItemResposta> listar(Long eventoId) {
        eventoService.buscarEntidade(eventoId);
        return repository.findByEvento(eventoId).stream().map(ItemMapper::paraResposta).toList();
    }

    @Transactional(readOnly = true)
    public ItemResposta buscar(Long eventoId, Long id) {
        return ItemMapper.paraResposta(buscarEntidade(eventoId, id));
    }

    @Transactional
    public ItemResposta criar(Long eventoId, ItemRequisicao req) {
        eventoService.buscarEntidade(eventoId).exigirEdicaoPermitida();
        var categoria = categoriaService.buscarEntidade(eventoId, req.categoriaId());
        var item = new Item(categoria, req.nome(), req.quantidadeNecessaria(), req.obrigatorio(), req.alergenos());
        return ItemMapper.paraResposta(repository.save(item));
    }

    @Transactional
    public ItemResposta editar(Long eventoId, Long id, ItemRequisicao req) {
        eventoService.buscarEntidade(eventoId).exigirEdicaoPermitida();
        var item = buscarEntidade(eventoId, id);
        var categoria = categoriaService.buscarEntidade(eventoId, req.categoriaId());
        item.atualizar(categoria, req.nome(), req.quantidadeNecessaria(), req.obrigatorio(), req.alergenos(),
                totalContribuido(item));
        return ItemMapper.paraResposta(item);
    }

    @Transactional
    public void excluir(Long eventoId, Long id) {
        eventoService.buscarEntidade(eventoId).exigirEdicaoPermitida();
        var item = buscarEntidade(eventoId, id);
        if (totalContribuido(item) > 0) {
            throw new RegraNegocioException("Não é possível excluir um item que já recebeu contribuições.");
        }
        repository.delete(item);
    }

    public Item buscarEntidade(Long eventoId, Long id) {
        return repository.findByIdAndEvento(id, eventoId)
                .orElseThrow(() -> new NaoEncontradoException("Item " + id + " não encontrado neste evento."));
    }

    // etapa 5: somar as contribuições ativas do item
    int totalContribuido(Item item) {
        return 0;
    }
}
