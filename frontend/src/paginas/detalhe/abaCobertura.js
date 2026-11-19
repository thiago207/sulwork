import modelo from './abaCobertura.html?raw';
import { campo, clonar, preencher, renderizar } from '../../dom.js';
import { colocarBarra, criarBarra } from '../../componentes/barra.js';
import { UNIDADES, rotulo } from '../../logica/rotulos.js';
import { formatarPercentual } from '../../logica/cobertura.js';

function criarCategoria(aba, categoria) {
  const bloco = clonar(aba, 'categoria-cobertura');
  preencher(bloco, {
    nome: categoria.nome,
    valores: `${categoria.contribuido} de ${categoria.meta} ${rotulo(UNIDADES, categoria.unidade)}`,
  });
  colocarBarra(bloco, categoria.percentual);
  return bloco;
}

function criarItem(aba, item) {
  const linha = clonar(aba, 'item-cobertura');
  const completo = item.faltante <= 0;
  preencher(linha, {
    nome: item.nome,
    valores: `${item.contribuido} de ${item.necessario} ${rotulo(UNIDADES, item.unidade)}`,
    situacao: completo ? 'completo' : `faltam ${item.faltante}`,
  });
  campo(linha, 'obrigatorio').hidden = !item.obrigatorio;
  campo(linha, 'situacao').classList.add(completo ? 'text-success' : 'text-secondary');
  colocarBarra(linha, item.percentual);
  return linha;
}

export function desenharCobertura(aba, cobertura) {
  renderizar(aba, modelo);
  aba.querySelector('#cobertura-numero').textContent = formatarPercentual(cobertura.coberturaGeral);
  aba.querySelector('#cobertura-barra').replaceChildren(criarBarra(cobertura.coberturaGeral));
  aba.querySelector('#cobertura-obrigatorios').textContent = `${cobertura.obrigatoriosCompletos} de ${cobertura.obrigatoriosTotal}`;

  aba.querySelector('#cobertura-categorias').replaceChildren(...cobertura.categorias.map((c) => criarCategoria(aba, c)));
  aba.querySelector('#cobertura-sem-categorias').hidden = cobertura.categorias.length > 0;
  aba.querySelector('#cobertura-itens').replaceChildren(...cobertura.itens.map((i) => criarItem(aba, i)));
}
