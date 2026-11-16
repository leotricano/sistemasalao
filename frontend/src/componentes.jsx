// Pedaços de tela usados em várias páginas (#19).

import { useCallback, useEffect, useState } from 'react'
import { rotuloStatus } from './formato.js'

/** Mostra a mensagem de erro da API, ou nada se não houver erro. */
export function Erro({ erro }) {
  if (!erro) return null
  return <div className="aviso erro" role="alert">{erro.message ?? String(erro)}</div>
}

export function Sucesso({ texto }) {
  if (!texto) return null
  return <div className="aviso ok" role="status">{texto}</div>
}

export function Carregando() {
  return <p className="muted">Carregando…</p>
}

export function Status({ status }) {
  return <span className={`etiqueta ${status}`}>{rotuloStatus[status] ?? status}</span>
}

/**
 * Busca dados da API ao abrir a tela e sempre que as dependências mudarem.
 * Devolve { dados, erro, carregando, recarregar }.
 * Ex.: const { dados } = useCarregar(() => api('/clientes'), [])
 */
export function useCarregar(buscar, dependencias) {
  const [estado, setEstado] = useState({ dados: null, erro: null, carregando: true })
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const executar = useCallback(buscar, dependencias)

  const recarregar = useCallback(() => {
    setEstado((atual) => ({ ...atual, carregando: true }))
    return executar()
      .then((dados) => setEstado({ dados, erro: null, carregando: false }))
      .catch((erro) => setEstado({ dados: null, erro, carregando: false }))
  }, [executar])

  useEffect(() => {
    recarregar()
  }, [recarregar])

  return { ...estado, recarregar }
}

/**
 * Envolve uma ação (salvar, excluir...) cuidando do "aguarde", do erro e da mensagem de sucesso.
 * Ex.: const acao = useAcao(); acao.rodar(() => api(...), 'Salvo!')
 */
export function useAcao() {
  const [estado, setEstado] = useState({ ocupado: false, erro: null, sucesso: null })

  async function rodar(funcao, mensagemSucesso) {
    setEstado({ ocupado: true, erro: null, sucesso: null })
    try {
      const resultado = await funcao()
      setEstado({ ocupado: false, erro: null, sucesso: mensagemSucesso ?? null })
      return resultado
    } catch (erro) {
      setEstado({ ocupado: false, erro, sucesso: null })
      return undefined
    }
  }

  return { ...estado, rodar, limpar: () => setEstado({ ocupado: false, erro: null, sucesso: null }) }
}
