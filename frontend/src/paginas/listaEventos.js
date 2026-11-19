import { api } from '../api.js';
import { escapar, opcoes, tentar } from '../ui.js';
import { STATUS, TIPOS, formatarDataHora, rotulo } from '../logica/rotulos.js';
import { corDoStatus } from '../logica/status.js';

export function htmlTabelaEventos(eventos) {
  if (!eventos.length) {
    return '<p class="text-muted">Nenhum evento encontrado.</p>';
  }
  const linhas = eventos.map((e) => `
    <tr>
      <td><a href="#/eventos/${e.id}">${escapar(e.nome)}</a></td>
      <td>${escapar(rotulo(TIPOS, e.tipo))}</td>
      <td>${escapar(formatarDataHora(e.dataHora))}</td>
      <td><span class="badge text-bg-${corDoStatus(e.status)}">${escapar(rotulo(STATUS, e.status))}</span></td>
    </tr>`).join('');
  return `
    <table class="table table-hover bg-white">
      <thead><tr><th>Nome</th><th>Tipo</th><th>Data</th><th>Status</th></tr></thead>
      <tbody>${linhas}</tbody>
    </table>`;
}

export async function renderListaEventos(container) {
  container.innerHTML = `
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h1 class="h3 mb-0">Eventos</h1>
      <a class="btn btn-primary" href="#/eventos/novo">Novo evento</a>
    </div>
    <select class="form-select mb-3 w-auto" id="filtro-status" aria-label="Filtrar por status">
      <option value="">Todos os status</option>${opcoes(Object.entries(STATUS))}
    </select>
    <div id="tabela-eventos"></div>`;

  const tabela = container.querySelector('#tabela-eventos');
  const desenhar = async (status) => {
    tabela.innerHTML = htmlTabelaEventos(await api.listarEventos(status));
  };
  container.querySelector('#filtro-status')
    .addEventListener('change', (e) => tentar(() => desenhar(e.target.value)));
  await desenhar('');
}
