// Minha conta (#18): dados de quem está logado e troca da própria senha.

import { useState } from 'react'
import { api } from '../api.js'
import { useSessao } from '../sessao.jsx'
import { Erro, Sucesso, useAcao } from '../componentes.jsx'
import { rotuloPerfil } from '../formato.js'

export default function MinhaConta() {
  const { usuario } = useSessao()
  const [senhas, setSenhas] = useState({ senhaAtual: '', novaSenha: '', confirmacao: '' })
  const acao = useAcao()
  const naoConfere = senhas.confirmacao !== '' && senhas.novaSenha !== senhas.confirmacao

  async function trocar(evento) {
    evento.preventDefault()
    const { confirmacao, ...corpo } = senhas
    if (await acao.rodar(() => api('/auth/senha', { metodo: 'PUT', corpo }).then(() => true), 'Senha trocada.')) {
      setSenhas({ senhaAtual: '', novaSenha: '', confirmacao: '' })
    }
  }

  return (
    <>
      <div className="cabecalho"><h1>Minha conta</h1></div>
      <section className="cartao">
        <p><strong>{usuario.nome}</strong> · login <span className="mono">{usuario.login}</span> · {rotuloPerfil[usuario.perfil]}</p>
      </section>

      <form className="cartao pilha" onSubmit={trocar} style={{ maxWidth: 420 }}>
        <h2>Trocar senha</h2>
        <Erro erro={acao.erro} />
        <Sucesso texto={acao.sucesso} />
        <label className="campo">Senha atual<input type="password" value={senhas.senhaAtual} onChange={(e) => setSenhas({ ...senhas, senhaAtual: e.target.value })} autoComplete="current-password" required /></label>
        <label className="campo">Nova senha<input type="password" minLength={6} value={senhas.novaSenha} onChange={(e) => setSenhas({ ...senhas, novaSenha: e.target.value })} autoComplete="new-password" required /></label>
        <label className="campo">Repita a nova senha<input type="password" value={senhas.confirmacao} onChange={(e) => setSenhas({ ...senhas, confirmacao: e.target.value })} autoComplete="new-password" required /></label>
        {naoConfere && <p style={{ color: 'var(--bad)', margin: 0 }}>As senhas não são iguais.</p>}
        <button className="primario" disabled={acao.ocupado || naoConfere}>Trocar senha</button>
      </form>
    </>
  )
}
