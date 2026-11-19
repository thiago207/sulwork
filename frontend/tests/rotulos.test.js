import { describe, expect, it } from 'vitest';
import { formatarCpf, iconeDoTipo, iniciais, mascararCpfDigitado, normalizar, partesDaData } from '../src/logica/rotulos.js';

describe('partesDaData', () => {
  it('separa dia, mês, dia da semana e hora em português', () => {
    expect(partesDaData('2026-12-18T08:00:00')).toEqual({ dia: '18', mes: 'dez', semana: 'sex', hora: '08:00' });
  });

  it('completa o dia com zero', () => {
    expect(partesDaData('2026-03-05T19:30:00').dia).toBe('05');
  });
});

describe('formatarCpf', () => {
  it('coloca pontos e traço', () => {
    expect(formatarCpf('52998224725')).toBe('529.982.247-25');
  });

  it('devolve como veio se não tiver 11 dígitos', () => {
    expect(formatarCpf('123')).toBe('123');
  });
});

describe('iniciais', () => {
  it('usa a primeira letra do primeiro e do último nome', () => {
    expect(iniciais('Ana Maria Souza')).toBe('AS');
    expect(iniciais('bruno')).toBe('B');
  });

  it('não quebra com nome vazio', () => {
    expect(iniciais('  ')).toBe('?');
  });
});

describe('iconeDoTipo', () => {
  it('tem ícone padrão para tipo desconhecido', () => {
    expect(iconeDoTipo('ALMOCO')).toBe('bi-egg-fried');
    expect(iconeDoTipo('XPTO')).toBe('bi-calendar-event');
  });
});

describe('mascararCpfDigitado', () => {
  it('formata aos poucos enquanto digita', () => {
    expect(mascararCpfDigitado('529')).toBe('529');
    expect(mascararCpfDigitado('5299')).toBe('529.9');
    expect(mascararCpfDigitado('5299822')).toBe('529.982.2');
    expect(mascararCpfDigitado('52998224725')).toBe('529.982.247-25');
  });

  it('ignora o que não é número e para em 11 dígitos', () => {
    expect(mascararCpfDigitado('529.982.247-2599')).toBe('529.982.247-25');
  });
});

describe('normalizar', () => {
  it('ignora maiúscula, minúscula e acento', () => {
    expect(normalizar(' João ')).toBe(normalizar('joao'));
  });
});
