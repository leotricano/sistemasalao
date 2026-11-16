// Conversa com a API do Spring (#19).
//
// Toda tela chama api('/rota', { metodo, corpo, params }). Esta função:
//  - manda o token do login no cabeçalho Authorization;
//  - converte o corpo para JSON e a resposta de volta para objeto;
//  - transforma erro da API em ErroApi, com a mensagem em português que o TratadorDeErros montou;
//  - avisa o App quando o token venceu (401), para voltar à tela de login.

const CHAVE_SESSAO = 'tesoura.sessao'

/** Erro vindo da API: status HTTP + mensagem para mostrar na tela. */
export class ErroApi extends Error {
  constructor(mensagem, status) {
    super(mensagem)
    this.status = status
  }
}

/** Sessão guardada no navegador: { token, expiraEm, usuario }. Some se o token tiver vencido. */
export function lerSessao() {
  try {
    const sessao = JSON.parse(localStorage.getItem(CHAVE_SESSAO))
    if (sessao && new Date(sessao.expiraEm) > new Date()) return sessao
  } catch {
    // localStorage bloqueado ou conteúdo inválido: segue sem sessão
  }
  return null
}

export function salvarSessao(sessao) {
  try {
    if (sessao) localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao))
    else localStorage.removeItem(CHAVE_SESSAO)
  } catch {
    // sem localStorage o login vale só enquanto a aba estiver aberta
  }
}

let aoPerderSessao = () => {}

/** O App registra aqui o que fazer quando a API responder 401 (token vencido ou inválido). */
export function quandoPerderSessao(funcao) {
  aoPerderSessao = funcao
}

export async function api(caminho, { metodo = 'GET', corpo, params } = {}) {
  const url = new URL(caminho, window.location.origin)
  // Parâmetros vazios ficam de fora: na API, parâmetro ausente = filtro desligado
  Object.entries(params ?? {}).forEach(([nome, valor]) => {
    if (valor !== undefined && valor !== null && valor !== '') url.searchParams.set(nome, valor)
  })

  const cabecalhos = {}
  if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json'
  const sessao = lerSessao()
  if (sessao) cabecalhos.Authorization = `Bearer ${sessao.token}`

  let resposta
  try {
    resposta = await fetch(url, {
      method: metodo,
      headers: cabecalhos,
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    })
  } catch {
    throw new ErroApi('Não foi possível falar com o servidor. Confira se o sistema está ligado.', 0)
  }

  if (resposta.status === 401 && caminho !== '/auth/login') aoPerderSessao()
  if (resposta.status === 204) return null

  const texto = await resposta.text()
  let dados = null
  try {
    dados = texto ? JSON.parse(texto) : null
  } catch {
    dados = null
  }

  if (!resposta.ok) {
    throw new ErroApi(dados?.mensagem ?? `Erro ${resposta.status}`, resposta.status)
  }
  return dados
}
