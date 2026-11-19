package com.sulwork.backend.participante;

import com.sulwork.backend.comum.erro.ConflitoException;
import com.sulwork.backend.comum.erro.NaoEncontradoException;
import com.sulwork.backend.comum.erro.RegraNegocioException;
import com.sulwork.backend.evento.StatusEvento;
import com.sulwork.backend.participacao.ParticipacaoRepository;
import com.sulwork.backend.participante.cpf.CpfValidator;
import com.sulwork.backend.participante.dto.ParticipanteEdicaoRequisicao;
import com.sulwork.backend.participante.dto.ParticipanteRequisicao;
import com.sulwork.backend.participante.dto.ParticipanteResposta;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParticipanteService {

    private static final EnumSet<StatusEvento> EM_ANDAMENTO = EnumSet.of(StatusEvento.ABERTO, StatusEvento.ENCERRADO);

    private final ParticipanteRepository repository;
    private final ParticipacaoRepository participacaoRepository;

    @Transactional
    public ParticipanteResposta criar(ParticipanteRequisicao req) {
        String cpf = CpfValidator.somenteNumeros(req.cpf());
        var existente = repository.findByCpf(cpf);
        if (existente.isPresent()) {
            var participante = existente.get();
            if (participante.isAtivo()) {
                throw new ConflitoException("Já existe um participante com este CPF.");
            }
            participante.reativar(req.nome(), req.restricoes(), req.observacaoRestricao());
            return ParticipanteMapper.paraResposta(participante);
        }
        var participante = new Participante(req.nome(), cpf, req.restricoes(), req.observacaoRestricao());
        return ParticipanteMapper.paraResposta(repository.save(participante));
    }

    @Transactional(readOnly = true)
    public List<ParticipanteResposta> listar() {
        return repository.findByAtivoTrueOrderByNome().stream().map(ParticipanteMapper::paraResposta).toList();
    }

    @Transactional(readOnly = true)
    public ParticipanteResposta buscar(Long id) {
        return ParticipanteMapper.paraResposta(buscarEntidade(id));
    }

    @Transactional
    public ParticipanteResposta editar(Long id, ParticipanteEdicaoRequisicao req) {
        var participante = buscarEntidade(id);
        String cpf = req.cpf() == null || req.cpf().isBlank()
                ? participante.getCpf()
                : CpfValidator.somenteNumeros(req.cpf());
        exigirCpfLivre(cpf, id);
        participante.atualizar(req.nome(), cpf, req.restricoes(), req.observacaoRestricao());
        return ParticipanteMapper.paraResposta(participante);
    }

    // sair do evento respeita o prazo (regra 2), então quem está num evento em andamento sai antes
    @Transactional
    public void excluir(Long id) {
        var participante = buscarEntidade(id);
        var eventos = participacaoRepository.findNomesDosEventos(id, EM_ANDAMENTO);
        if (!eventos.isEmpty()) {
            throw new RegraNegocioException("O participante está no evento \"" + eventos.getFirst()
                    + "\", que ainda não terminou. Tire-o do evento antes de excluir.");
        }
        participante.desativar();
    }

    public Participante buscarEntidade(Long id) {
        return repository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new NaoEncontradoException("Participante " + id + " não encontrado."));
    }

    // o CPF é único também entre os inativos
    private void exigirCpfLivre(String cpf, Long idAtual) {
        repository.findByCpf(cpf)
                .filter(existente -> !existente.getId().equals(idAtual))
                .ifPresent(existente -> {
                    throw new ConflitoException("Já existe um participante com este CPF.");
                });
    }
}
