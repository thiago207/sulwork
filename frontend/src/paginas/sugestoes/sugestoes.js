import modelo from './sugestoes.html?raw';
import './sugestoes.css';
import { api } from '../../api.js';
import { limparAlertas, marcarErros, mostrarAvisos, mostrarErro, mostrarErroNoForm, mostrarSucesso } from '../../ui.js';
import { campo, clonar, preencher, preencherSelect, renderizar } from '../../dom.js';
import { colocarBarra } from '../../componentes/barra.js';
import { RESTRICOES, UNIDADES, rotulo } from '../../logica/rotulos.js';
import { aceitaContribuicoes } from '../../logica/status.js';
import { validarContribuicao } from '../../logica/validacao.js';

function criarCartao(pagina, s, podeLevar) {
  const cartao = clonar(pagina, 'sugestao');
  const { participantes, comVoce, restricoes } = s.conflitos;
  preencher(cartao, {
    categoria: s.categoria,
    item: s.item,
    faltante: `${s.faltante} ${rotulo(UNIDADES, s.unidade)}`,
    sua: s.suaContribuicao ? `Você já leva ${s.suaContribuicao}.` : '',
    'conflito-texto': `Afeta ${participantes} participante(s): ${restricoes.map((r) => rotulo(RESTRICOES, r)).join(', ')}.`,
    unidade: rotulo(UNIDADES, s.unidade),
  });
  cartao.querySelector('.sugestao').classList.toggle('obrigatoria', s.obrigatorio);
  campo(cartao, 'obrigatorio').hidden = !s.obrigatorio;
  campo(cartao, 'conflito').hidden = participantes === 0;
  campo(cartao, 'conflito').classList.toggle('com-voce', comVoce);
  campo(cartao, 'conflito-voce').hidden = !comVoce;
  colocarBarra(cartao, s.cobertura);

  const form = cartao.querySelector('form');
  form.hidden = !podeLevar;
  form.dataset.itemId = s.itemId;
  form.dataset.faltante = s.faltante;
  form.dataset.item = s.item;
  form.elements.quantidade.max = s.faltante;
  form.elements.quantidade.value = s.faltante;
  return cartao;
}

function desenhar(pagina, resposta, mensagemVazia, podeLevar) {
  const sugestoes = resposta?.sugestoes ?? [];
  const lista = pagina.querySelector('#sugestoes');
  lista.replaceChildren(...sugestoes.map((s) => criarCartao(pagina, s, podeLevar)));
  lista.hidden = !sugestoes.length;
  pagina.querySelector('#sem-sugestoes').hidden = sugestoes.length > 0;
  preencher(pagina.querySelector('#sem-sugestoes'), { vazio: mensagemVazia });

  const ocultos = resposta?.ocultosPorConflito ?? 0;
  pagina.querySelector('#ocultos').hidden = !ocultos;
  preencher(pagina.querySelector('#ocultos'), {
    ocultos: `${ocultos} item(ns) oculto(s) por conflitar com todos os participantes que têm restrição.`,
  });
}

export async function renderSugestoes(container, { id }) {
  const [evento, participantes] = await Promise.all([api.buscarEvento(id), api.listarParticipantesDoEvento(id)]);
  const podeLevar = aceitaContribuicoes(evento, new Date());
  renderizar(container, modelo);

  container.querySelector('#voltar').href = `#/eventos/${id}`;
  container.querySelector('#nome-evento').textContent = evento.nome;
  const quem = container.querySelector('#quem');
  preencherSelect(quem, participantes.map((p) => [p.id, p.nome]));

  const inicial = participantes.length ? 'Escolha seu nome para ver as sugestões.' : 'Ninguém participa deste evento ainda.';
  desenhar(container, null, inicial, podeLevar);

  const atualizar = async () => {
    if (!quem.value) {
      desenhar(container, null, inicial, podeLevar);
      return;
    }
    desenhar(container, await api.sugestoes(id, quem.value), 'Nada pendente para sugerir.', podeLevar);
  };

  quem.addEventListener('change', async () => {
    limparAlertas();
    try {
      await atualizar();
    } catch (erro) {
      mostrarErro(erro);
    }
  });

  // "Vou levar" cria a contribuição direto daqui, sem voltar para a aba Contribuições
  container.querySelector('#sugestoes').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;
    const contribuicao = { participanteId: quem.value, itemId: form.dataset.itemId, quantidade: form.elements.quantidade.value };
    const erros = validarContribuicao(contribuicao, Number(form.dataset.faltante));
    marcarErros(form, erros);
    if (Object.keys(erros).length) return;

    limparAlertas();
    form.querySelector('button').disabled = true;
    try {
      const criada = await api.criarContribuicao(id, {
        participanteId: Number(contribuicao.participanteId),
        itemId: Number(contribuicao.itemId),
        quantidade: Number(contribuicao.quantidade),
      });
      mostrarSucesso(`Combinado: você leva ${criada.quantidade} ${rotulo(UNIDADES, criada.unidade)} de ${form.dataset.item}.`);
      mostrarAvisos(criada.avisos);
      await atualizar();
    } catch (erro) {
      form.querySelector('button').disabled = false;
      mostrarErroNoForm(form, erro);
    }
  });
}
