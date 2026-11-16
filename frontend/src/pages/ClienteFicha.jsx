// Ficha do cliente (#20, RF01 e RF06): dados para editar e o histórico de atendimentos.

import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { useSessao } from '../sessao.jsx'
import { Carregando, Erro, Status, Sucesso, useAcao, useCarregar } from '../componentes.jsx'
import { dataBR, dinheiro, hora } from '../formato.js'

export default function ClienteFicha() {
  const { id } = useParams()
  const { ehAdmin } = useSessao()
  const navegar = useNavigate()
  const acao = useAcao()
  const historico = useCarregar(() => api(`/clientes/${id}/historico`), [id])
  // A API não tem GET /clientes/{id}: a ficha procura o cliente na lista
  const clientes = useCarregar(() => api('/clientes'), [])
  const cliente = (clientes.dados ?? []).find((c) => String(c.id) === id)
  const [dados, setDados] = useState(null)

  useEffect(() => {
    if (cliente) setDados({ nome: cliente.nome, telefone: cliente.telefone, email: cliente.email })
  }, [cliente])

  if (historico.erro) return <Erro erro={historico.erro} />
  if (!historico.dados || !dados) return <Carregando />
  const h = historico.dados

  async function salvar(evento) {
    evento.preventDefault()
    await acao.rodar(() => api(`/clientes/${id}`, { metodo: 'PUT', corpo: dados }), 'Dados salvos.')
    clientes.recarregar()
    historico.recarregar()
  }

  async function excluir() {
    if (!window.confirm(`Excluir ${h.nomeCliente}? Só é possível para cliente sem agendamentos.`)) return
    const ok = await acao.rodar(() => api(`/clientes/${id}`, { metodo: 'DELETE' }).then(() => true))
    if (ok) navegar('/clientes')
  }

  return (
    <>
      <div className="cabecalho"><h1>{h.nomeCliente}</h1></div>
      <Erro erro={acao.erro} />
      <Sucesso texto={acao.sucesso} />

      <div className="indicadores">
        <div className="indicador"><div className="rotulo">Atendimentos</div><div className="valor">{h.totalAtendimentos}</div></div>
        <div className="indicador"><div className="rotulo">Total gasto</div><div className="valor">{dinheiro(h.totalGasto)}</div></div>
        <div className="indicador">
          <div className="rotulo">Último atendimento</div>
          <div className="valor">{h.ultimoAtendimento ? dataBR(h.ultimoAtendimento) : '—'}</div>
        </div>
      </div>

      <form className="cartao formulario" onSubmit={salvar}>
        <label className="campo">Nome<input value={dados.nome} onChange={(e) => setDados({ ...dados, nome: e.target.value })} required /></label>
        <label className="campo">Telefone<input value={dados.telefone} onChange={(e) => setDados({ ...dados, telefone: e.target.value })} required /></label>
        <label className="campo">E-mail<input type="email" value={dados.email} onChange={(e) => setDados({ ...dados, email: e.target.value })} required /></label>
        <div className="linha">
          <button className="primario" disabled={acao.ocupado}>Salvar</button>
          {ehAdmin && <button type="button" className="perigo" disabled={acao.ocupado} onClick={excluir}>Excluir</button>}
        </div>
      </form>

      <section className="cartao">
        <h2>Histórico de atendimentos</h2>
        {h.agendas.length === 0 ? <p className="muted">Nenhum agendamento ainda.</p> : (
          <div className="tabela-rolavel">
            <table>
              <thead><tr><th>Data</th><th>Serviços</th><th>Status</th><th className="direita">Valor</th></tr></thead>
              <tbody>
                {h.agendas.map((a) => (
                  <tr key={a.id} className="clicavel" onClick={() => navegar(`/agendamentos/${a.id}`)}>
                    <td className="mono">{dataBR(a.dataHora)} {hora(a.dataHora)}</td>
                    <td>{a.itens.map((i) => `${i.nomeServico} (${i.nomeProfissional})`).join(', ') || '—'}</td>
                    <td><Status status={a.status} /></td>
                    <td className="direita">{dinheiro(a.total)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  )
}
