import modelo from './abaParticipantes.html?raw';
import { api } from '../../api.js';
import { preencherSelect, renderizar } from '../../dom.js';
import { criarPessoa } from '../../componentes/pessoa.js';
import { confirmar } from '../../componentes/confirmacao.js';

const BOTAO_SAIR = { acao: 'sair', titulo: 'Tirar do evento', icone: 'bi-box-arrow-right' };

export function desenharParticipantes(aba, { doEvento, todos, aberto }, { id, executar }) {
  renderizar(aba, modelo);
  aba.querySelector('#participantes-evento').replaceChildren(...doEvento.map((p) => criarPessoa(p, aberto ? [BOTAO_SAIR] : [])));
  aba.querySelector('#caixa-participantes').hidden = !doEvento.length;
  aba.querySelector('#sem-participantes-evento').hidden = doEvento.length > 0;
  aba.querySelector('#participantes-bloqueado').hidden = aberto;

  const noEvento = new Set(doEvento.map((p) => p.id));
  const disponiveis = todos.filter((p) => !noEvento.has(p.id));
  const form = aba.querySelector('#form-entrar');
  form.hidden = !aberto || !disponiveis.length;
  aba.querySelector('#todos-no-evento').hidden = !aberto || disponiveis.length > 0;
  if (!aberto) return;

  preencherSelect(form.elements.participanteId, disponiveis.map((p) => [p.id, p.nome]));

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const participanteId = form.elements.participanteId.value;
    if (participanteId) executar(() => api.entrarNoEvento(id, participanteId));
  });

  aba.querySelector('#participantes-evento').addEventListener('click', async (e) => {
    const botao = e.target.closest('[data-acao="sair"]');
    if (!botao) return;
    const ok = await confirmar({
      titulo: 'Tirar este participante do evento?',
      texto: 'As contribuições dele neste evento deixam de valer.',
      botao: 'Tirar do evento',
    });
    if (ok) executar(() => api.sairDoEvento(id, botao.dataset.id));
  });
}
