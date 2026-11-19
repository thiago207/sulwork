import { limparAlertas, mostrarErro } from './ui.js';
import { renderListaEventos } from './paginas/listaEventos.js';
import { renderFormEvento } from './paginas/formEvento.js';
import { renderDetalheEvento } from './paginas/detalheEvento.js';
import { renderParticipantes } from './paginas/participantes.js';
import { renderSugestoes } from './paginas/sugestoes.js';

// a ordem importa: "novo" precisa vir antes de ":id"
const rotas = [
  [/^#\/eventos\/novo$/, renderFormEvento],
  [/^#\/eventos\/(\d+)\/editar$/, renderFormEvento],
  [/^#\/eventos\/(\d+)\/levar$/, renderSugestoes],
  [/^#\/eventos\/(\d+)$/, renderDetalheEvento],
  [/^#\/participantes$/, renderParticipantes],
  [/^#\/eventos$/, renderListaEventos],
];

async function navegar(container) {
  const hash = location.hash || '#/eventos';
  const rota = rotas.find(([padrao]) => padrao.test(hash));
  if (!rota) {
    location.hash = '#/eventos';
    return;
  }
  const [padrao, pagina] = rota;
  const [, id] = hash.match(padrao);
  limparAlertas();
  container.innerHTML = '<div class="text-center p-5"><div class="spinner-border"></div></div>';
  try {
    await pagina(container, { id: id ? Number(id) : null });
  } catch (erro) {
    container.innerHTML = '';
    mostrarErro(erro);
  }
}

export function iniciarRoteador(container) {
  window.addEventListener('hashchange', () => navegar(container));
  navegar(container);
}
