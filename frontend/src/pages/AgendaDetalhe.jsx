// Detalhe de um atendimento (#19, #21): serviços, pagamentos (com troco), concluir, cancelar,
// reativar e remarcar (RF03, RF05, RF08). O profissional só consulta.

import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { useSessao } from '../sessao.jsx'
import { Carregando, Erro, Status, Sucesso, useAcao, useCarregar } from '../componentes.jsx'
import { dataBR, dinheiro, hora, minutosEmTexto, rotuloForma } from '../formato.js'

export default function AgendaDetalhe() {
  const { id } = useParams()
  const { podeOperar } = useSessao()
  const navegar = useNavigate()
  const acao = useAcao()

  const agenda = useCarregar(() => api(`/agendas/${id}`), [id])
  const pagamentos = useCarregar(() => (podeOperar ? api(`/agendas/${id}/pagamentos`) : Promise.resolve([])), [id, podeOperar])

  if (agenda.erro) return <Erro erro={agenda.erro} />
  if (!agenda.dados) return <Carregando />

  const a = agenda.dados
  const pago = (pagamentos.dados ?? []).reduce((soma, p) => soma + Number(p.valor), 0)
  const falta = Math.max(0, Number(a.total) - pago)
  const agendado = a.status === 'AGENDADO'

  async function executar(funcao, mensagem) {
    const ok = await acao.rodar(funcao, mensagem)
    agenda.recarregar()
    pagamentos.recarregar()
    return ok
  }

  const mudarStatus = (status, mensagem) =>
    executar(() => api(`/agendas/${id}/status`, { metodo: 'PUT', corpo: status }), mensagem)

  async function excluir() {
    if (!window.confirm('Excluir este agendamento? Use só para lançamento errado; para desmarcar, cancele.')) return
    const ok = await acao.rodar(() => api(`/agendas/${id}`, { metodo: 'DELETE' }).then(() => true))
    if (ok) navegar('/')
  }

  return (
    <>
      <div className="cabecalho">
        <div>
          <h1>{a.nomeCliente}</h1>
          <div className="muted">
            {dataBR(a.dataHora)} · {hora(a.dataHora)} às {hora(a.dataHoraFim)} · {minutosEmTexto(a.duracaoTotalMinutos)}
          </div>
        </div>
        <Status status={a.status} />
      </div>

      <Erro erro={acao.erro} />
      <Sucesso texto={acao.sucesso} />

      <div className="grade">
        <section className="cartao">
          <h2>Serviços</h2>
          {a.itens.length === 0 && <p className="muted">Nenhum serviço ainda.</p>}
          <table>
            <tbody>
              {a.itens.map((item) => (
                <tr key={item.id}>
                  <td>
                    <strong>{item.nomeServico}</strong>
                    <div className="muted">{item.nomeProfissional} · {minutosEmTexto(item.duracaoMinutos)}</div>
                  </td>
                  <td className="direita">{dinheiro(item.precoCobrado)}</td>
                  {podeOperar && agendado && (
                    <td className="direita">
                      <button className="pequeno perigo" disabled={acao.ocupado}
                        onClick={() => executar(() => api(`/agendas/${id}/itens/${item.id}`, { metodo: 'DELETE' }), 'Serviço removido.')}>
                        Remover
                      </button>
                    </td>
                  )}
                </tr>
              ))}
              <tr>
                <td><strong>Total</strong></td>
                <td className="direita"><strong>{dinheiro(a.total)}</strong></td>
                {podeOperar && agendado && <td />}
              </tr>
            </tbody>
          </table>
          {podeOperar && agendado && <AdicionarServico agendaId={id} executar={executar} ocupado={acao.ocupado} />}
        </section>

        {podeOperar && (
          <section className="cartao">
            <h2>Pagamento</h2>
            <p>
              Pago: <strong>{dinheiro(pago)}</strong> · Falta: <strong>{dinheiro(falta)}</strong>
            </p>
            {(pagamentos.dados ?? []).map((p) => (
              <div key={p.id} className="linha" style={{ justifyContent: 'space-between', padding: '.3rem 0' }}>
                <span>{rotuloForma[p.formaPagamento]} · {dataBR(p.dataHora)} {hora(p.dataHora)}</span>
                <span className="linha">
                  <strong>{dinheiro(p.valor)}</strong>
                  <button className="pequeno perigo" disabled={acao.ocupado}
                    onClick={() => window.confirm('Remover este pagamento?') &&
                      executar(() => api(`/agendas/${id}/pagamentos/${p.id}`, { metodo: 'DELETE' }), 'Pagamento removido.')}>
                    Remover
                  </button>
                </span>
              </div>
            ))}
            {a.status !== 'CANCELADO' && falta > 0 && (
              <RegistrarPagamento key={falta} agendaId={id} falta={falta} executar={executar} ocupado={acao.ocupado} />
            )}
            {falta === 0 && Number(a.total) > 0 && <p className="muted">Atendimento quitado.</p>}
          </section>
        )}
      </div>

      {/* Atendimento concluído é histórico: não há mais ações sobre ele */}
      {podeOperar && a.status !== 'CONCLUIDO' && (
        <section className="cartao">
          <h2>Ações</h2>
          <div className="linha">
            {agendado && (
              <button className="primario" disabled={acao.ocupado || a.itens.length === 0}
                onClick={() => mudarStatus('CONCLUIDO', 'Atendimento concluído.')}>
                Concluir atendimento
              </button>
            )}
            {agendado && (
              <button disabled={acao.ocupado}
                onClick={() => window.confirm('Cancelar este agendamento? O horário fica livre.') &&
                  mudarStatus('CANCELADO', 'Agendamento cancelado.')}>
                Cancelar
              </button>
            )}
            {a.status === 'CANCELADO' && (
              <button disabled={acao.ocupado} onClick={() => mudarStatus('AGENDADO', 'Agendamento reativado.')}>
                Reativar
              </button>
            )}
            {a.status !== 'CONCLUIDO' && (
              <button className="perigo" disabled={acao.ocupado} onClick={excluir}>Excluir lançamento</button>
            )}
          </div>
          {agendado && <Remarcar agenda={a} executar={executar} ocupado={acao.ocupado} />}
        </section>
      )}
    </>
  )
}

