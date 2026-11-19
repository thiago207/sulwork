import { api } from '../api.js';
import { escapar, limparAlertas, mostrarErro, opcoes } from '../ui.js';
import { RESTRICOES, UNIDADES, rotulo } from '../logica/rotulos.js';
import { htmlBarra } from './detalheEvento.js';

export function htmlSugestoes(resposta) {
  const cartoes = resposta.sugestoes.map((s) => {
    const conflito = s.conflitos.participantes > 0
      ? `<div class="alert ${s.conflitos.comVoce ? 'alert-danger' : 'alert-warning'} py-2 mb-0 mt-2 small">
           ⚠ ${s.conflitos.comVoce ? '<strong>Conflita com a sua restrição.</strong> ' : ''}
           Afeta ${s.conflitos.participantes} participante(s):
           ${s.conflitos.restricoes.map((r) => escapar(rotulo(RESTRICOES, r))).join(', ')}
         </div>`
      : '';
    return `
      <div class="col-md-6 col-lg-4">
        <div class="card h-100 ${s.obrigatorio ? 'border-dark' : ''}">
          <div class="card-body">
            <h3 class="h5 card-title">${escapar(s.item)}
              ${s.obrigatorio ? '<span class="badge text-bg-dark align-middle fs-6">obrigatório</span>' : ''}
            </h3>
            <p class="card-subtitle text-muted mb-2">${escapar(s.categoria)}</p>
            <p class="mb-2">Faltam <strong>${s.faltante} ${escapar(rotulo(UNIDADES, s.unidade))}</strong></p>
            ${htmlBarra(s.cobertura)}
            ${s.suaContribuicao ? `<p class="small text-muted mt-2 mb-0">Você já leva ${s.suaContribuicao}.</p>` : ''}
            ${conflito}
          </div>
        </div>
      </div>`;
  }).join('');
  const ocultos = resposta.ocultosPorConflito
    ? `<p class="text-muted">${resposta.ocultosPorConflito} item(ns) oculto(s) por conflitar com todos os participantes que têm restrição.</p>`
    : '';
  return `${ocultos}${cartoes ? `<div class="row g-3">${cartoes}</div>` : '<p class="text-muted">Nada pendente para sugerir.</p>'}`;
}

export async function renderSugestoes(container, { id }) {
  const [evento, participantes] = await Promise.all([api.buscarEvento(id), api.listarParticipantesDoEvento(id)]);

  container.innerHTML = `
    <a href="#/eventos/${id}" class="small">← ${escapar(evento.nome)}</a>
    <h1 class="h3 my-3">O que eu posso levar?</h1>
    <select class="form-select mb-4 w-auto" id="quem" aria-label="Participante">
      <option value="">Quem é você?</option>${opcoes(participantes.map((p) => [p.id, p.nome]))}
    </select>
    <div id="resultado"></div>`;

  container.querySelector('#quem').addEventListener('change', async (e) => {
    const resultado = container.querySelector('#resultado');
    limparAlertas();
    resultado.innerHTML = '';
    if (!e.target.value) return;
    try {
      resultado.innerHTML = htmlSugestoes(await api.sugestoes(id, e.target.value));
    } catch (erro) {
      mostrarErro(erro);
    }
  });
}
