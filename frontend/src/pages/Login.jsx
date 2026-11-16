// Tela de login (#19, RNF01). Manda login e senha para POST /auth/login e guarda o token.

import { useState } from 'react'
import { useSessao } from '../sessao.jsx'
import { Erro } from '../componentes.jsx'

export default function Login() {
  const { entrar } = useSessao()
  const [login, setLogin] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState(null)
  const [ocupado, setOcupado] = useState(false)

  async function enviar(evento) {
    evento.preventDefault() // sem isso o navegador recarregaria a página ao enviar o formulário
    setOcupado(true)
    setErro(null)
    try {
      await entrar(login, senha)
    } catch (e) {
      setErro(e)
      setOcupado(false)
    }
  }

  return (
    <div className="tela-login">
      <form className="cartao pilha" onSubmit={enviar}>
        <div className="linha">
          <img src="/favicon.svg" alt="" width="40" height="40" />
          <div>
            <h1 style={{ margin: 0 }}>Tesoura</h1>
            <div className="muted">Studio WL · agenda e caixa</div>
          </div>
        </div>
        <Erro erro={erro} />
        <label className="campo">
          Login
          <input value={login} onChange={(e) => setLogin(e.target.value)} autoComplete="username" autoFocus required />
        </label>
        <label className="campo">
          Senha
          <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} autoComplete="current-password" required />
        </label>
        <button className="primario" disabled={ocupado} style={{ width: '100%' }}>
          {ocupado ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </div>
  )
}
