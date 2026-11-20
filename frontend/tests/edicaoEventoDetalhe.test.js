import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../src/api.js';
import { desenharContribuicoes } from '../src/paginas/detalhe/abaContribuicoes.js';
import { desenharItens } from '../src/paginas/detalhe/abaItens.js';

vi.mock('../src/api.js', () => ({
  api: { editarCategoria: vi.fn(), editarItem: vi.fn(), editarContribuicao: vi.fn() },
}));

const categoria = { id: 2, nome: 'Bebidas', meta: 12, unidade: 'LITRO' };
const item = {
  id: 3, categoriaId: 2, nome: 'Suco', quantidadeNecessaria: 6, unidade: 'LITRO',
  obrigatorio: true, alergenos: ['MEL'],
};
const contribuicao = {
  id: 4, participanteId: 5, participanteNome: 'Ana', itemId: 3, itemNome: 'Suco',
  quantidade: 4, unidade: 'LITRO', avisos: [],
};

describe('edição no detalhe do evento', () => {
  let aba;
  let executar;

  beforeEach(() => {
    vi.clearAllMocks();
    aba = document.createElement('section');
    document.body.replaceChildren(aba);
    executar = vi.fn((acao) => acao());
  });

  function abrirEdicao(acao, formulario) {
    aba.querySelector(`[data-acao="${acao}"]`).click();
    return aba.querySelector(formulario);
  }

  function enviar(form) {
    form.dispatchEvent(new Event('submit', { cancelable: true }));
  }

  it('permite reduzir a contribuição de um item completo e rejeita valor acima do limite', () => {
    desenharContribuicoes(aba, {
      contribuicoes: [contribuicao],
      participantes: [{ id: 5, nome: 'Ana' }],
      cobertura: { itens: [{ itemId: 3, nome: 'Suco', faltante: 0, unidade: 'LITRO' }] },
      aberto: true,
    }, { id: 1, executar });

    const form = abrirEdicao('editar-contribuicao', '#form-contribuicao');
    expect(form.elements.itemId.value).toBe('3');
    expect(form.elements.quantidade.value).toBe('4');
    form.elements.quantidade.value = '5';
    enviar(form);
    expect(executar).not.toHaveBeenCalled();
    expect(form.elements.quantidade.classList.contains('is-invalid')).toBe(true);

    form.elements.quantidade.value = '3';
    enviar(form);
    expect(api.editarContribuicao).toHaveBeenCalledWith(1, 4, { quantidade: 3 });
  });

  it('preenche o formulário da categoria e envia os valores editados', () => {
    desenharItens(aba, { categorias: [categoria], itens: [item], editavel: true }, { id: 1, executar });
    const form = abrirEdicao('editar-categoria', '#form-categoria');
    expect(form.elements.nome.value).toBe('Bebidas');
    expect(form.elements.unidade.value).toBe('LITRO');
    form.elements.meta.value = '15';
    enviar(form);
    expect(api.editarCategoria).toHaveBeenCalledWith(1, 2, { nome: 'Bebidas', unidade: 'LITRO', meta: 15 });
  });

  it('preserva os alérgenos e a obrigatoriedade do item ao editar', () => {
    desenharItens(aba, { categorias: [categoria], itens: [item], editavel: true }, { id: 1, executar });
    const form = abrirEdicao('editar-item', '#form-item');
    expect(form.querySelector('[name="alergenos"][value="MEL"]').checked).toBe(true);
    form.elements.nome.value = 'Suco de laranja';
    enviar(form);
    expect(api.editarItem).toHaveBeenCalledWith(1, 3, {
      nome: 'Suco de laranja', categoriaId: 2, quantidadeNecessaria: 6, obrigatorio: true, alergenos: ['MEL'],
    });
  });
});
