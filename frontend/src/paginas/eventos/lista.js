import modelo from './lista.html?raw';
import './lista.css';
import { api } from '../../api.js';
import { tentar } from '../../ui.js';
import { campo, clonar, preencher, preencherSelect, renderizar } from '../../dom.js';
import { STATUS, TIPOS, iconeDoTipo, partesDaData, rotulo } from '../../logica/rotulos.js';

function criarLinha(pagina, evento) {
  const linha = clonar(pagina, 'evento');
  const { dia, mes, semana, hora } = partesDaData(evento.dataHora);
  preencher(linha, {
    dia,
    mes,
    ano: evento.dataHora.slice(0, 4),
    nome: evento.nome,
    status: rotulo(STATUS, evento.status),
    tipo: rotulo(TIPOS, evento.tipo),
    quando: `${semana}, ${hora}`,
  });
  linha.querySelector('a').href = `#/eventos/${evento.id}`;
  linha.querySelector('time').dateTime = evento.dataHora;
  campo(linha, 'status').dataset.status = evento.status;
  linha.querySelector('[data-icone-tipo]').classList.add(iconeDoTipo(evento.tipo));
  return linha;
}

function desenharEventos(pagina, eventos) {
  const lista = pagina.querySelector('#lista-eventos');
  lista.replaceChildren(...eventos.map((e) => criarLinha(pagina, e)));
  lista.hidden = !eventos.length;
  pagina.querySelector('#colunas-eventos').hidden = !eventos.length;
  pagina.querySelector('#sem-eventos').hidden = eventos.length > 0;
}

export async function renderListaEventos(container) {
  const eventos = await api.listarEventos('');
  renderizar(container, modelo);

  const filtro = container.querySelector('#filtro-status');
  preencherSelect(filtro, Object.entries(STATUS));
  filtro.addEventListener('change', () => tentar(async () => {
    desenharEventos(container, await api.listarEventos(filtro.value));
  }));

  desenharEventos(container, eventos);
}
