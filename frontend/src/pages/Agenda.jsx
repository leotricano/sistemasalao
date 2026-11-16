// Agenda do dia ou da semana (#19, RF04). Usa GET /agendas?de=&ate=&profissionalId=&status=.
// Para o perfil PROFISSIONAL, a API já devolve só os horários dele.

import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { useSessao } from '../sessao.jsx'
import { Carregando, Erro, Status, useCarregar } from '../componentes.jsx'
import { diaCurto, dinheiro, hoje, hora, minutosEmTexto, segundaDaSemana, somarDias } from '../formato.js'

export default function Agenda() {
  const { podeOperar, ehProfissional } = useSessao()
  const [modo, setModo] = useState('dia')
  const [dia, setDia] = useState(hoje())
  const [profissionalId, setProfissionalId] = useState('')
  const [mostrarCancelados, setMostrarCancelados] = useState(false)

  // Período pedido à API: o dia escolhido ou a semana (segunda a domingo) que o contém
  const de = modo === 'dia' ? dia : segundaDaSemana(dia)
  const ate = modo === 'dia' ? dia : somarDias(de, 6)
  const passo = modo === 'dia' ? 1 : 7

  const profissionais = useCarregar(() => (podeOperar ? api('/profissionais') : Promise.resolve([])), [podeOperar])
  const agendas = useCarregar(
    () => api('/agendas', { params: { de, ate, profissionalId } }),
    [de, ate, profissionalId],
  )

  const lista = (agendas.dados ?? []).filter((a) => mostrarCancelados || a.status !== 'CANCELADO')

  // Agrupa por dia: { "2026-11-18": [agenda, agenda], ... }
  const porDia = {}
  for (const agenda of lista) {
    const chave = agenda.dataHora.slice(0, 10)
    ;(porDia[chave] ??= []).push(agenda)
  }
  const dias = modo === 'dia' ? [dia] : Array.from({ length: 7 }, (_, i) => somarDias(de, i))

  return (
    <>
      <div className="cabecalho">
        <h1>{ehProfissional ? 'Minha agenda' : 'Agenda'}</h1>
        {podeOperar && <Link className="botao primario" to="/agendamentos/novo">+ Novo agendamento</Link>}
      </div>

      <div className="cartao linha">
        <div className="escolhas" role="group" aria-label="Período">
          <button className="escolha" aria-pressed={modo === 'dia'} onClick={() => setModo('dia')}>Dia</button>
          <button className="escolha" aria-pressed={modo === 'semana'} onClick={() => setModo('semana')}>Semana</button>
        </div>
        <button onClick={() => setDia(somarDias(dia, -passo))} aria-label="Anterior">‹</button>
        <input type="date" value={dia} onChange={(e) => e.target.value && setDia(e.target.value)} aria-label="Dia" />
        <button onClick={() => setDia(somarDias(dia, passo))} aria-label="Próximo">›</button>
        <button onClick={() => setDia(hoje())}>Hoje</button>
        {podeOperar && (
          <select value={profissionalId} onChange={(e) => setProfissionalId(e.target.value)} aria-label="Profissional">
            <option value="">Todos os profissionais</option>
            {(profissionais.dados ?? []).map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
          </select>
        )}
        <label className="linha" style={{ fontWeight: 600 }}>
          <input type="checkbox" checked={mostrarCancelados} onChange={(e) => setMostrarCancelados(e.target.checked)} style={{ minHeight: 0 }} />
          Mostrar cancelados
        </label>
      </div>

      <Erro erro={agendas.erro} />
      {agendas.carregando && !agendas.dados ? <Carregando /> : (
        dias.map((d) => (
          <section className="dia" key={d}>
            <h2>{diaCurto(d)}</h2>
            {(porDia[d] ?? []).length === 0 ? (
              modo === 'dia' && <div className="cartao vazio">Nenhum atendimento neste dia.</div>
            ) : (
              porDia[d].map((a) => <CartaoAtendimento key={a.id} agenda={a} />)
            )}
            {modo === 'semana' && !(porDia[d] ?? []).length && <p className="muted">Livre.</p>}
          </section>
        ))
      )}
    </>
  )
}

function CartaoAtendimento({ agenda }) {
  const servicos = agenda.itens.map((i) => `${i.nomeServico} · ${i.nomeProfissional}`).join(' + ')
  return (
    <Link to={`/agendamentos/${agenda.id}`} className={`atendimento ${agenda.status}`}>
      <div className="horario">
        {hora(agenda.dataHora)}
        <div className="muted" style={{ fontSize: '.8rem' }}>até {hora(agenda.dataHoraFim)}</div>
      </div>
      <div>
        <strong>{agenda.nomeCliente}</strong>
        <div className="muted">{servicos || 'Sem serviços ainda'}</div>
      </div>
      <div className="direita">
        <Status status={agenda.status} />
        <div className="muted" style={{ fontSize: '.85rem' }}>
          {dinheiro(agenda.total)} · {minutosEmTexto(agenda.duracaoTotalMinutos)}
        </div>
      </div>
    </Link>
  )
}
