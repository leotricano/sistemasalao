// Novo agendamento (#19, RF03), no fluxo que o dono do salão pediu na validação do protótipo (#28):
// especialidade → profissional → serviços (vários) → cliente → data e horário.
// Dá para juntar serviços de outro profissional na mesma visita (ex.: pedicure depois do cabelo).
//
// Na API, o agendamento é criado em partes: POST /agendas e depois POST /agendas/{id}/itens para
// cada serviço. Se algum serviço for recusado (ex.: 409, horário ocupado), a agenda recém-criada é
// apagada, para não sobrar um agendamento pela metade.

import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api.js'
import { Carregando, Erro, useAcao, useCarregar } from '../componentes.jsx'
import { dinheiro, hoje, hora, minutosEmTexto } from '../formato.js'

export default function NovoAgendamento() {
  const navegar = useNavigate()
  const acao = useAcao()
  const profissionais = useCarregar(() => api('/profissionais'), [])
  const clientes = useCarregar(() => api('/clientes'), [])

  const [especialidade, setEspecialidade] = useState('')
  const [profissionalId, setProfissionalId] = useState(null)
  // Serviços escolhidos: [{ profissional, servico }], na ordem em que serão feitos
  const [escolhidos, setEscolhidos] = useState([])
  const [clienteId, setClienteId] = useState(null)
  const [busca, setBusca] = useState('')
  const [dia, setDia] = useState(hoje())
  const [horario, setHorario] = useState('09:00')

  const lista = profissionais.dados ?? []
  const especialidades = useMemo(() => [...new Set(lista.map((p) => p.especialidade))].sort(), [lista])
  const filtrados = lista.filter((p) => !especialidade || p.especialidade === especialidade)
  const profissional = lista.find((p) => p.id === profissionalId)

  const duracao = escolhidos.reduce((soma, e) => soma + e.servico.duracaoMinutos, 0)
  const total = escolhidos.reduce((soma, e) => soma + Number(e.servico.preco), 0)
  const envolvidos = [...new Set(escolhidos.map((e) => e.profissional.id))]

  const marcado = (servico) => escolhidos.some((e) => e.profissional.id === profissionalId && e.servico.id === servico.id)
  function alternarServico(servico) {
    setEscolhidos((atual) => marcado(servico)
      ? atual.filter((e) => !(e.profissional.id === profissionalId && e.servico.id === servico.id))
      : [...atual, { profissional, servico }])
  }

  const termo = busca.trim().toLowerCase()
  const clientesFiltrados = (clientes.dados ?? [])
    .filter((c) => !termo || c.nome.toLowerCase().includes(termo) || (c.telefone ?? '').includes(termo))
    .slice(0, 8)
  const cliente = (clientes.dados ?? []).find((c) => c.id === clienteId)

  async function confirmar() {
    const agenda = await acao.rodar(async () => {
      const criada = await api('/agendas', { metodo: 'POST', corpo: { cliente: { id: clienteId }, dataHora: `${dia}T${horario}:00` } })
      try {
        for (const e of escolhidos) {
          await api(`/agendas/${criada.id}/itens`, { metodo: 'POST', params: { servicoId: e.servico.id, profissionalId: e.profissional.id } })
        }
      } catch (erro) {
        await api(`/agendas/${criada.id}`, { metodo: 'DELETE' }).catch(() => {})
        throw erro
      }
      return criada
    })
    if (agenda) navegar(`/agendamentos/${agenda.id}`)
  }

  if (profissionais.carregando && !profissionais.dados) return <Carregando />

  return (
    <>
      <div className="cabecalho"><h1>Novo agendamento</h1></div>
      <Erro erro={profissionais.erro ?? clientes.erro} />

      <section className="cartao pilha">
        <h2>1. Especialidade</h2>
        <div className="escolhas">
          <button className="escolha" aria-pressed={!especialidade} onClick={() => setEspecialidade('')}>Todas</button>
          {especialidades.map((e) => (
            <button key={e} className="escolha" aria-pressed={especialidade === e} onClick={() => setEspecialidade(e)}>{e}</button>
          ))}
        </div>

        <h2>2. Profissional</h2>
        <div className="escolhas">
          {filtrados.map((p) => (
            <button key={p.id} className="escolha" aria-pressed={profissionalId === p.id} onClick={() => setProfissionalId(p.id)}>
              {p.nome} <span className="muted" style={{ fontWeight: 400 }}>· {p.especialidade}</span>
            </button>
          ))}
          {filtrados.length === 0 && <span className="muted">Nenhum profissional cadastrado.</span>}
        </div>

        {profissional && (
          <>
            <h2>3. Serviços com {profissional.nome}</h2>
            {profissional.servicos.length === 0 && <p className="muted">Este profissional ainda não tem serviços vinculados.</p>}
            <div className="escolhas">
              {profissional.servicos.map((s) => (
                <button key={s.id} className="escolha" aria-pressed={marcado(s)} onClick={() => alternarServico(s)}>
                  {s.nome} · {dinheiro(s.preco)} · {minutosEmTexto(s.duracaoMinutos)}
                </button>
              ))}
            </div>
            <p className="muted">Para incluir um serviço com outra pessoa (ex.: manicure), escolha outro profissional acima.</p>
          </>
        )}

        {escolhidos.length > 0 && (
          <div className="cartao" style={{ background: 'var(--sunk)', boxShadow: 'none' }}>
            <strong>Serviços da visita, nesta ordem:</strong>
            <ol style={{ margin: '.4rem 0' }}>
              {escolhidos.map((e) => (
                <li key={`${e.profissional.id}-${e.servico.id}`}>{e.servico.nome} com {e.profissional.nome}</li>
              ))}
            </ol>
            Duração: <strong>{minutosEmTexto(duracao)}</strong> · Total: <strong>{dinheiro(total)}</strong>
          </div>
        )}
      </section>

      <section className="cartao pilha">
        <h2>4. Cliente</h2>
        {cliente ? (
          <div className="linha">
            <strong>{cliente.nome}</strong> <span className="muted">{cliente.telefone}</span>
            <button className="pequeno" onClick={() => setClienteId(null)}>Trocar</button>
          </div>
        ) : (
          <>
            <input placeholder="Buscar por nome ou telefone" value={busca} onChange={(e) => setBusca(e.target.value)} aria-label="Buscar cliente" />
            <div className="escolhas">
              {clientesFiltrados.map((c) => (
                <button key={c.id} className="escolha" onClick={() => setClienteId(c.id)}>{c.nome}</button>
              ))}
            </div>
            <ClienteRapido onCriado={(novo) => { clientes.recarregar(); setClienteId(novo.id) }} nomeInicial={busca} />
          </>
        )}
      </section>

      <section className="cartao pilha">
        <h2>5. Data e horário</h2>
        <div className="formulario">
          <label className="campo">Dia<input type="date" value={dia} onChange={(e) => setDia(e.target.value)} /></label>
          <label className="campo">Horário<input type="time" step="900" value={horario} onChange={(e) => setHorario(e.target.value)} /></label>
        </div>
        {envolvidos.map((id) => <Ocupacao key={id} profissional={lista.find((p) => p.id === id)} dia={dia} />)}
        <Erro erro={acao.erro} />
        <div className="linha fim">
          <button className="primario" onClick={confirmar} disabled={acao.ocupado || !clienteId || escolhidos.length === 0}>
            {acao.ocupado ? 'Agendando…' : 'Confirmar agendamento'}
          </button>
        </div>
      </section>
    </>
  )
}

