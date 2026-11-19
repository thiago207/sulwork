package com.sulwork.backend.evento;

import static com.sulwork.backend.evento.StatusEvento.ABERTO;
import static com.sulwork.backend.evento.StatusEvento.ENCERRADO;
import static com.sulwork.backend.evento.StatusEvento.RASCUNHO;

import com.sulwork.backend.comum.erro.RegraNegocioException;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String descricao;

    @Enumerated(EnumType.STRING)
    private TipoEvento tipo;

    private LocalDateTime dataHora;
    private LocalDateTime prazoAlteracoes;

    @Enumerated(EnumType.STRING)
    private StatusEvento status;

    public Evento(String nome, String descricao, TipoEvento tipo,
                  LocalDateTime dataHora, LocalDateTime prazoAlteracoes, LocalDateTime agora) {
        exigirFuturo(dataHora, agora, "A data e hora do evento deve estar no futuro.");
        exigirFuturo(prazoAlteracoes, agora, "O prazo de alterações deve estar no futuro.");
        exigirPrazoAntesDaData(dataHora, prazoAlteracoes);
        this.nome = nome;
        this.descricao = descricao;
        this.tipo = tipo;
        this.dataHora = dataHora;
        this.prazoAlteracoes = prazoAlteracoes;
        this.status = RASCUNHO;
    }

    public void editar(String nome, String descricao, TipoEvento tipo,
                       LocalDateTime dataHora, LocalDateTime prazoAlteracoes, LocalDateTime agora) {
        if (status != RASCUNHO && status != ABERTO) {
            throw new RegraNegocioException(
                    "O evento só pode ser editado com status RASCUNHO ou ABERTO. Status atual: " + status + ".");
        }
        // só a data que mudou precisa estar no futuro
        if (!dataHora.equals(this.dataHora)) {
            exigirFuturo(dataHora, agora, "A data e hora do evento deve estar no futuro.");
        }
        if (!prazoAlteracoes.equals(this.prazoAlteracoes)) {
            exigirFuturo(prazoAlteracoes, agora, "O prazo de alterações deve estar no futuro.");
        }
        exigirPrazoAntesDaData(dataHora, prazoAlteracoes);
        this.nome = nome;
        this.descricao = descricao;
        this.tipo = tipo;
        this.dataHora = dataHora;
        this.prazoAlteracoes = prazoAlteracoes;
    }

    public void mudarStatus(StatusEvento destino, LocalDateTime agora) {
        if (!status.podeIrPara(destino)) {
            throw new RegraNegocioException("Não é permitido mudar o status de " + status + " para " + destino + ".");
        }
        if (status == ENCERRADO && destino == ABERTO && !agora.isBefore(prazoAlteracoes)) {
            throw new RegraNegocioException("Não é possível reabrir o evento: o prazo de alterações já venceu.");
        }
        this.status = destino;
    }

    // regra 2: entrar, sair e mexer em contribuições só com ABERTO e antes do prazo
    public void exigirAlteracoesAbertas(LocalDateTime agora) {
        if (status != ABERTO) {
            throw new RegraNegocioException("O evento precisa estar ABERTO para esta alteração. Status atual: " + status + ".");
        }
        if (!agora.isBefore(prazoAlteracoes)) {
            throw new RegraNegocioException("O prazo de alterações do evento já venceu.");
        }
    }

    public void validarExclusao() {
        if (status != RASCUNHO) {
            throw new RegraNegocioException(
                    "Só é possível excluir eventos com status RASCUNHO. Status atual: " + status + ".");
        }
    }

    private static void exigirFuturo(LocalDateTime data, LocalDateTime agora, String mensagem) {
        if (!data.isAfter(agora)) {
            throw new RegraNegocioException(mensagem);
        }
    }

    private static void exigirPrazoAntesDaData(LocalDateTime dataHora, LocalDateTime prazoAlteracoes) {
        if (!prazoAlteracoes.isBefore(dataHora)) {
            throw new RegraNegocioException("O prazo de alterações deve ser anterior à data e hora do evento.");
        }
    }
}
