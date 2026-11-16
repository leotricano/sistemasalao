// Quem está logado, disponível para qualquer tela (#19).
//
// Um "contexto" do React é uma variável que todas as telas dentro do <SessaoProvider> conseguem
// ler com useSessao(), sem passar de componente em componente.

import { createContext, useContext, useEffect, useState } from 'react'
import { api, lerSessao, quandoPerderSessao, salvarSessao } from './api.js'

const SessaoContexto = createContext(null)

export function SessaoProvider({ children }) {
  const [sessao, setSessao] = useState(lerSessao)

  // Se a API disser 401 em qualquer tela, a sessão acabou: volta ao login
  useEffect(() => {
    quandoPerderSessao(() => {
      salvarSessao(null)
      setSessao(null)
    })
  }, [])

  async function entrar(login, senha) {
    const resposta = await api('/auth/login', { metodo: 'POST', corpo: { login, senha } })
    salvarSessao(resposta)
    setSessao(resposta)
  }

  function sair() {
    salvarSessao(null)
    setSessao(null)
  }

  const usuario = sessao?.usuario ?? null
  const perfil = usuario?.perfil
  const valor = {
    usuario,
    entrar,
    sair,
    ehAdmin: perfil === 'ADMIN',
    ehProfissional: perfil === 'PROFISSIONAL',
    // Recepção e dono operam o balcão: agendar, receber, cadastrar clientes
    podeOperar: perfil === 'ADMIN' || perfil === 'RECEPCAO',
  }
  return <SessaoContexto.Provider value={valor}>{children}</SessaoContexto.Provider>
}

export const useSessao = () => useContext(SessaoContexto)