/** Escolhe o profissional e depois um dos serviços que ele faz. */
function AdicionarServico({ agendaId, executar, ocupado }) {
  const profissionais = useCarregar(() => api('/profissionais'), [])
  const [profissionalId, setProfissionalId] = useState('')
  const [servicoId, setServicoId] = useState('')
  const escolhido = (profissionais.dados ?? []).find((p) => String(p.id) === profissionalId)

  async function adicionar(evento) {
    evento.preventDefault()
    await executar(
      () => api(`/agendas/${agendaId}/itens`, { metodo: 'POST', params: { servicoId, profissionalId } }),
      'Serviço incluído.',
    )
    setServicoId('')
  }

  return (
    <form className="formulario" onSubmit={adicionar} style={{ marginTop: '1rem' }}>
      <label className="campo">
        Profissional
        <select value={profissionalId} onChange={(e) => { setProfissionalId(e.target.value); setServicoId('') }} required>
          <option value="">Escolha…</option>
          {(profissionais.dados ?? []).map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
        </select>
      </label>
      <label className="campo">
        Serviço
        <select value={servicoId} onChange={(e) => setServicoId(e.target.value)} required disabled={!escolhido}>
          <option value="">Escolha…</option>
          {(escolhido?.servicos ?? []).map((s) => (
            <option key={s.id} value={s.id}>{s.nome} · {dinheiro(s.preco)}</option>
          ))}
        </select>
      </label>
      <button disabled={ocupado || !servicoId}>Incluir serviço</button>
    </form>
  )
}

/**
 * Registra um pagamento. Em dinheiro, calcula o troco: a pessoa digita quanto o cliente
 * entregou, e o sistema mostra quanto devolver. O valor registrado é o do atendimento, não o entregue.
 */
function RegistrarPagamento({ agendaId, falta, executar, ocupado }) {
  const [forma, setForma] = useState('PIX')
  const [valor, setValor] = useState(falta.toFixed(2))
  const [recebido, setRecebido] = useState('')
  const troco = Number(recebido) - Number(valor)

  async function registrar(evento) {
    evento.preventDefault()
    await executar(
      () => api(`/agendas/${agendaId}/pagamentos`, { metodo: 'POST', params: { valor, formaPagamento: forma } }),
      'Pagamento registrado.',
    )
  }

  return (
    <form className="pilha" onSubmit={registrar} style={{ marginTop: '.75rem' }}>
      <div className="escolhas" role="group" aria-label="Forma de pagamento">
        {Object.entries(rotuloForma).map(([chave, rotulo]) => (
          <button type="button" key={chave} className="escolha" aria-pressed={forma === chave} onClick={() => setForma(chave)}>
            {rotulo}
          </button>
        ))}
      </div>
      <div className="formulario">
        <label className="campo">
          Valor (R$)
          <input type="number" min="0.01" step="0.01" max={falta.toFixed(2)} value={valor} onChange={(e) => setValor(e.target.value)} required />
        </label>
        {forma === 'DINHEIRO' && (
          <label className="campo">
            Cliente entregou (R$)
            <input type="number" min="0" step="0.01" value={recebido} onChange={(e) => setRecebido(e.target.value)} />
          </label>
        )}
      </div>
      {forma === 'DINHEIRO' && recebido !== '' && (
        <p>
          {troco >= 0 ? <>Troco: <strong>{dinheiro(troco)}</strong></> : <span style={{ color: 'var(--bad)' }}>Faltam {dinheiro(-troco)}</span>}
        </p>
      )}
      <button className="primario" disabled={ocupado}>Registrar pagamento</button>
    </form>
  )
}

function Remarcar({ agenda, executar, ocupado }) {
  const [novaDataHora, setNovaDataHora] = useState(agenda.dataHora.slice(0, 16))

  return (
    <form className="formulario" style={{ marginTop: '1rem' }}
      onSubmit={(e) => {
        e.preventDefault()
        executar(() => api(`/agendas/${agenda.id}/remarcar`, { metodo: 'PUT', corpo: { dataHora: `${novaDataHora}:00` } }),
          'Agendamento remarcado.')
      }}>
      <label className="campo">
        Remarcar para
        <input type="datetime-local" value={novaDataHora} onChange={(e) => setNovaDataHora(e.target.value)} required />
      </label>
      <button disabled={ocupado}>Remarcar</button>
    </form>
  )
}
