// Tela de login (#19, RNF01), "esqueci minha senha" (#42) e primeiro acesso do dono (#46).
// Entrar: POST /auth/login. Esqueci: POST /auth/esqueci-senha manda um código por e-mail, e
// POST /auth/redefinir-senha troca a senha com esse código. Sistema recém-instalado, sem nenhum
// usuário: POST /auth/primeiro-acesso cria a conta do dono (só no computador do salão).

import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { useSessao } from '../sessao.jsx'
import { Erro, Sucesso } from '../componentes.jsx'

export default function Login() {
  // 'entrar' → 'pedirCodigo' → 'redefinir' → volta para 'entrar'
  const [etapa, setEtapa] = useState('entrar')
  const [loginOuEmail, setLoginOuEmail] = useState('')
  const [aviso, setAviso] = useState(null)
  const [opcoes, setOpcoes] = useState({ recuperacaoPorEmail: false, primeiroAcesso: false })

  // A API diz o que mostrar: "Esqueci minha senha" só com o e-mail do salão configurado, e
  // "Criar a conta do dono" só enquanto não existe nenhum usuário
  useEffect(() => {
    api('/auth/opcoes').then(setOpcoes).catch(() => {})
  }, [])

  const irPara = (proxima, mensagem = null) => {
    setAviso(mensagem)
    setEtapa(proxima)
  }

  return (
    <div className="tela-login">
      <div className="cartao pilha">
        <div className="linha">
          <img src="/favicon.svg" alt="" width="40" height="40" />
          <div>
            <h1 style={{ margin: 0 }}>Tesoura</h1>
            <div className="muted">Studio WL · agenda e caixa</div>
          </div>
        </div>
        <Sucesso texto={aviso} />
        {opcoes.primeiroAcesso && <CriarDono />}
        {!opcoes.primeiroAcesso && etapa === 'entrar' && (
          <Entrar loginInicial={loginOuEmail} recuperacaoLigada={opcoes.recuperacaoPorEmail}
            onEsqueci={(login) => { setLoginOuEmail(login); irPara('pedirCodigo') }} />
        )}
        {etapa === 'pedirCodigo' && (
          <PedirCodigo loginOuEmail={loginOuEmail} setLoginOuEmail={setLoginOuEmail}
            onEnviado={() => irPara('redefinir', 'Se esse login ou e-mail tiver um e-mail cadastrado, enviamos um código. Olhe a caixa de entrada e o spam.')}
            onVoltar={() => irPara('entrar')} />
        )}
        {etapa === 'redefinir' && (
          <Redefinir loginOuEmail={loginOuEmail}
            onPronto={() => irPara('entrar', 'Senha nova criada. Entre com ela.')}
            onVoltar={() => irPara('pedirCodigo')} />
        )}
      </div>
    </div>
  )
}

function Entrar({ loginInicial, recuperacaoLigada, onEsqueci }) {
  const { entrar } = useSessao()
  const [login, setLogin] = useState(loginInicial.includes('@') ? '' : loginInicial)
  const [senha, setSenha] = useState('')
  // Marcado por padrão: no celular de cada profissional, ninguém quer digitar a senha todo dia.
  // No computador da recepção, que várias pessoas usam, dá para desmarcar.
  const [manterConectado, setManterConectado] = useState(true)
  const [erro, setErro] = useState(null)
  const [ocupado, setOcupado] = useState(false)

  async function enviar(evento) {
    evento.preventDefault() // sem isso o navegador recarregaria a página ao enviar o formulário
    setOcupado(true)
    setErro(null)
    try {
      await entrar(login, senha, manterConectado)
    } catch (e) {
      setErro(e)
      setOcupado(false)
    }
  }

  return (
    <form className="pilha" onSubmit={enviar}>
      <Erro erro={erro} />
      <label className="campo">
        Login
        <input value={login} onChange={(e) => setLogin(e.target.value)} autoComplete="username" autoCapitalize="none" autoFocus required />
      </label>
      <label className="campo">
        Senha
        <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} autoComplete="current-password" required />
      </label>
      <label className="linha" style={{ fontWeight: 600 }}>
        <input type="checkbox" checked={manterConectado} onChange={(e) => setManterConectado(e.target.checked)} style={{ minHeight: 0 }} />
        Manter conectado por 30 dias
      </label>
      <button className="primario" disabled={ocupado} style={{ width: '100%' }}>
        {ocupado ? 'Entrando…' : 'Entrar'}
      </button>
      {recuperacaoLigada ? (
        <button type="button" className="pequeno" style={{ border: 0, background: 'none', color: 'var(--accent)' }}
          onClick={() => onEsqueci(login)}>
          Esqueci minha senha
        </button>
      ) : (
        <p className="muted" style={{ margin: 0, fontSize: '.85rem', textAlign: 'center' }}>
          Esqueceu a senha? Peça ao dono do salão para criar uma nova em Acessos.
        </p>
      )}
    </form>
  )
}

