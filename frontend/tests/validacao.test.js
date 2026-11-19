import { describe, expect, it } from 'vitest';
import { erroDeQuantidade, paraNumero, validarContribuicao } from '../src/logica/validacao.js';

const valida = { participanteId: '1', itemId: '2', quantidade: '3' };

describe('validarContribuicao', () => {
  it('sem erros quando tudo está certo', () => {
    expect(validarContribuicao(valida, 5)).toEqual({});
  });

  it('aceita exatamente o que falta', () => {
    expect(validarContribuicao({ ...valida, quantidade: '5' }, 5)).toEqual({});
  });

  it('exige participante, item e quantidade', () => {
    expect(validarContribuicao({ participanteId: '', itemId: '', quantidade: '' })).toEqual({
      participanteId: 'Escolha o participante.',
      itemId: 'Escolha o item.',
      quantidade: 'Informe a quantidade.',
    });
  });

  it.each(['0', '-2', '1.5', 'abc'])('rejeita a quantidade %s', (quantidade) => {
    expect(validarContribuicao({ ...valida, quantidade }, 5).quantidade)
      .toBe('A quantidade deve ser um número inteiro maior que zero.');
  });

  it('rejeita quantidade acima do faltante', () => {
    expect(validarContribuicao({ ...valida, quantidade: '6' }, 5).quantidade)
      .toBe('A quantidade passa do que falta para o item (faltam 5).');
  });

  it('sem faltante conhecido não limita a quantidade', () => {
    expect(validarContribuicao({ ...valida, quantidade: '999' })).toEqual({});
  });
});

describe('erroDeQuantidade', () => {
  it('aceita inteiro positivo', () => {
    expect(erroDeQuantidade('3')).toBeNull();
  });

  it('diferencia vazio, decimal e zero', () => {
    expect(erroDeQuantidade('')).toBe('Informe um valor.');
    expect(erroDeQuantidade('2.5')).toBe('Use um número inteiro, sem casas decimais.');
    expect(erroDeQuantidade('0')).toBe('Deve ser maior que zero.');
  });
});

describe('paraNumero', () => {
  it('zero continua zero e vazio vira null', () => {
    expect(paraNumero('0')).toBe(0);
    expect(paraNumero('')).toBeNull();
    expect(paraNumero('7')).toBe(7);
  });
});
