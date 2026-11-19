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
  const remover = linha.querySelector('[data-acao]');
  remover.dataset.id = contribuicao.id;
  remover.hidden = !aberto;
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
  preencherSelect(form.elements.participanteId, participantes.map((p) => [p.id, p.nome]));
  preencherSelect(form.elements.itemId, pendentes.map((i) => [i.itemId, `${i.nome} (faltam ${i.faltante} ${rotulo(UNIDADES, i.unidade)})`]));

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const dados = new FormData(form);
    const contribuicao = {
      participanteId: dados.get('participanteId'),
      itemId: dados.get('itemId'),
      quantidade: dados.get('quantidade'),
    };
    const item = cobertura.itens.find((i) => String(i.itemId) === contribuicao.itemId);
    const erros = validarContribuicao(contribuicao, item?.faltante);
    marcarErros(form, erros);
    if (Object.keys(erros).length) return;

    const criada = await executar(() => api.criarContribuicao(id, {
      participanteId: Number(contribuicao.participanteId),
      itemId: Number(contribuicao.itemId),
      quantidade: Number(contribuicao.quantidade),
    }), form);
    if (criada) {
      mostrarSucesso('Contribuição incluída.');
      mostrarAvisos(criada.avisos);
    }
  });

  aba.querySelector('#linhas-contribuicoes').addEventListener('click', async (e) => {
    const botao = e.target.closest('[data-acao="remover-contribuicao"]');
    if (botao && await confirmar({ titulo: 'Remover esta contribuição?', botao: 'Remover' })) {
      executar(() => api.removerContribuicao(id, botao.dataset.id));
    }
  });
}
