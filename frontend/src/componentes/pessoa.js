import modelo from './pessoa.html?raw';
import { campo, instanciar, preencher } from '../dom.js';
import { criarChips } from './chips.js';
import { RESTRICOES, formatarCpf, iniciais } from '../logica/rotulos.js';

// botoes: lista de { acao, titulo, icone, classe? }; o botão do modelo serve de molde para cada um
export function criarPessoa(participante, botoes = []) {
  const item = instanciar(modelo);
  preencher(item, {
    iniciais: iniciais(participante.nome),
    nome: participante.nome,
    cpf: participante.cpf ? `CPF ${formatarCpf(participante.cpf)}` : '',
    observacao: participante.observacaoRestricao,
  });
  criarChips(campo(item, 'restricoes'), participante.restricoes ?? [], RESTRICOES, 'chip-restricao');

  const acoes = campo(item, 'acoes');
  const molde = acoes.querySelector('button');
  acoes.replaceChildren(...botoes.map((b) => {
    const botao = molde.cloneNode(true);
    botao.dataset.acao = b.acao;
    botao.dataset.id = participante.id;
    botao.title = b.titulo;
    botao.setAttribute('aria-label', b.titulo);
    botao.querySelector('.bi').classList.add(b.icone);
    if (b.classe) botao.classList.add(b.classe);
    return botao;
  }));
  return item;
}
