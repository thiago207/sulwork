package com.sulwork.backend.evento;

import com.sulwork.backend.comum.erro.NaoEncontradoException;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import com.sulwork.backend.item.ItemRepository;
import com.sulwork.backend.evento.dto.EventoRequisicao;
import com.sulwork.backend.evento.dto.EventoResposta;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository repository;
    private final ItemRepository itemRepository;
    private final Clock relogio;

    @Transactional
    public EventoResposta criar(EventoRequisicao req) {
        var evento = new Evento(req.nome(), req.descricao(), req.tipo(), req.dataHora(), req.prazoAlteracoes(), agora());
        return EventoMapper.paraResposta(repository.save(evento));
    }

    @Transactional(readOnly = true)
    public List<EventoResposta> listar(StatusEvento status) {
        var eventos = status == null
                ? repository.findAllByOrderByDataHoraAsc()
                : repository.findByStatusOrderByDataHoraAsc(status);
        return eventos.stream().map(EventoMapper::paraResposta).toList();
    }

    @Transactional(readOnly = true)
    public EventoResposta buscar(Long id) {
        return EventoMapper.paraResposta(buscarEntidade(id));
    }

    @Transactional
    public EventoResposta editar(Long id, EventoRequisicao req) {
        var evento = travarParaAlteracao(id);
        evento.editar(req.nome(), req.descricao(), req.tipo(), req.dataHora(), req.prazoAlteracoes(), agora());
        return EventoMapper.paraResposta(evento);
    }

    @Transactional
    public void excluir(Long id) {
        var evento = travarParaAlteracao(id);
        evento.validarExclusao();
        repository.delete(evento);
    }

    @Transactional
    public EventoResposta mudarStatus(Long id, StatusEvento destino) {
        var evento = travarParaAlteracao(id);
        if (evento.getStatus() == StatusEvento.RASCUNHO && destino == StatusEvento.ABERTO
                && !itemRepository.existsByCategoriaEventoId(id)) {
            throw new RegraNegocioException("Cadastre pelo menos um item antes de abrir o evento.");
        }
        evento.mudarStatus(destino, agora());
        return EventoMapper.paraResposta(evento);
    }

    public Evento buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Evento " + id + " não encontrado."));
    }

    // Toda escrita em um evento começa pelo mesmo lock, antes de acessar os registros vinculados.
    public Evento travarParaAlteracao(Long id) {
        return repository.findByIdComLock(id)
                .orElseThrow(() -> new NaoEncontradoException("Evento " + id + " não encontrado."));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }
}