function PedirCodigo({ loginOuEmail, setLoginOuEmail, onEnviado, onVoltar }) {
  const [erro, setErro] = useState(null)
  const [ocupado, setOcupado] = useState(false)

  async function enviar(evento) {
    evento.preventDefault()
    setOcupado(true)
    setErro(null)
    try {
      await api('/auth/esqueci-senha', { metodo: 'POST', corpo: { loginOuEmail } })
      onEnviado()
    } catch (e) {
      setErro(e)
      setOcupado(false)
    }
  }

  return (
    <form className="pilha" onSubmit={enviar}>
      <h2 style={{ margin: 0 }}>Esqueci minha senha</h2>
      <p className="muted" style={{ margin: 0 }}>Informe seu login ou e-mail. Vamos mandar um código de 6 números para o seu e-mail cadastrado.</p>
      <Erro erro={erro} />
      <label className="campo">
        Login ou e-mail
        <input value={loginOuEmail} onChange={(e) => setLoginOuEmail(e.target.value)} autoCapitalize="none" autoFocus required />
      </label>
      <button className="primario" disabled={ocupado}>{ocupado ? 'Enviando…' : 'Enviar código'}</button>
      <button type="button" className="pequeno" onClick={onVoltar}>Voltar para o login</button>
      <p className="muted" style={{ margin: 0, fontSize: '.85rem' }}>Sem e-mail cadastrado? Peça ao dono do salão para criar uma senha nova para você.</p>
    </form>
  )
}

function Redefinir({ loginOuEmail, onPronto, onVoltar }) {
  const [codigo, setCodigo] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [erro, setErro] = useState(null)
  const [ocupado, setOcupado] = useState(false)

  async function enviar(evento) {
    evento.preventDefault()
    setOcupado(true)
    setErro(null)
    try {
      await api('/auth/redefinir-senha', { metodo: 'POST', corpo: { loginOuEmail, codigo, novaSenha } })
      onPronto()
    } catch (e) {
      setErro(e)
      setOcupado(false)
    }
  }

  return (
    <form className="pilha" onSubmit={enviar}>
      <h2 style={{ margin: 0 }}>Criar senha nova</h2>
      <Erro erro={erro} />
      <label className="campo">
        Código recebido por e-mail
        <input value={codigo} onChange={(e) => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))}
          inputMode="numeric" autoComplete="one-time-code" placeholder="000000" autoFocus required />
      </label>
      <label className="campo">
        Senha nova
        <input type="password" minLength={6} value={novaSenha} onChange={(e) => setNovaSenha(e.target.value)} autoComplete="new-password" required />
      </label>
      <button className="primario" disabled={ocupado || codigo.length !== 6}>{ocupado ? 'Salvando…' : 'Criar senha nova'}</button>
      <button type="button" className="pequeno" onClick={onVoltar}>Pedir outro código</button>
    </form>
  )
}

// Sistema recém-instalado (#46): o dono cria a própria conta. A API só aceita se o pedido vier do
// próprio computador do salão; de outro aparelho, mostra o erro explicando.
function CriarDono() {
  const { aceitarLogin } = useSessao()
  const [dados, setDados] = useState({ nome: '', login: '', senha: '', repetida: '' })
  const [erro, setErro] = useState(null)
  const [ocupado, setOcupado] = useState(false)
  const mudar = (campo) => (e) => setDados({ ...dados, [campo]: e.target.value })

  async function enviar(evento) {
    evento.preventDefault()
    if (dados.senha !== dados.repetida) {
      setErro(new Error('As duas senhas não são iguais.'))
      return
    }
    setOcupado(true)
    setErro(null)
    try {
      const { nome, login, senha } = dados
      aceitarLogin(await api('/auth/primeiro-acesso', { metodo: 'POST', corpo: { nome, login, senha } }))
    } catch (e) {
      setErro(e)
      setOcupado(false)
    }
  }

  return (
    <form className="pilha" onSubmit={enviar}>
      <h2 style={{ margin: 0 }}>Criar a conta do dono</h2>
      <p className="muted" style={{ margin: 0 }}>
        O sistema ainda não tem nenhum acesso. Crie o seu: você será o administrador e depois cria o acesso da equipe em Acessos.
      </p>
      <Erro erro={erro} />
      <label className="campo">Seu nome<input value={dados.nome} onChange={mudar('nome')} autoFocus required /></label>
      <label className="campo">Login<input value={dados.login} onChange={mudar('login')} autoCapitalize="none" autoComplete="username" required /></label>
      <label className="campo">Senha<input type="password" minLength={6} value={dados.senha} onChange={mudar('senha')} autoComplete="new-password" required /></label>
      <label className="campo">Repita a senha<input type="password" minLength={6} value={dados.repetida} onChange={mudar('repetida')} autoComplete="new-password" required /></label>
      <button className="primario" disabled={ocupado}>{ocupado ? 'Criando…' : 'Criar conta e entrar'}</button>
    </form>
  )
}
