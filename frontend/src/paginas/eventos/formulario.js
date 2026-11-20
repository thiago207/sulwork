import modelo from './formulario.html?raw';
import { api } from '../../api.js';
import { limparAlertas, mostrarErroNoForm, sucessoNaProximaTela } from '../../ui.js';
import { preencherSelect, renderizar } from '../../dom.js';
import { TIPOS } from '../../logica/rotulos.js';
import { dataLocalRecife } from '../../logica/datas.js';

// a API manda "2026-12-18T08:00:00"; o input datetime-local quer "2026-12-18T08:00"
const paraInput = (iso) => (iso ? iso.slice(0, 16) : '');

export async function renderFormEvento(container, { id }) {
  const evento = id ? await api.buscarEvento(id) : {};
  renderizar(container, modelo);

  const destinoVoltar = id ? `#/eventos/${id}` : '#/eventos';
  container.querySelector('#titulo').textContent = id ? 'Editar evento' : 'Novo evento';
  container.querySelector('#voltar').href = destinoVoltar;
  container.querySelector('#cancelar').href = destinoVoltar;

  const form = container.querySelector('#form-evento');
  preencherSelect(form.elements.tipo, Object.entries(TIPOS));
  form.elements.nome.value = evento.nome ?? '';
  form.elements.descricao.value = evento.descricao ?? '';
  form.elements.tipo.value = evento.tipo ?? '';
  form.elements.dataHora.value = paraInput(evento.dataHora);
  form.elements.prazoAlteracoes.value = paraInput(evento.prazoAlteracoes);
  if (!id) {
    const agora = paraInput(dataLocalRecife(new Date()));
    form.elements.dataHora.min = agora;
    form.elements.prazoAlteracoes.min = agora;
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    limparAlertas();
    const dados = Object.fromEntries(new FormData(form));
    for (const campo of ['dataHora', 'prazoAlteracoes']) {
      if (id && dados[campo] === paraInput(evento[campo])) dados[campo] = evento[campo];
    }
    try {
      const salvo = id ? await api.editarEvento(id, dados) : await api.criarEvento(dados);
      sucessoNaProximaTela(id ? 'Evento atualizado.' : 'Evento criado. Agora cadastre as categorias e os itens.');
      location.hash = `#/eventos/${salvo.id}`;
    } catch (erro) {
      mostrarErroNoForm(form, erro);
    }
  });
}
