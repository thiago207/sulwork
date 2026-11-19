import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../src/api.js';
import { renderSugestoes } from '../src/paginas/sugestoes/sugestoes.js';

vi.mock('../src/api.js', () => ({
  api: {
    buscarEvento: vi.fn(),
    listarParticipantesDoEvento: vi.fn(),
    sugestoes: vi.fn(),
    criarContribuicao: vi.fn(),
  },
}));

const evento = { id: 8, nome: 'Confra', status: 'ABERTO', prazoAlteracoes: '2999-01-01T00:00:00' };
const coxinha = {
  itemId: 3, item: 'Coxinha', categoria: 'Salgados', unidade: 'UNIDADE', obrigatorio: true,
  faltante: 30, cobertura: 0, suaContribuicao: 0,
  conflitos: { participantes: 1, restricoes: ['SEM_GLUTEN'], comVoce: false },
};

describe('tela "O que eu posso levar?"', () => {
  let container;
  const escolher = async (id) => {
    const quem = container.querySelector('#quem');
    quem.value = id;
    quem.dispatchEvent(new Event('change'));
    await vi.waitFor(() => expect(container.querySelectorAll('.sugestao')).toHaveLength(1));
  };

  beforeEach(() => {
    document.body.innerHTML = '<div id="alertas"></div><main id="app"></main>';
    container = document.getElementById('app');
    vi.clearAllMocks();
    api.buscarEvento.mockResolvedValue(evento);
    api.listarParticipantesDoEvento.mockResolvedValue([{ id: 1, nome: 'Thiago' }]);
    api.sugestoes.mockResolvedValue({ ocultosPorConflito: 0, sugestoes: [coxinha] });
  });

  it('mostra o "Vou levar" com o que falta já preenchido', async () => {
    await renderSugestoes(container, { id: 8 });
    await escolher('1');

    const form = container.querySelector('.sugestao form');
    expect(form.hidden).toBe(false);
    expect(form.elements.quantidade.value).toBe('30');
  });

  it('conflito que não é com você não diz "sua restrição"', async () => {
    await renderSugestoes(container, { id: 8 });
    await escolher('1');

    expect(container.querySelector('[data-campo="conflito-voce"]').hidden).toBe(true);
  });

  it('"Vou levar" cria a contribuição e mostra o sucesso', async () => {
    api.criarContribuicao.mockResolvedValue({ quantidade: 5, unidade: 'UNIDADE', avisos: [] });
    await renderSugestoes(container, { id: 8 });
    await escolher('1');

    const form = container.querySelector('.sugestao form');
    form.elements.quantidade.value = '5';
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));

    await vi.waitFor(() => expect(document.querySelector('#alertas .alert-success')).not.toBeNull());
    expect(api.criarContribuicao).toHaveBeenCalledWith(8, { participanteId: 1, itemId: 3, quantidade: 5 });
    expect(document.querySelector('#alertas').textContent).toContain('você leva 5 unidade(s) de Coxinha');
  });
});
