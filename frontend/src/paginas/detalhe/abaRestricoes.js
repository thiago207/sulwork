import modelo from './abaRestricoes.html?raw';
import { campo, clonar, preencher, renderizar } from '../../dom.js';
import { criarChips } from '../../componentes/chips.js';
import { ALERGENOS, RESTRICOES, iniciais, rotulo } from '../../logica/rotulos.js';

function criarResumo(aba, { restricao, participantes }) {
  return preencher(clonar(aba, 'resumo-restricao'), {
    quantidade: participantes,
    restricao: rotulo(RESTRICOES, restricao),
  });
}

function criarItem(aba, item) {
  const linha = clonar(aba, 'item-restricao');
  const afetados = item.participantesAfetados;
  preencher(linha, { nome: item.nome, afetados: `${afetados} participante(s)` });
  criarChips(campo(linha, 'alergenos'), item.alergenos, ALERGENOS, 'chip-alergeno');
  criarChips(campo(linha, 'conflitos'), item.restricoesEmConflito, RESTRICOES, 'chip-restricao');
  campo(linha, 'sem-alergenos').hidden = item.alergenos.length > 0;
  campo(linha, 'com-afetados').hidden = !afetados;
  campo(linha, 'sem-afetados').hidden = afetados > 0;
  linha.classList.toggle('linha-alerta', afetados > 0);
  return linha;
}

function criarObservacao(aba, participante) {
  return preencher(clonar(aba, 'observacao'), {
    iniciais: iniciais(participante.nome),
    nome: participante.nome,
    texto: participante.observacaoRestricao,
  });
}

export function desenharRestricoes(aba, restricoes, participantes) {
  renderizar(aba, modelo);
  const comObservacao = participantes.filter((p) => p.observacaoRestricao);
  aba.querySelector('#observacoes').replaceChildren(...comObservacao.map((p) => criarObservacao(aba, p)));
  aba.querySelector('#caixa-observacoes').hidden = !comObservacao.length;
  aba.querySelector('#restricoes-ativos').textContent = `${restricoes.participantesAtivos} ativo(s)`;
  aba.querySelector('#restricoes-resumo').replaceChildren(...restricoes.resumo.map((r) => criarResumo(aba, r)));
  aba.querySelector('#restricoes-nenhuma').hidden = restricoes.resumo.length > 0;
  aba.querySelector('#restricoes-itens').replaceChildren(...restricoes.itens.map((i) => criarItem(aba, i)));
  aba.querySelector('#caixa-restricoes-itens').hidden = !restricoes.itens.length;
}
