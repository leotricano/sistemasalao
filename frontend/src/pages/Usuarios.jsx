// Acessos ao sistema (#18): o dono cria o login da recepção e de cada profissional.

import { useState } from 'react'
import { api } from '../api.js'
import { useSessao } from '../sessao.jsx'
import { Carregando, Erro, Sucesso, useAcao, useCarregar } from '../componentes.jsx'
import { rotuloPerfil } from '../formato.js'

const vazio = { nome: '', login: '', senha: '', perfil: 'RECEPCAO', profissionalId: '' }

export default function Usuarios() {
  const { usuario } = useSessao()
  const usuarios = useCarregar(() => api('/usuarios'), [])
  const profissionais = useCarregar(() => api('/profissionais'), [])
  const [novo, setNovo] = useState(vazio)
  const acao = useAcao()

  async function executar(funcao, mensagem) {
    const ok = await acao.rodar(funcao, mensagem)
    usuarios.recarregar()
    return ok
  }

  async function criar(evento) {
    evento.preventDefault()
    const corpo = { ...novo, profissionalId: novo.perfil === 'PROFISSIONAL' ? Number(novo.profissionalId) : null }
    if (await executar(() => api('/usuarios', { metodo: 'POST', corpo }), `Acesso de ${novo.nome} criado. Passe o login e a senha para a pessoa.`)) {
      setNovo(vazio)
    }
  }

  return (
    <>
      <div className="cabecalho"><h1>Acessos</h1></div>

      <form className="cartao formulario" onSubmit={criar}>
        <label className="campo">Nome<input value={novo.nome} onChange={(e) => setNovo({ ...novo, nome: e.target.value })} required /></label>
        <label className="campo">Login<input value={novo.login} onChange={(e) => setNovo({ ...novo, login: e.target.value })} autoComplete="off" required /></label>
        <label className="campo">Senha inicial<input type="password" minLength={6} value={novo.senha} onChange={(e) => setNovo({ ...novo, senha: e.target.value })} autoComplete="new-password" required /></label>
        <label className="campo">
          Perfil
          <select value={novo.perfil} onChange={(e) => setNovo({ ...novo, perfil: e.target.value })}>
            {Object.entries(rotuloPerfil).map(([chave, rotulo]) => <option key={chave} value={chave}>{rotulo}</option>)}
          </select>
        </label>
        {novo.perfil === 'PROFISSIONAL' && (
          <label className="campo">
            Qual profissional
            <select value={novo.profissionalId} onChange={(e) => setNovo({ ...novo, profissionalId: e.target.value })} required>
              <option value="">Escolha…</option>
              {(profissionais.dados ?? []).map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
            </select>
          </label>
        )}
        <button className="primario" disabled={acao.ocupado}>Criar acesso</button>
      </form>
      <Erro erro={acao.erro ?? usuarios.erro} />
      <Sucesso texto={acao.sucesso} />

      <section className="cartao tabela-rolavel">
        {!usuarios.dados ? <Carregando /> : (
          <table>
            <thead><tr><th>Nome</th><th>Login</th><th>Perfil</th><th /></tr></thead>
            <tbody>
              {usuarios.dados.map((u) => (
                <tr key={u.id}>
                  <td><strong>{u.nome}</strong></td>
                  <td className="mono">{u.login}</td>
                  <td>{rotuloPerfil[u.perfil]}{u.nomeProfissional ? ` · ${u.nomeProfissional}` : ''}</td>
                  <td className="direita">
                    {u.id !== usuario.id && (
                      <button className="pequeno perigo" disabled={acao.ocupado}
                        onClick={() => window.confirm(`Remover o acesso de ${u.nome}?`) && executar(() => api(`/usuarios/${u.id}`, { metodo: 'DELETE' }), 'Acesso removido.')}>
                        Remover
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  )
}
