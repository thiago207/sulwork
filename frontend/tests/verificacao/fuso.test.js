import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { aceitaContribuicoes, transicoesVisiveis } from '../../src/logica/status.js';
import { formatarDataHora } from '../../src/logica/rotulos.js';

// A API manda "2026-11-20T12:00:00" sem fuso, pensando em America/Recife (UTC-3).

const evento = {
  status: 'ABERTO',
  prazoAlteracoes: '2026-11-20T12:00:00',
  dataHora: '2026-11-20T13:00:00',
  proximosStatus: ['ENCERRADO', 'CANCELADO'],
};

function usarFuso(fuso) {
  // o Node relê o fuso quando process.env.TZ muda
  process.env.TZ = fuso;
}

describe('o front usa o horário de Recife em qualquer fuso do navegador', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => {
    vi.useRealTimers();
    usarFuso('America/Recife');
  });

  it('o ambiente de teste consegue trocar de fuso (pré-condição)', () => {
    usarFuso('America/Manaus');
    expect(new Date('2026-11-20T12:00:00').getTimezoneOffset()).toBe(240);
    usarFuso('America/Recife');
    expect(new Date('2026-11-20T12:00:00').getTimezoneOffset()).toBe(180);
  });

  it('fecha as contribuições depois do prazo tanto em Recife quanto em Manaus', () => {
    // 15h30 UTC = 12h30 em Recife = 11h30 em Manaus
    vi.setSystemTime(new Date('2026-11-20T15:30:00Z'));

    usarFuso('America/Recife');
    expect(aceitaContribuicoes(evento, new Date())).toBe(false);

    usarFuso('America/Manaus');
    expect(aceitaContribuicoes(evento, new Date())).toBe(false);
  });

  it('em Lisboa mantém a contribuição disponível até o prazo de Recife', () => {
    // 14h UTC = 11h em Recife: o back ainda aceita até 12h
    vi.setSystemTime(new Date('2026-11-20T14:00:00Z'));

    usarFuso('Europe/Lisbon');
    expect(aceitaContribuicoes(evento, new Date())).toBe(true);
  });

  it('em Manaus pode marcar como realizado quando chega a hora do evento em Recife', () => {
    // 16h30 UTC = 13h30 em Recife (evento já aconteceu) = 12h30 em Manaus
    vi.setSystemTime(new Date('2026-11-20T16:30:00Z'));
    const encerrado = { ...evento, status: 'ENCERRADO', proximosStatus: ['REALIZADO', 'CANCELADO'] };

    usarFuso('America/Manaus');
    expect(transicoesVisiveis(encerrado, new Date())).toContain('REALIZADO');
  });

  it('em Manaus exibe a hora do calendário de Recife', () => {
    usarFuso('America/Manaus');
    expect(formatarDataHora('2026-11-20T12:00:00')).toBe('20/11/2026, 12:00');
  });

  it('permite contribuir um milissegundo antes e bloqueia no instante do prazo', () => {
    usarFuso('Europe/Lisbon');
    expect(aceitaContribuicoes(evento, new Date('2026-11-20T14:59:59.999Z'))).toBe(true);
    expect(aceitaContribuicoes(evento, new Date('2026-11-20T15:00:00Z'))).toBe(false);
  });
});
