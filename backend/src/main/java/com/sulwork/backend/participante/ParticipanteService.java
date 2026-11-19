package com.sulwork.backend.participante;

import com.sulwork.backend.comum.erro.ConflitoException;
import com.sulwork.backend.comum.erro.NaoEncontradoException;
import com.sulwork.backend.participante.cpf.CpfValidator;
import com.sulwork.backend.participante.dto.ParticipanteRequisicao;
import com.sulwork.backend.participante.dto.ParticipanteResposta;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParticipanteService {

    private final ParticipanteRepository repository;

    @Transactional
    public ParticipanteResposta criar(ParticipanteRequisicao req) {
        String cpf = CpfValidator.somenteNumeros(req.cpf());
        exigirCpfLivre(cpf, null);
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
    public ParticipanteResposta editar(Long id, ParticipanteRequisicao req) {
        var participante = buscarEntidade(id);
        String cpf = CpfValidator.somenteNumeros(req.cpf());
        exigirCpfLivre(cpf, id);
        participante.atualizar(req.nome(), cpf, req.restricoes(), req.observacaoRestricao());
        return ParticipanteMapper.paraResposta(participante);
    }

    @Transactional
    public void excluir(Long id) {
        buscarEntidade(id).desativar();
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
                    throw new ConflitoException(existente.isAtivo()
                            ? "Já existe um participante com este CPF."
                            : "Já existe um participante inativo com este CPF.");
                });
    }
}
