package com.sulwork.backend.participacao;

import com.sulwork.backend.comum.erro.ConflitoException;
import com.sulwork.backend.evento.Evento;
import com.sulwork.backend.participante.Participante;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Participante participante;

    private boolean ativo = true;

    public Participacao(Evento evento, Participante participante) {
        this.evento = evento;
        this.participante = participante;
    }

    public void reativar() {
        if (ativo) {
            throw new ConflitoException("O participante já está neste evento.");
        }
        this.ativo = true;
    }

    public void sair() {
        this.ativo = false;
    }
}
