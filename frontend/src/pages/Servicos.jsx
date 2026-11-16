// Serviços do salão (#20, RF02). A recepção consulta; só o dono cadastra, edita e remove.

import { useState } from 'react'
import { api } from '../api.js'
import { useSessao } from '../sessao.jsx'
import { Carregando, Erro, Sucesso, useAcao, useCarregar } from '../componentes.jsx'
import { dinheiro, minutosEmTexto } from '../formato.js'

const vazio = { nome: '', duracaoMinutos: 60, preco: '' }

export default function Servicos() {
  const { ehAdmin } = useSessao()
  const servicos = useCarregar(() => api('/servicos'), [])
  const [novo, setNovo] = useState(vazio)
  const [editando, setEditando] = useState(null) // { id, nome, duracaoMinutos, preco }
  const acao = useAcao()

  const lista = [...(servicos.dados ?? [])].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))

  async function executar(funcao, mensagem) {
    const ok = await acao.rodar(funcao, mensagem)
    servicos.recarregar()
    return ok
  }

  async function cadastrar(evento) {
    evento.preventDefault()
    if (await executar(() => api('/servicos', { metodo: 'POST', corpo: novo }), `${novo.nome} cadastrado.`)) setNovo(vazio)
  }

  async function salvarEdicao(evento) {
    evento.preventDefault()
    const { id, ...corpo } = editando
    await executar(() => api(`/servicos/${id}`, { metodo: 'PUT', corpo }), 'Serviço atualizado.')
    setEditando(null)
  }

  return (
    <>
      <div className="cabecalho"><h1>Serviços</h1></div>

      {ehAdmin && (
        <form className="cartao formulario" onSubmit={cadastrar}>
          <label className="campo">Nome<input value={novo.nome} onChange={(e) => setNovo({ ...novo, nome: e.target.value })} required /></label>
          <label className="campo">Duração (min)<input type="number" min="5" step="5" value={novo.duracaoMinutos} onChange={(e) => setNovo({ ...novo, duracaoMinutos: e.target.value })} required /></label>
          <label className="campo">Preço (R$)<input type="number" min="0" step="0.01" value={novo.preco} onChange={(e) => setNovo({ ...novo, preco: e.target.value })} required /></label>
          <button className="primario" disabled={acao.ocupado}>Cadastrar serviço</button>
        </form>
      )}
      <Erro erro={acao.erro ?? servicos.erro} />
      <Sucesso texto={acao.sucesso} />

      <section className="cartao tabela-rolavel">
        {servicos.carregando && !servicos.dados ? <Carregando /> : (
          <table>
            <thead><tr><th>Serviço</th><th>Duração</th><th className="direita">Preço</th>{ehAdmin && <th />}</tr></thead>
            <tbody>
              {lista.map((s) => editando?.id === s.id ? (
                <tr key={s.id}>
                  <td colSpan={ehAdmin ? 4 : 3}>
                    <form className="formulario" onSubmit={salvarEdicao}>
                      <input value={editando.nome} onChange={(e) => setEditando({ ...editando, nome: e.target.value })} aria-label="Nome" required />
                      <input type="number" min="5" step="5" value={editando.duracaoMinutos} onChange={(e) => setEditando({ ...editando, duracaoMinutos: e.target.value })} aria-label="Duração" required />
                      <input type="number" min="0" step="0.01" value={editando.preco} onChange={(e) => setEditando({ ...editando, preco: e.target.value })} aria-label="Preço" required />
                      <div className="linha">
                        <button className="primario pequeno" disabled={acao.ocupado}>Salvar</button>
                        <button type="button" className="pequeno" onClick={() => setEditando(null)}>Voltar</button>
                      </div>
                    </form>
                  </td>
                </tr>
              ) : (
                <tr key={s.id}>
                  <td><strong>{s.nome}</strong></td>
                  <td>{minutosEmTexto(s.duracaoMinutos)}</td>
                  <td className="direita">{dinheiro(s.preco)}</td>
                  {ehAdmin && (
                    <td className="direita">
                      <div className="linha fim">
                        <button className="pequeno" onClick={() => setEditando({ ...s })}>Editar</button>
                        <button className="pequeno perigo" disabled={acao.ocupado}
                          onClick={() => window.confirm(`Remover ${s.nome}?`) && executar(() => api(`/servicos/${s.id}`, { metodo: 'DELETE' }), 'Serviço removido.')}>
                          Remover
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {servicos.dados && lista.length === 0 && <p className="vazio">Nenhum serviço cadastrado.</p>}
      </section>
    </>
  )
}
