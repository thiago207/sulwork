import { describe, expect, it } from 'vitest';
import { classeBarra, formatarPercentual, larguraBarra } from '../src/logica/cobertura.js';

describe('formatarPercentual', () => {
  it('usa vírgula e uma casa decimal', () => {
    expect(formatarPercentual(62.5)).toBe('62,5%');
    expect(formatarPercentual(66.7)).toBe('66,7%');
  });

  it('mostra a casa decimal mesmo em números inteiros', () => {
    expect(formatarPercentual(100)).toBe('100,0%');
    expect(formatarPercentual(0)).toBe('0,0%');
  });

  it('trata valor ausente como zero', () => {
    expect(formatarPercentual(undefined)).toBe('0,0%');
  });
});

describe('classeBarra', () => {
  it.each([
    [0, 'bg-danger'],
    [49.9, 'bg-danger'],
    [50, 'bg-warning'],
    [99.9, 'bg-warning'],
    [100, 'bg-success'],
  ])('%s%% fica %s', (percentual, classe) => {
    expect(classeBarra(percentual)).toBe(classe);
  });
});

describe('larguraBarra', () => {
  it('fica sempre entre 0 e 100', () => {
    expect(larguraBarra(-5)).toBe(0);
    expect(larguraBarra(62.5)).toBe(62.5);
    expect(larguraBarra(130)).toBe(100);
  });
});
