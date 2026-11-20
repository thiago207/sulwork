import { describe, expect, it } from 'vitest';
import { aceitaContribuicoes, botaoDaTransicao, confirmacaoDaTransicao, podeEditar, transicoesVisiveis } from '../src/logica/status.js';

const PRAZO = '2026-12-16T18:00:00';
const ANTES_DO_PRAZO = new Date('2026-12-10T10:00:00-03:00');
const DEPOIS_DO_PRAZO = new Date('2026-12-17T10:00:00-03:00');
const DATA = '2026-12-18T08:00:00';
const DEPOIS_DO_EVENTO = new Date('2026-12-18T09:00:00-03:00');

const evento = (status, proximosStatus) => ({ status, proximosStatus, prazoAlteracoes: PRAZO, dataHora: DATA });

describe('transicoesVisiveis', () => {
  it('mostra as transições que o back permite', () => {
    expect(transicoesVisiveis(evento('RASCUNHO', ['ABERTO', 'CANCELADO']), ANTES_DO_PRAZO))
      .toEqual(['ABERTO', 'CANCELADO']);
    expect(transicoesVisiveis(evento('ABERTO', ['ENCERRADO', 'CANCELADO']), DEPOIS_DO_PRAZO))
      .toEqual(['ENCERRADO', 'CANCELADO']);
  });

  it('mostra reabrir antes do prazo', () => {
    expect(transicoesVisiveis(evento('ENCERRADO', ['REALIZADO', 'ABERTO', 'CANCELADO']), ANTES_DO_PRAZO))
      .toEqual(['ABERTO', 'CANCELADO']);
  });

  it('esconde reabrir depois do prazo', () => {
    expect(transicoesVisiveis(evento('ENCERRADO', ['REALIZADO', 'ABERTO', 'CANCELADO']), DEPOIS_DO_PRAZO))
      .toEqual(['CANCELADO']);
  });

  it('só mostra marcar como realizado depois da data do evento', () => {
    expect(transicoesVisiveis(evento('ENCERRADO', ['REALIZADO', 'ABERTO', 'CANCELADO']), DEPOIS_DO_EVENTO))
      .toEqual(['REALIZADO', 'CANCELADO']);
  });

  it('não mostra nada nos status finais', () => {
    expect(transicoesVisiveis(evento('REALIZADO', []), ANTES_DO_PRAZO)).toEqual([]);
    expect(transicoesVisiveis(evento('CANCELADO', []), ANTES_DO_PRAZO)).toEqual([]);
  });
});

describe('botaoDaTransicao', () => {
  it('diferencia abrir de reabrir', () => {
    expect(botaoDaTransicao('RASCUNHO', 'ABERTO').texto).toBe('Abrir');
    expect(botaoDaTransicao('ENCERRADO', 'ABERTO').texto).toBe('Reabrir');
  });

  it('cancelar tem estilo de perigo', () => {
    expect(botaoDaTransicao('ABERTO', 'CANCELADO')).toEqual({ texto: 'Cancelar evento', estilo: 'outline-danger' });
  });
});

describe('confirmacaoDaTransicao', () => {
  it('avisa que realizado e cancelado são finais', () => {
    expect(confirmacaoDaTransicao('ENCERRADO', 'REALIZADO').texto).toContain('final');
    expect(confirmacaoDaTransicao('ABERTO', 'CANCELADO')).toMatchObject({ botao: 'Cancelar evento', estilo: 'danger' });
  });

  it('diferencia abrir de reabrir', () => {
    expect(confirmacaoDaTransicao('RASCUNHO', 'ABERTO').titulo).toBe('Abrir o evento?');
    expect(confirmacaoDaTransicao('ENCERRADO', 'ABERTO').titulo).toBe('Reabrir o evento?');
  });
});

describe('regras de tela', () => {
  it('edita só em RASCUNHO e ABERTO', () => {
    expect(['RASCUNHO', 'ABERTO', 'ENCERRADO', 'REALIZADO', 'CANCELADO'].filter((s) => podeEditar({ status: s })))
      .toEqual(['RASCUNHO', 'ABERTO']);
  });

  it('aceita contribuições só aberto e antes do prazo', () => {
    expect(aceitaContribuicoes(evento('ABERTO'), ANTES_DO_PRAZO)).toBe(true);
    expect(aceitaContribuicoes(evento('ABERTO'), DEPOIS_DO_PRAZO)).toBe(false);
    expect(aceitaContribuicoes(evento('RASCUNHO'), ANTES_DO_PRAZO)).toBe(false);
  });
});