/** Horários já ocupados do profissional no dia escolhido, para ajudar a achar um horário livre. */
function Ocupacao({ profissional, dia }) {
  const agendas = useCarregar(
    () => api('/agendas', { params: { de: dia, ate: dia, profissionalId: profissional.id, status: 'AGENDADO' } }),
    [dia, profissional.id],
  )
  const ocupados = agendas.dados ?? []
  return (
    <p className="muted">
      <strong>{profissional.nome}</strong> neste dia:{' '}
      {ocupados.length === 0 ? 'livre o dia todo.' : ocupados.map((a) => `${hora(a.dataHora)}–${hora(a.dataHoraFim)}`).join(', ')}
    </p>
  )
}

/** Cadastro rápido de cliente sem sair do agendamento. */
function ClienteRapido({ onCriado, nomeInicial }) {
  const [aberto, setAberto] = useState(false)
  const [dados, setDados] = useState({ nome: '', telefone: '', email: '' })
  const acao = useAcao()

  if (!aberto) {
    return <button className="pequeno" onClick={() => { setDados({ ...dados, nome: nomeInicial }); setAberto(true) }}>+ Cliente novo</button>
  }

  async function salvar(evento) {
    evento.preventDefault()
    const novo = await acao.rodar(() => api('/clientes', { metodo: 'POST', corpo: dados }))
    if (novo) onCriado(novo)
  }

  return (
    <form className="formulario" onSubmit={salvar}>
      <Erro erro={acao.erro} />
      <label className="campo">Nome<input value={dados.nome} onChange={(e) => setDados({ ...dados, nome: e.target.value })} required /></label>
      <label className="campo">Telefone<input value={dados.telefone} onChange={(e) => setDados({ ...dados, telefone: e.target.value })} required /></label>
      <label className="campo">E-mail<input type="email" value={dados.email} onChange={(e) => setDados({ ...dados, email: e.target.value })} required /></label>
      <button disabled={acao.ocupado}>Cadastrar cliente</button>
    </form>
  )
}
