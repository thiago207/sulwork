import { limparAlertas, mostrarErro, mostrarSucessoPendente } from './ui.js';
import { renderListaEventos } from './paginas/eventos/lista.js';
import { renderFormEvento } from './paginas/eventos/formulario.js';
import { ABAS, renderDetalheEvento } from './paginas/detalhe/detalhe.js';
import { renderParticipantes } from './paginas/participantes/participantes.js';
import { renderSugestoes } from './paginas/sugestoes/sugestoes.js';

// a ordem importa: "novo" precisa vir antes de ":id"; a aba do detalhe fica na URL (#/eventos/1/cobertura)
const rotas = [
  [/^#\/eventos\/novo$/, renderFormEvento],
  [/^#\/eventos\/(\d+)\/editar$/, renderFormEvento],
  [/^#\/eventos\/(\d+)\/levar$/, renderSugestoes],
  [new RegExp(`^#\\/eventos\\/(\\d+)(?:\\/(${ABAS.join('|')}))?$`), renderDetalheEvento],
  [/^#\/participantes$/, renderParticipantes],
  [/^#\/eventos$/, renderListaEventos],
];

function marcarMenu(hash) {
  document.querySelectorAll('[data-nav]').forEach((link) => {
    const ativo = hash.startsWith(`#/${link.dataset.nav}`);
    link.classList.toggle('active', ativo);
    if (ativo) link.setAttribute('aria-current', 'location');
    else link.removeAttribute('aria-current');
  });
}

async function navegar(container) {
  const hash = location.hash || '#/eventos';
  const rota = rotas.find(([padrao]) => padrao.test(hash));
  if (!rota) {
    location.hash = '#/eventos';
    return;
  }
  const [padrao, pagina] = rota;
  const [, id, aba] = hash.match(padrao);
  const carregando = document.getElementById('carregando');

  limparAlertas();
  marcarMenu(hash);
  container.replaceChildren();
  carregando.hidden = false;
  window.scrollTo(0, 0);
  try {
    await pagina(container, { id: id ? Number(id) : null, aba });
    mostrarSucessoPendente();
  } catch (erro) {
    container.replaceChildren();
    mostrarErro(erro);
  } finally {
    carregando.hidden = true;
  }
}

export function iniciarRoteador(container) {
  window.addEventListener('hashchange', () => navegar(container));
  navegar(container);
}
