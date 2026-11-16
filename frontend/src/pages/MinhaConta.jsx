// Minha conta (#18): dados de quem está logado e troca da própria senha.

import { useEffect, useState } from 'react'
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

      <MeuEmail />
      <UsarComoAplicativo />

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

/**
 * E-mail para o "esqueci minha senha" (#42). Busca o valor atual em GET /auth/me e salva em PUT /auth/me.
 */
function MeuEmail() {
  const [email, setEmail] = useState('')
  const acao = useAcao()

  useEffect(() => {
    api('/auth/me').then((eu) => setEmail(eu.email ?? '')).catch(() => {})
  }, [])

  async function salvar(evento) {
    evento.preventDefault()
    const salvo = await acao.rodar(() => api('/auth/me', { metodo: 'PUT', corpo: { email } }),
      email.trim() ? 'E-mail salvo. Se esquecer a senha, o código chega nele.' : 'E-mail removido.')
    if (salvo) setEmail(salvo.email ?? '')
  }

  return (
    <form className="cartao pilha" onSubmit={salvar} style={{ maxWidth: 420 }}>
      <h2>Meu e-mail</h2>
      <p className="muted" style={{ margin: 0 }}>Usado só para mandar o código quando você esquecer a senha.</p>
      <Erro erro={acao.erro} />
      <Sucesso texto={acao.sucesso} />
      <label className="campo">E-mail<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" /></label>
      <button disabled={acao.ocupado}>Salvar e-mail</button>
    </form>
  )
}

/** Passo a passo para pôr o sistema na tela inicial do celular, como um aplicativo (#43). */
function UsarComoAplicativo() {
  return (
    <section className="cartao pilha" style={{ maxWidth: 420 }}>
      <h2>Usar como aplicativo no celular</h2>
      <p className="muted" style={{ margin: 0 }}>Abra o sistema no navegador do celular, entre com “Manter conectado” marcado e:</p>
      <ul style={{ margin: 0, paddingLeft: '1.2rem' }}>
        <li><strong>Android (Chrome):</strong> menu ⋮ → “Adicionar à tela inicial”.</li>
        <li><strong>iPhone (Safari):</strong> botão Compartilhar → “Adicionar à Tela de Início”.</li>
      </ul>
      <p className="muted" style={{ margin: 0 }}>O ícone da tesoura aparece junto dos outros aplicativos.</p>
    </section>
  )
}
