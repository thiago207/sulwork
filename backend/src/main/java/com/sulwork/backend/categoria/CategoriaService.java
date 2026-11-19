package com.sulwork.backend.categoria;

import com.sulwork.backend.categoria.dto.CategoriaRequisicao;
import com.sulwork.backend.categoria.dto.CategoriaResposta;
import com.sulwork.backend.comum.erro.ConflitoException;
import com.sulwork.backend.comum.erro.NaoEncontradoException;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import com.sulwork.backend.contribuicao.ContribuicaoRepository;
import com.sulwork.backend.evento.EventoService;
import com.sulwork.backend.item.ItemRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repository;
    private final ItemRepository itemRepository;
    private final EventoService eventoService;
    private final ContribuicaoRepository contribuicaoRepository;

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
        exigirNomeLivre(eventoId, req.nome(), null);
        var categoria = new Categoria(evento, req.nome(), req.meta(), req.unidade());
        return CategoriaMapper.paraResposta(repository.save(categoria));
    }

    @Transactional
    public CategoriaResposta editar(Long eventoId, Long id, CategoriaRequisicao req) {
        eventoService.buscarEntidade(eventoId).exigirEdicaoPermitida();
        var categoria = buscarEntidade(eventoId, id);
        exigirNomeLivre(eventoId, req.nome(), id);
        if (req.unidade() != categoria.getUnidade() && contribuicaoRepository.somarAtivasDaCategoria(id) > 0) {
            throw new RegraNegocioException("A categoria já tem contribuições e não pode trocar de unidade.");
        }
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

    // "Refrigerante", "refrigerante" e "Refrigerânte" contam como o mesmo nome
    private void exigirNomeLivre(Long eventoId, String nome, Long idAtual) {
        String chave = normalizar(nome);
        boolean repetido = repository.findByEventoIdOrderByNome(eventoId).stream()
                .anyMatch(c -> !c.getId().equals(idAtual) && normalizar(c.getNome()).equals(chave));
        if (repetido) {
            throw new ConflitoException("Já existe uma categoria com este nome no evento.");
        }
    }

    static String normalizar(String nome) {
        String semAcento = Normalizer.normalize(nome.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }

    public Categoria buscarEntidade(Long eventoId, Long id) {
        return repository.findByIdAndEventoId(id, eventoId)
                .orElseThrow(() -> new NaoEncontradoException("Categoria " + id + " não encontrada neste evento."));
    }
}
