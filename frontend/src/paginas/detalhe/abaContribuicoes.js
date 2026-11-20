import modelo from './abaContribuicoes.html?raw';
import { api } from '../../api.js';
import { marcarErros, mostrarAvisos, mostrarSucesso } from '../../ui.js';
import { campo, clonar, criar, preencher, preencherSelect, renderizar } from '../../dom.js';
import { UNIDADES, iniciais, rotulo } from '../../logica/rotulos.js';
import { validarContribuicao } from '../../logica/validacao.js';
import { confirmar } from '../../componentes/confirmacao.js';

function criarLinha(aba, contribuicao, aberto) {
  const linha = clonar(aba, 'contribuicao');
  preencher(linha, {
    iniciais: iniciais(contribuicao.participanteNome),
    participante: contribuicao.participanteNome,
    item: contribuicao.itemNome,
    quantidade: `${contribuicao.quantidade} ${rotulo(UNIDADES, contribuicao.unidade)}`,
  });
  campo(linha, 'avisos').replaceChildren(...contribuicao.avisos.map((a) => criar('li', 'aviso-alergeno', a)));
  linha.classList.toggle('linha-alerta', contribuicao.avisos.length > 0);
  const acoes = campo(linha, 'acoes');
  acoes.dataset.id = contribuicao.id;
  if (!aberto) acoes.replaceChildren();
  return linha;
}

export function desenharContribuicoes(aba, { contribuicoes, participantes, cobertura, aberto }, { id, executar }) {
  renderizar(aba, modelo);
  aba.querySelector('#linhas-contribuicoes').replaceChildren(...contribuicoes.map((c) => criarLinha(aba, c, aberto)));
  aba.querySelector('#caixa-contribuicoes').hidden = !contribuicoes.length;
  aba.querySelector('#sem-contribuicoes').hidden = contribuicoes.length > 0;
  aba.querySelector('#contribuicoes-bloqueado').hidden = aberto;

  const form = aba.querySelector('#form-contribuicao');
  form.hidden = !aberto;
  if (!aberto) return;

  const pendentes = cobertura.itens.filter((i) => i.faltante > 0);
  const { participanteId, itemId, quantidade } = form.elements;
  let emEdicao;
  const mudarModo = (contribuicao) => {
    emEdicao = contribuicao;
    form.reset();
    marcarErros(form, {});
    participanteId.length = 1;
    itemId.length = 1;
    participanteId.disabled = itemId.disabled = Boolean(contribuicao);
    if (contribuicao) {
      preencherSelect(participanteId, [[contribuicao.participanteId, contribuicao.participanteNome]]);
      preencherSelect(itemId, [[contribuicao.itemId, contribuicao.itemNome]]);
      participanteId.value = contribuicao.participanteId;
      itemId.value = contribuicao.itemId;
      quantidade.value = contribuicao.quantidade;
      quantidade.focus();
    } else {
      preencherSelect(participanteId, participantes.map((p) => [p.id, p.nome]));
      preencherSelect(itemId, pendentes.map((i) => [i.itemId, `${i.nome} (faltam ${i.faltante} ${rotulo(UNIDADES, i.unidade)})`]));
    }
    form.querySelector('h3').textContent = contribuicao ? 'Editar contribuição' : 'Nova contribuição';
    form.querySelector('[type="submit"]').textContent = contribuicao ? 'Salvar contribuição' : 'Incluir contribuição';
    form.querySelector('[data-cancelar]').hidden = !contribuicao;
  };
  mudarModo();
  form.querySelector('[data-cancelar]').addEventListener('click', () => mudarModo());

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const contribuicao = {
      participanteId: participanteId.value,
      itemId: itemId.value,
      quantidade: quantidade.value,
    };
    const item = cobertura.itens.find((i) => String(i.itemId) === contribuicao.itemId);
    const limite = item && item.faltante + (emEdicao?.quantidade ?? 0);
    const erros = validarContribuicao(contribuicao, limite);
    marcarErros(form, erros);
    if (Object.keys(erros).length) return;

    const edicao = emEdicao;
    const salva = await executar(() => edicao
      ? api.editarContribuicao(id, edicao.id, { quantidade: Number(contribuicao.quantidade) })
      : api.criarContribuicao(id, {
        participanteId: Number(contribuicao.participanteId),
        itemId: Number(contribuicao.itemId),
        quantidade: Number(contribuicao.quantidade),
      }), form);
    if (salva) {
      mostrarSucesso(edicao ? 'Contribuição atualizada.' : 'Contribuição incluída.');
      mostrarAvisos(salva.avisos);
    }
  });

  aba.querySelector('#linhas-contribuicoes').addEventListener('click', async (e) => {
    const botao = e.target.closest('[data-acao]');
    if (!botao) return;
    const alvo = botao.closest('[data-id]').dataset.id;
    if (botao.dataset.acao === 'editar-contribuicao') {
      mudarModo(contribuicoes.find((c) => String(c.id) === alvo));
    } else if (await confirmar({ titulo: 'Remover esta contribuição?', botao: 'Remover' })) {
      executar(() => api.removerContribuicao(id, alvo));
    }
  });
}
