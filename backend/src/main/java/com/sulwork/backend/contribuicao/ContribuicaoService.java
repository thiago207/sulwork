package com.sulwork.backend.contribuicao;

import com.sulwork.backend.comum.erro.ConflitoException;
import com.sulwork.backend.comum.erro.NaoEncontradoException;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import com.sulwork.backend.contribuicao.dto.ContribuicaoRequisicao;
import com.sulwork.backend.contribuicao.dto.ContribuicaoResposta;
import com.sulwork.backend.evento.EventoService;
import com.sulwork.backend.item.Item;
import com.sulwork.backend.item.ItemRepository;
import com.sulwork.backend.participacao.Participacao;
import com.sulwork.backend.participacao.ParticipacaoRepository;
import com.sulwork.backend.participante.ParticipanteService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContribuicaoService {

    private final ContribuicaoRepository repository;
    private final ParticipacaoRepository participacaoRepository;
    private final ItemRepository itemRepository;
    private final EventoService eventoService;
    private final ParticipanteService participanteService;
    private final Clock relogio;

    @Transactional
    public ContribuicaoResposta criar(Long eventoId, ContribuicaoRequisicao req) {
        var evento = eventoService.travarParaAlteracao(eventoId);
        evento.exigirAlteracoesAbertas(agora());
        var participacao = participacaoAtiva(eventoId, req.participanteId());
        var item = travarItem(eventoId, req.itemId());

        if (repository.existsByParticipacaoIdAndItemIdAndAtivoTrue(participacao.getId(), item.getId())) {
            throw new ConflitoException("O participante já contribui com este item. Altere a contribuição existente.");
        }
        exigirQuantidadeDisponivel(item, repository.somarAtivasDoItem(item.getId()), req.quantidade());
        evento.exigirAlteracoesAbertas(agora());

        var contribuicao = repository.save(new Contribuicao(participacao, item, req.quantidade()));
        return ContribuicaoMapper.paraResposta(contribuicao);
    }

    @Transactional
    public ContribuicaoResposta alterar(Long eventoId, Long id, int quantidade) {
        var evento = eventoService.travarParaAlteracao(eventoId);
        evento.exigirAlteracoesAbertas(agora());
        var contribuicao = buscarAtiva(eventoId, id);
        var item = travarItem(eventoId, contribuicao.getItem().getId());

        exigirQuantidadeDisponivel(item, repository.somarOutrasAtivasDoItem(item.getId(), id), quantidade);
        evento.exigirAlteracoesAbertas(agora());
        contribuicao.alterarQuantidade(quantidade);
        return ContribuicaoMapper.paraResposta(contribuicao);
    }

    @Transactional
    public void remover(Long eventoId, Long id) {
        eventoService.travarParaAlteracao(eventoId).exigirAlteracoesAbertas(agora());
        buscarAtiva(eventoId, id).remover();
    }

    @Transactional(readOnly = true)
    public List<ContribuicaoResposta> listarDoEvento(Long eventoId) {
        eventoService.buscarEntidade(eventoId);
        return repository.findAtivasDoEvento(eventoId).stream().map(ContribuicaoMapper::paraResposta).toList();
    }

    @Transactional(readOnly = true)
    public List<ContribuicaoResposta> listarDoParticipante(Long participanteId) {
        participanteService.buscarEntidade(participanteId);
        return repository.findAtivasDoParticipante(participanteId).stream().map(ContribuicaoMapper::paraResposta).toList();
    }

    private Participacao participacaoAtiva(Long eventoId, Long participanteId) {
        return participacaoRepository.findByEventoIdAndParticipanteId(eventoId, participanteId)
                .filter(Participacao::isAtivo)
                .orElseThrow(() -> new RegraNegocioException("Só quem participa do evento pode contribuir."));
    }

    private Item travarItem(Long eventoId, Long itemId) {
        return itemRepository.findByIdAndEventoComLock(itemId, eventoId)
                .orElseThrow(() -> new NaoEncontradoException("Item " + itemId + " não encontrado neste evento."));
    }

    private Contribuicao buscarAtiva(Long eventoId, Long id) {
        return repository.findAtivaNoEvento(id, eventoId)
                .orElseThrow(() -> new NaoEncontradoException("Contribuição " + id + " não encontrada neste evento."));
    }

    // regra 7: a soma das ativas não pode passar do necessário
    private static void exigirQuantidadeDisponivel(Item item, long jaContribuido, int quantidade) {
        long faltam = Math.max(0, item.getQuantidadeNecessaria() - jaContribuido);
        if (quantidade > faltam) {
            throw new RegraNegocioException("A quantidade ultrapassa o necessário para o item " + item.getNome()
                    + ". Faltam " + faltam + ".");
        }
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }
}
