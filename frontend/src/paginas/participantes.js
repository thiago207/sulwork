import { api } from '../api.js';
import { escapar, limparAlertas, mostrarErro, mostrarSucesso, tentar } from '../ui.js';
import { RESTRICOES, rotulo } from '../logica/rotulos.js';

function htmlLista(participantes) {
  if (!participantes.length) {
    return '<p class="text-muted">Nenhum participante cadastrado.</p>';
  }
  return `<ul class="list-group">${participantes.map((p) => `
    <li class="list-group-item d-flex justify-content-between align-items-start">
      <div>
        <strong>${escapar(p.nome)}</strong> <small class="text-muted">CPF ${escapar(p.cpf)}</small><br>
        ${p.restricoes.map((r) => `<span class="badge text-bg-info me-1">${escapar(rotulo(RESTRICOES, r))}</span>`).join('')}
        ${p.observacaoRestricao ? `<small class="text-muted">${escapar(p.observacaoRestricao)}</small>` : ''}
      </div>
      <button class="btn btn-sm btn-outline-danger" data-acao="excluir" data-id="${p.id}">Excluir</button>
    </li>`).join('')}</ul>`;
}

export async function renderParticipantes(container) {
  const desenhar = async () => {
    container.querySelector('#lista').innerHTML = htmlLista(await api.listarParticipantes());
  };

  container.innerHTML = `
    <h1 class="h3 mb-3">Participantes</h1>
    <div class="row g-4">
      <div class="col-lg-5">
        <form id="form-participante" class="card card-body" novalidate>
          <h2 class="h5">Novo participante</h2>
          <div class="mb-2">
            <label class="form-label" for="nome">Nome</label>
            <input class="form-control" id="nome" name="nome" maxlength="120" required>
          </div>
          <div class="mb-2">
            <label class="form-label" for="cpf">CPF</label>
            <input class="form-control" id="cpf" name="cpf" placeholder="000.000.000-00" required>
          </div>
          <fieldset class="mb-2">
            <legend class="form-label fs-6">Restrições alimentares</legend>
            ${Object.entries(RESTRICOES).map(([valor, texto]) => `
              <div class="form-check form-check-inline">
                <input class="form-check-input" type="checkbox" name="restricoes" value="${valor}" id="r-${valor}">
                <label class="form-check-label" for="r-${valor}">${escapar(texto)}</label>
              </div>`).join('')}
          </fieldset>
          <div class="mb-3">
            <label class="form-label" for="observacaoRestricao">Observação</label>
            <input class="form-control" id="observacaoRestricao" name="observacaoRestricao" maxlength="500">
          </div>
          <button class="btn btn-primary" type="submit">Cadastrar</button>
        </form>
      </div>
      <div class="col-lg-7" id="lista"></div>
    </div>`;

  container.querySelector('#form-participante').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = new FormData(e.target);
    const dados = {
      nome: form.get('nome'),
      cpf: form.get('cpf'),
      restricoes: form.getAll('restricoes'),
      observacaoRestricao: form.get('observacaoRestricao') || null,
    };
    limparAlertas();
    try {
      await api.criarParticipante(dados);
      e.target.reset();
      mostrarSucesso('Participante cadastrado.');
      await desenhar();
    } catch (erro) {
      mostrarErro(erro);
    }
  });

  container.querySelector('#lista').addEventListener('click', async (e) => {
    const botao = e.target.closest('[data-acao="excluir"]');
    if (!botao || !confirm('Excluir este participante?')) return;
    limparAlertas();
    await tentar(async () => {
      await api.excluirParticipante(botao.dataset.id);
      await desenhar();
    });
  });

  await desenhar();
}
