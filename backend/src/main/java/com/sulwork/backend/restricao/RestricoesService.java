package com.sulwork.backend.restricao;

import com.sulwork.backend.evento.EventoService;
import com.sulwork.backend.item.Alergeno;
import com.sulwork.backend.item.Item;
import com.sulwork.backend.item.ItemRepository;
import com.sulwork.backend.participacao.ParticipacaoRepository;
import com.sulwork.backend.participante.Participante;
import com.sulwork.backend.participante.RestricaoAlimentar;
import com.sulwork.backend.restricao.dto.RestricoesResposta;
import com.sulwork.backend.restricao.dto.RestricoesResposta.PorItem;
import com.sulwork.backend.restricao.dto.RestricoesResposta.Resumo;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestricoesService {

    private final ParticipacaoRepository participacaoRepository;
    private final ItemRepository itemRepository;
    private final EventoService eventoService;

    @Transactional(readOnly = true)
    public RestricoesResposta resumir(Long eventoId) {
        eventoService.buscarEntidade(eventoId);
        var participantes = participacaoRepository.findParticipantesAtivos(eventoId);
        var itens = itemRepository.findByEvento(eventoId);

        var resumo = Arrays.stream(RestricaoAlimentar.values())
                .map(r -> new Resumo(r, participantes.stream().filter(p -> p.getRestricoes().contains(r)).count()))
                .filter(r -> r.participantes() > 0)
                .toList();
        var porItem = itens.stream().map(item -> porItem(item, participantes)).toList();

        return new RestricoesResposta(eventoId, participantes.size(), resumo, porItem);
    }

    private static PorItem porItem(Item item, List<Participante> participantes) {
        long afetados = participantes.stream()
                .filter(p -> p.getRestricoes().stream().anyMatch(r -> r.conflitaCom(item.getAlergenos())))
                .count();
        var emConflito = EnumSet.noneOf(RestricaoAlimentar.class);
        participantes.forEach(p -> p.getRestricoes().stream()
                .filter(r -> r.conflitaCom(item.getAlergenos()))
                .forEach(emConflito::add));
        var alergenos = EnumSet.noneOf(Alergeno.class);
        alergenos.addAll(item.getAlergenos());
        return new PorItem(item.getId(), item.getNome(), alergenos, afetados, emConflito);
    }
}
