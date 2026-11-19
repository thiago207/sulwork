import { api } from '../api.js';
import { escapar, mostrarErro, opcoes } from '../ui.js';
import { TIPOS } from '../logica/rotulos.js';

// a API manda "2026-12-18T08:00:00"; o input datetime-local quer "2026-12-18T08:00"
const paraInput = (iso) => (iso ? iso.slice(0, 16) : '');

export async function renderFormEvento(container, { id }) {
  const evento = id ? await api.buscarEvento(id) : {};

  container.innerHTML = `
    <h1 class="h3 mb-3">${id ? 'Editar evento' : 'Novo evento'}</h1>
    <form id="form-evento" class="card card-body" novalidate>
      <div class="mb-3">
        <label class="form-label" for="nome">Nome</label>
        <input class="form-control" id="nome" name="nome" maxlength="120" required value="${escapar(evento.nome)}">
      </div>
      <div class="mb-3">
        <label class="form-label" for="descricao">Descrição</label>
        <textarea class="form-control" id="descricao" name="descricao" maxlength="1000" rows="3">${escapar(evento.descricao)}</textarea>
      </div>
      <div class="row">
        <div class="col-md-4 mb-3">
          <label class="form-label" for="tipo">Tipo</label>
          <select class="form-select" id="tipo" name="tipo" required>
            <option value="">Escolha...</option>${opcoes(Object.entries(TIPOS), evento.tipo)}
          </select>
        </div>
        <div class="col-md-4 mb-3">
          <label class="form-label" for="dataHora">Data e hora</label>
          <input class="form-control" type="datetime-local" id="dataHora" name="dataHora" required value="${paraInput(evento.dataHora)}">
        </div>
        <div class="col-md-4 mb-3">
          <label class="form-label" for="prazoAlteracoes">Prazo para alterações</label>
          <input class="form-control" type="datetime-local" id="prazoAlteracoes" name="prazoAlteracoes" required value="${paraInput(evento.prazoAlteracoes)}">
          <div class="form-text">Deve ser antes da data do evento.</div>
        </div>
      </div>
      <div class="d-flex gap-2">
        <button class="btn btn-primary" type="submit">Salvar</button>
        <a class="btn btn-outline-secondary" href="${id ? `#/eventos/${id}` : '#/eventos'}">Voltar</a>
      </div>
    </form>`;

  container.querySelector('#form-evento').addEventListener('submit', async (e) => {
    e.preventDefault();
    const dados = Object.fromEntries(new FormData(e.target));
    try {
      const salvo = id ? await api.editarEvento(id, dados) : await api.criarEvento(dados);
      location.hash = `#/eventos/${salvo.id}`;
    } catch (erro) {
      mostrarErro(erro);
    }
  });
}
