import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '../src/api.js';
import { renderListaEventos } from '../src/paginas/eventos/lista.js';

// troca o módulo inteiro da API por funções falsas: nada de rede no teste
vi.mock('../src/api.js', () => ({
  api: { listarEventos: vi.fn() },
}));

const eventos = [
  { id: 1, nome: 'Café de fim de ano', tipo: 'CAFE_DA_MANHA', dataHora: '2026-12-18T08:00:00', status: 'ABERTO' },
  { id: 2, nome: '<img src=x onerror=alert(1)>', tipo: 'ALMOCO', dataHora: '2026-12-20T12:00:00', status: 'RASCUNHO' },
];

describe('tela de lista de eventos', () => {
  let container;
  const linhas = () => container.querySelectorAll('#lista-eventos .evento-linha');

  beforeEach(() => {
    document.body.innerHTML = '<div id="alertas"></div><main id="app"></main>';
    container = document.getElementById('app');
    api.listarEventos.mockReset();
  });

  it('mostra uma linha por evento com data, tipo e status', async () => {
    api.listarEventos.mockResolvedValue(eventos);

    await renderListaEventos(container);

    expect(linhas()).toHaveLength(2);
    const primeiro = linhas()[0];
    expect(primeiro.getAttribute('href')).toBe('#/eventos/1');
    expect(primeiro.querySelector('[data-campo="dia"]').textContent).toBe('18');
    expect(primeiro.querySelector('[data-campo="mes"]').textContent).toBe('dez');
    expect(primeiro.textContent).toContain('Café da manhã');
    expect(primeiro.querySelector('.bi').classList).toContain('bi-cup-hot');

    const status = primeiro.querySelector('.badge-status');
    expect(status.textContent).toBe('Aberto');
    expect(status.dataset.status).toBe('ABERTO');
    expect(container.querySelector('#sem-eventos').hidden).toBe(true);
  });

  it('mostra o nome vindo da API como texto em vez de interpretar como HTML', async () => {
    api.listarEventos.mockResolvedValue(eventos);

    await renderListaEventos(container);

    expect(container.querySelector('#lista-eventos img')).toBeNull();
    expect(linhas()[1].querySelector('[data-campo="nome"]').textContent).toBe('<img src=x onerror=alert(1)>');
  });

  it('filtrar por status chama a API com o status escolhido', async () => {
    api.listarEventos.mockResolvedValueOnce(eventos).mockResolvedValueOnce([eventos[0]]);
    await renderListaEventos(container);

    const filtro = container.querySelector('#filtro-status');
    expect(filtro.options).toHaveLength(6);
    filtro.value = 'ABERTO';
    filtro.dispatchEvent(new Event('change'));
    await vi.waitFor(() => expect(linhas()).toHaveLength(1));

    expect(api.listarEventos).toHaveBeenLastCalledWith('ABERTO');
  });

  it('mostra o aviso de lista vazia quando não há eventos', async () => {
    api.listarEventos.mockResolvedValue([]);

    await renderListaEventos(container);

    expect(linhas()).toHaveLength(0);
    expect(container.querySelector('#sem-eventos').hidden).toBe(false);
  });
});
