import { describe, expect, it } from 'vitest';
import { validarContribuicao } from '../src/logica/validacao.js';

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
