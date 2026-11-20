package com.sulwork.backend.participacao;

import com.sulwork.backend.comum.erro.NaoEncontradoException;
import com.sulwork.backend.contribuicao.ContribuicaoRepository;
import com.sulwork.backend.evento.EventoService;
import com.sulwork.backend.participacao.dto.ParticipacaoResposta;
import com.sulwork.backend.participante.ParticipanteMapper;
import com.sulwork.backend.participante.ParticipanteService;
import com.sulwork.backend.participante.dto.ParticipanteResposta;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParticipacaoService {

    private final ParticipacaoRepository repository;
    private final EventoService eventoService;
    private final ParticipanteService participanteService;
    private final ContribuicaoRepository contribuicaoRepository;
    private final Clock relogio;

    @Transactional
    public ParticipacaoResposta entrar(Long eventoId, Long participanteId) {
        var evento = eventoService.travarParaAlteracao(eventoId);
        var participante = participanteService.travarAtivo(participanteId);
        evento.exigirAlteracoesAbertas(LocalDateTime.now(relogio));

        // regra 5: se já participou e saiu, reativa a mesma linha
        var participacao = repository.findByEventoIdAndParticipanteId(eventoId, participanteId)
                .map(existente -> {
                    existente.reativar();
                    return existente;
                })
                .orElseGet(() -> repository.save(new Participacao(evento, participante)));
        return paraResposta(participacao);
    }

    @Transactional
    public void sair(Long eventoId, Long participanteId) {
        var evento = eventoService.travarParaAlteracao(eventoId);
        evento.exigirAlteracoesAbertas(LocalDateTime.now(relogio));

        var participacao = repository.findByEventoIdAndParticipanteId(eventoId, participanteId)
                .filter(Participacao::isAtivo)
                .orElseThrow(() -> new NaoEncontradoException("O participante não está neste evento."));
        // regra 5: sair desativa também as contribuições; se voltar, elas continuam inativas
        contribuicaoRepository.desativarDaParticipacao(participacao.getId());
        participacao.sair();
    }

    @Transactional(readOnly = true)
    public List<ParticipanteResposta> listarParticipantesAtivos(Long eventoId) {
        eventoService.buscarEntidade(eventoId);
        return repository.findParticipantesAtivos(eventoId).stream().map(ParticipanteMapper::paraResposta).toList();
    }

    private static ParticipacaoResposta paraResposta(Participacao p) {
        return new ParticipacaoResposta(p.getId(), p.getEvento().getId(), p.getParticipante().getId(), p.isAtivo());
    }
}
