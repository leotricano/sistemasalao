// Profissionais e os serviços que cada um faz (#20). Só o dono cadastra e muda os vínculos.

import { useState } from 'react'
import { api } from '../api.js'
import { useSessao } from '../sessao.jsx'
import { Carregando, Erro, Sucesso, useAcao, useCarregar } from '../componentes.jsx'

export default function Profissionais() {
  const { ehAdmin } = useSessao()
  const profissionais = useCarregar(() => api('/profissionais'), [])
  const servicos = useCarregar(() => api('/servicos'), [])
  const [novo, setNovo] = useState({ nome: '', especialidade: '' })
  const acao = useAcao()

  async function executar(funcao, mensagem) {
    const ok = await acao.rodar(funcao, mensagem)
    profissionais.recarregar()
    return ok
  }

  async function cadastrar(evento) {
    evento.preventDefault()
    if (await executar(() => api('/profissionais', { metodo: 'POST', corpo: novo }), `${novo.nome} cadastrado(a).`)) {
      setNovo({ nome: '', especialidade: '' })
    }
  }

  /** Marca ou desmarca um serviço do profissional (POST ou DELETE no vínculo). */
  const alternar = (profissional, servico, faz) =>
    executar(() => api(`/profissionais/${profissional.id}/servicos/${servico.id}`, { metodo: faz ? 'DELETE' : 'POST' }))

  const lista = [...(profissionais.dados ?? [])].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))

  return (
    <>
      <div className="cabecalho"><h1>Profissionais</h1></div>

      {ehAdmin && (
        <form className="cartao formulario" onSubmit={cadastrar}>
          <label className="campo">Nome<input value={novo.nome} onChange={(e) => setNovo({ ...novo, nome: e.target.value })} required /></label>
          <label className="campo">Especialidade<input value={novo.especialidade} onChange={(e) => setNovo({ ...novo, especialidade: e.target.value })} placeholder="Ex.: Cabeleireiro" required /></label>
          <button className="primario" disabled={acao.ocupado}>Cadastrar profissional</button>
        </form>
      )}
      <Erro erro={acao.erro ?? profissionais.erro} />
      <Sucesso texto={acao.sucesso} />

      {profissionais.carregando && !profissionais.dados ? <Carregando /> : (
        <div className="grade">
          {lista.map((p) => (
            <section key={p.id} className="cartao pilha">
              <div className="linha" style={{ justifyContent: 'space-between' }}>
                <div><h2 style={{ margin: 0 }}>{p.nome}</h2><span className="muted">{p.especialidade}</span></div>
                {ehAdmin && (
                  <button className="pequeno perigo" disabled={acao.ocupado}
                    onClick={() => window.confirm(`Remover ${p.nome}?`) && executar(() => api(`/profissionais/${p.id}`, { metodo: 'DELETE' }), 'Profissional removido.')}>
                    Remover
                  </button>
                )}
              </div>
              <div className="escolhas" aria-label={`Serviços de ${p.nome}`}>
                {ehAdmin
                  ? (servicos.dados ?? []).map((s) => {
                      const faz = p.servicos.some((x) => x.id === s.id)
                      return (
                        <button key={s.id} className="escolha pequeno" aria-pressed={faz} disabled={acao.ocupado} onClick={() => alternar(p, s, faz)}>
                          {s.nome}
                        </button>
                      )
                    })
                  : p.servicos.map((s) => <span key={s.id} className="etiqueta neutra">{s.nome}</span>)}
              </div>
              {ehAdmin && <p className="muted" style={{ fontSize: '.85rem' }}>Toque num serviço para ligar ou desligar.</p>}
            </section>
          ))}
        </div>
      )}
    </>
  )
}
