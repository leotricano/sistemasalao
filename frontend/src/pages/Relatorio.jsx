// Relatório de faturamento do mês (#21, RF07). Só o dono vê. Usa GET /relatorios/faturamento.
// O botão "Baixar planilha" gera um CSV que o Excel abre (adianta o #30).

import { useState } from 'react'
import { api } from '../api.js'
import { Carregando, Erro, useCarregar } from '../componentes.jsx'
import { dataBR, dinheiro, hoje, limitesDoMes, rotuloForma } from '../formato.js'

export default function Relatorio() {
  const [mes, setMes] = useState(hoje().slice(0, 7))
  const [profissionalId, setProfissionalId] = useState('')
  const { de, ate } = limitesDoMes(mes)

  const profissionais = useCarregar(() => api('/profissionais'), [])
  const relatorio = useCarregar(
    () => api('/relatorios/faturamento', { params: { de, ate, profissionalId } }),
    [de, ate, profissionalId],
  )
  const r = relatorio.dados
  const aReceber = r ? Number(r.faturamento) - Number(r.totalRecebido) : 0
  const maior = r ? Math.max(...r.porProfissional.map((p) => Number(p.faturamento)), 1) : 1
  const totalFormas = r ? r.porFormaPagamento.reduce((s, f) => s + Number(f.total), 0) : 0

  return (
    <>
      <div className="cabecalho">
        <h1>Relatório de faturamento</h1>
        {r && <button onClick={() => baixarPlanilha(r)}>Baixar planilha (Excel)</button>}
      </div>

      <div className="cartao linha">
        <label className="campo">Mês<input type="month" value={mes} onChange={(e) => e.target.value && setMes(e.target.value)} /></label>
        <label className="campo">
          Profissional
          <select value={profissionalId} onChange={(e) => setProfissionalId(e.target.value)}>
            <option value="">Salão inteiro</option>
            {(profissionais.dados ?? []).map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
          </select>
        </label>
        <span className="muted">{dataBR(de)} a {dataBR(ate)} · só atendimentos concluídos</span>
      </div>

      <Erro erro={relatorio.erro} />
      {!r ? <Carregando /> : (
        <>
          <div className="indicadores">
            <Indicador rotulo="Faturamento" valor={dinheiro(r.faturamento)} detalhe="serviços concluídos" />
            <Indicador rotulo="Recebido" valor={dinheiro(r.totalRecebido)} detalhe={aReceber > 0 ? `${dinheiro(aReceber)} a receber` : 'tudo recebido'} />
            <Indicador rotulo="Atendimentos" valor={r.atendimentos} detalhe={`${r.clientesAtendidos} clientes diferentes`} />
            <Indicador rotulo="Ticket médio" valor={dinheiro(r.ticketMedio)} detalhe="por atendimento" />
          </div>

          <div className="grade">
            <section className="cartao">
              <h2>Faturamento por profissional</h2>
              {r.porProfissional.length === 0 ? <p className="muted">Nenhum atendimento concluído no período.</p> : (
                <div className="barras">
                  {r.porProfissional.map((p) => (
                    <div key={p.profissionalId} className="barra-linha"
                      title={`${p.nome}: ${dinheiro(p.faturamento)} em ${p.atendimentos} atendimento(s)`}>
                      <span>{p.nome}</span>
                      <div className="barra-trilho">
                        <div className="barra" style={{ width: `${(Number(p.faturamento) / maior) * 100}%` }} />
                      </div>
                      <span className="direita">
                        <strong>{dinheiro(p.faturamento)}</strong>
                        <div className="muted" style={{ fontSize: '.8rem' }}>{p.atendimentos} atend.</div>
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </section>

            <section className="cartao">
              <h2>Recebido por forma de pagamento</h2>
              <table>
                <thead><tr><th>Forma</th><th className="direita">Valor</th><th className="direita">Parte</th></tr></thead>
                <tbody>
                  {r.porFormaPagamento.map((f) => (
                    <tr key={f.formaPagamento}>
                      <td>{rotuloForma[f.formaPagamento]}</td>
                      <td className="direita">{dinheiro(f.total)}</td>
                      <td className="direita muted">{totalFormas ? Math.round((Number(f.total) / totalFormas) * 100) : 0}%</td>
                    </tr>
                  ))}
                  <tr><td><strong>Total</strong></td><td className="direita"><strong>{dinheiro(r.totalRecebido)}</strong></td><td /></tr>
                </tbody>
              </table>
            </section>
          </div>
        </>
      )}
    </>
  )
}

function Indicador({ rotulo, valor, detalhe }) {
  return (
    <div className="indicador">
      <div className="rotulo">{rotulo}</div>
      <div className="valor">{valor}</div>
      <div className="detalhe">{detalhe}</div>
    </div>
  )
}

/**
 * Monta um CSV e faz o navegador baixar. Ponto e vírgula e vírgula decimal são o formato que o
 * Excel em português entende; o "﻿" no começo faz o Excel ler os acentos certo.
 */
function baixarPlanilha(r) {
  const num = (v) => Number(v).toFixed(2).replace('.', ',')
  const linhas = [
    ['Relatório de faturamento', `${dataBR(r.de)} a ${dataBR(r.ate)}`],
    [],
    ['Faturamento', num(r.faturamento)],
    ['Recebido', num(r.totalRecebido)],
    ['Atendimentos', r.atendimentos],
    ['Clientes atendidos', r.clientesAtendidos],
    ['Ticket médio', num(r.ticketMedio)],
    [],
    ['Profissional', 'Atendimentos', 'Faturamento'],
    ...r.porProfissional.map((p) => [p.nome, p.atendimentos, num(p.faturamento)]),
    [],
    ['Forma de pagamento', 'Valor'],
    ...r.porFormaPagamento.map((f) => [rotuloForma[f.formaPagamento], num(f.total)]),
  ]
  const csv = '﻿' + linhas.map((l) => l.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(';')).join('\r\n')
  const link = document.createElement('a')
  link.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  link.download = `faturamento-${r.de.slice(0, 7)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}
