import modelo from './detalhe.html?raw';
import './detalhe.css';
import { Tooltip } from 'bootstrap';
import { api } from '../../api.js';
import { limparAlertas, mostrarErro, mostrarErroNoForm, sucessoNaProximaTela, tentar } from '../../ui.js';
import { campo, criar, preencher, renderizar } from '../../dom.js';
import { colocarBarra } from '../../componentes/barra.js';
import { confirmar } from '../../componentes/confirmacao.js';
import { STATUS, TIPOS, formatarDataHora, iconeDoTipo, rotulo } from '../../logica/rotulos.js';
import { aceitaContribuicoes, botaoDaTransicao, confirmacaoDaTransicao, podeEditar, transicoesVisiveis } from '../../logica/status.js';
import { formatarPercentual } from '../../logica/cobertura.js';
import { desenharItens } from './abaItens.js';
import { desenharParticipantes } from './abaParticipantes.js';
import { desenharContribuicoes } from './abaContribuicoes.js';
import { desenharCobertura } from './abaCobertura.js';
import { desenharRestricoes } from './abaRestricoes.js';

export const ABAS = ['itens', 'participantes', 'contribuicoes', 'cobertura', 'restricoes'];

function desenharCabecalho(cabecalho, evento, cobertura, totalParticipantes, agora) {
  preencher(cabecalho, {
    status: rotulo(STATUS, evento.status),
    tipo: rotulo(TIPOS, evento.tipo),
    nome: evento.nome,
    descricao: evento.descricao,
    data: formatarDataHora(evento.dataHora),
    prazo: formatarDataHora(evento.prazoAlteracoes),
    participantes: totalParticipantes,
    cobertura: formatarPercentual(cobertura.coberturaGeral),
    obrigatorios: `${cobertura.obrigatoriosCompletos} de ${cobertura.obrigatoriosTotal}`,
  });
  campo(cabecalho, 'status').dataset.status = evento.status;
  cabecalho.querySelector('#icone-tipo').classList.add(iconeDoTipo(evento.tipo));
  colocarBarra(cabecalho, cobertura.coberturaGeral);
  cabecalho.querySelectorAll('[data-bs-toggle="tooltip"]').forEach((el) => new Tooltip(el));

  const levar = cabecalho.querySelector('#levar');
  levar.href = `#/eventos/${evento.id}/levar`;
  levar.hidden = !aceitaContribuicoes(evento, agora);
  const editar = cabecalho.querySelector('#editar');
  editar.href = `#/eventos/${evento.id}/editar`;
  editar.hidden = !podeEditar(evento);
  cabecalho.querySelector('#excluir').hidden = evento.status !== 'RASCUNHO';

  const botoes = transicoesVisiveis(evento, agora).map((destino) => {
    const { texto, estilo } = botaoDaTransicao(evento.status, destino);
    const botao = criar('button', `btn btn-sm btn-${estilo}`, texto);
    botao.type = 'button';
    botao.dataset.acao = 'status';
    botao.dataset.status = destino;
    return botao;
  });
  cabecalho.querySelector('#botoes-status').replaceChildren(...botoes);
  cabecalho.querySelector('#transicoes').hidden = !botoes.length;
}

function prepararAbas(pagina, eventoId, abaAtiva, contadores, aoTrocar) {
  for (const [nome, total] of Object.entries(contadores)) {
    pagina.querySelector(`[data-contador="${nome}"]`).textContent = total;
  }
  pagina.querySelector(`[data-aba="${abaAtiva}"]`).classList.add('active');
  pagina.querySelector(`#aba-${abaAtiva}`).classList.add('show', 'active');
  pagina.querySelector('.abas').addEventListener('click', (e) => {
    const aba = e.target.closest('[data-aba]');
    if (!aba) return;
    aoTrocar(aba.dataset.aba);
    // replaceState troca a URL sem disparar o hashchange (não recarrega a página)
    history.replaceState(null, '', `#/eventos/${eventoId}/${aba.dataset.aba}`);
  });
}

export async function renderDetalheEvento(container, { id, aba }) {
  let abaAtiva = ABAS.includes(aba) ? aba : 'itens';
  const agora = new Date();
  const [evento, categorias, itens, doEvento, todos, contribuicoes, cobertura, restricoes] = await Promise.all([
    api.buscarEvento(id), api.listarCategorias(id), api.listarItens(id), api.listarParticipantesDoEvento(id),
    api.listarParticipantes(), api.listarContribuicoes(id), api.cobertura(id), api.restricoes(id),
  ]);
  const editavel = podeEditar(evento);
  const aberto = aceitaContribuicoes(evento, agora);

  // executa a ação e redesenha a página; os botões ficam travados até a resposta (evita clique duplo)
  const executar = async (acao, form) => {
    limparAlertas();
    const botoes = [...container.querySelectorAll('button:not([disabled])')];
    botoes.forEach((b) => { b.disabled = true; });
    try {
      const resultado = await acao();
      await renderDetalheEvento(container, { id, aba: abaAtiva });
      return resultado;
    } catch (erro) {
      botoes.forEach((b) => { b.disabled = false; });
      if (form) mostrarErroNoForm(form, erro);
      else mostrarErro(erro);
      return undefined;
    }
  };
  const contexto = { id, executar };

  renderizar(container, modelo);
  const cabecalho = container.querySelector('#cabecalho-evento');
  desenharCabecalho(cabecalho, evento, cobertura, doEvento.length, agora);
  prepararAbas(container, id, abaAtiva,
    { itens: itens.length, participantes: doEvento.length, contribuicoes: contribuicoes.length },
    (nova) => { abaAtiva = nova; });

  desenharItens(container.querySelector('#aba-itens'), { categorias, itens, editavel }, contexto);
  desenharParticipantes(container.querySelector('#aba-participantes'), { doEvento, todos, aberto }, contexto);
  desenharContribuicoes(container.querySelector('#aba-contribuicoes'), { contribuicoes, participantes: doEvento, cobertura, aberto }, contexto);
  desenharCobertura(container.querySelector('#aba-cobertura'), cobertura);
  desenharRestricoes(container.querySelector('#aba-restricoes'), restricoes, doEvento);

  cabecalho.addEventListener('click', async (e) => {
    const botao = e.target.closest('[data-acao]');
    if (!botao) return;
    if (botao.dataset.acao === 'status') {
      const destino = botao.dataset.status;
      if (await confirmar(confirmacaoDaTransicao(evento.status, destino))) {
        await executar(() => api.mudarStatus(id, destino));
      }
    }
    if (botao.dataset.acao === 'excluir-evento') {
      const ok = await confirmar({ titulo: 'Excluir o evento?', texto: 'Categorias e itens do rascunho também serão apagados.', botao: 'Excluir' });
      if (!ok) return;
      limparAlertas();
      await tentar(async () => {
        await api.excluirEvento(id);
        sucessoNaProximaTela('Evento excluído.');
        location.hash = '#/eventos';
      });
    }
  });
}
