// Lista e cadastro de clientes (#20, RF01).

import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api.js'
import { Carregando, Erro, Sucesso, useAcao, useCarregar } from '../componentes.jsx'

const vazio = { nome: '', telefone: '', email: '' }

export default function Clientes() {
  const navegar = useNavigate()
  const clientes = useCarregar(() => api('/clientes'), [])
  const [busca, setBusca] = useState('')
  const [novo, setNovo] = useState(vazio)
  const acao = useAcao()

  const termo = busca.trim().toLowerCase()
  const lista = (clientes.dados ?? [])
    .filter((c) => !termo || c.nome.toLowerCase().includes(termo) || (c.telefone ?? '').includes(termo) || (c.email ?? '').toLowerCase().includes(termo))
    .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))

  async function cadastrar(evento) {
    evento.preventDefault()
    const criado = await acao.rodar(() => api('/clientes', { metodo: 'POST', corpo: novo }), `${novo.nome} cadastrado(a).`)
    if (criado) {
      setNovo(vazio)
      clientes.recarregar()
    }
  }

  return (
    <>
      <div className="cabecalho"><h1>Clientes</h1></div>

      <form className="cartao formulario" onSubmit={cadastrar}>
        <label className="campo">Nome<input value={novo.nome} onChange={(e) => setNovo({ ...novo, nome: e.target.value })} required /></label>
        <label className="campo">Telefone<input value={novo.telefone} onChange={(e) => setNovo({ ...novo, telefone: e.target.value })} required /></label>
        <label className="campo">E-mail<input type="email" value={novo.email} onChange={(e) => setNovo({ ...novo, email: e.target.value })} required /></label>
        <button className="primario" disabled={acao.ocupado}>Cadastrar cliente</button>
      </form>
      <Erro erro={acao.erro} />
      <Sucesso texto={acao.sucesso} />

      <section className="cartao">
        <input placeholder="Buscar por nome, telefone ou e-mail" value={busca} onChange={(e) => setBusca(e.target.value)}
          aria-label="Buscar cliente" style={{ width: '100%', marginBottom: '.5rem' }} />
        <Erro erro={clientes.erro} />
        {clientes.carregando && !clientes.dados ? <Carregando /> : (
          <div className="tabela-rolavel">
            <table>
              <thead><tr><th>Nome</th><th>Telefone</th><th>E-mail</th></tr></thead>
              <tbody>
                {lista.map((c) => (
                  <tr key={c.id} className="clicavel" onClick={() => navegar(`/clientes/${c.id}`)}>
                    <td><strong>{c.nome}</strong></td><td>{c.telefone}</td><td className="muted">{c.email}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {lista.length === 0 && <p className="vazio">Nenhum cliente encontrado.</p>}
          </div>
        )}
      </section>
    </>
  )
}
