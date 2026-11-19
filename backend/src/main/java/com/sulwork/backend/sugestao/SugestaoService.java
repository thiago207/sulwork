package com.sulwork.backend.sugestao;

import com.sulwork.backend.cobertura.CalculoCobertura;
import com.sulwork.backend.cobertura.SomaItem;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import com.sulwork.backend.contribuicao.Contribuicao;
import com.sulwork.backend.contribuicao.ContribuicaoRepository;
import com.sulwork.backend.evento.EventoService;
import com.sulwork.backend.item.ItemRepository;
import com.sulwork.backend.participacao.Participacao;
import com.sulwork.backend.participacao.ParticipacaoRepository;
import com.sulwork.backend.participante.Participante;
import com.sulwork.backend.participante.ParticipanteService;
import com.sulwork.backend.sugestao.dto.SugestoesResposta;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SugestaoService {

    private final EventoService eventoService;
    private final ParticipanteService participanteService;
    private final ParticipacaoRepository participacaoRepository;
    private final ItemRepository itemRepository;
    private final ContribuicaoRepository contribuicaoRepository;
    private final CalculoCobertura calculo;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public SugestoesResposta sugerir(Long eventoId, Long participanteId) {
        var evento = eventoService.buscarEntidade(eventoId);
        var participante = participanteService.buscarEntidade(participanteId);
        evento.exigirAlteracoesAbertas(LocalDateTime.now(relogio));
        participacaoRepository.findByEventoIdAndParticipanteId(eventoId, participanteId)
                .filter(Participacao::isAtivo)
                .orElseThrow(() -> new RegraNegocioException("O participante não está ativo neste evento."));

        Map<Long, Long> contribuidoPorItem = contribuicaoRepository.somarPorItemDoEvento(eventoId).stream()
                .collect(Collectors.toMap(SomaItem::getItemId, SomaItem::getContribuido));
        Map<Long, Integer> minhasPorItem = contribuicaoRepository.findAtivasDoParticipante(participanteId).stream()
                .filter(c -> c.getParticipacao().getEvento().getId().equals(eventoId))
                .collect(Collectors.toMap(c -> c.getItem().getId(), Contribuicao::getQuantidade));
        var restricoesDosAtivos = participacaoRepository.findParticipantesAtivos(eventoId).stream()
                .map(Participante::getRestricoes)
                .toList();

        var candidatos = itemRepository.findByEvento(eventoId).stream()
                .map(i -> new Sugestoes.Candidato(i.getId(), i.getNome(), i.getCategoria().getNome(), i.getUnidade(),
                        i.isObrigatorio(), i.getQuantidadeNecessaria(), contribuidoPorItem.getOrDefault(i.getId(), 0L),
                        i.getAlergenos(), minhasPorItem.getOrDefault(i.getId(), 0)))
                .toList();

        var resultado = Sugestoes.montar(candidatos, restricoesDosAtivos, participante.getRestricoes(), calculo);
        return new SugestoesResposta(eventoId, participanteId, resultado.ocultosPorConflito(), resultado.sugestoes());
    }
}
