// Estrutura das telas (#19): login, barra do topo com o menu de cada perfil e as rotas.

import { Navigate, NavLink, Route, Routes } from 'react-router-dom'
import { useSessao } from './sessao.jsx'
import { rotuloPerfil } from './formato.js'
import Login from './pages/Login.jsx'
import Agenda from './pages/Agenda.jsx'
import AgendaDetalhe from './pages/AgendaDetalhe.jsx'
import NovoAgendamento from './pages/NovoAgendamento.jsx'
import Clientes from './pages/Clientes.jsx'
import ClienteFicha from './pages/ClienteFicha.jsx'
import Servicos from './pages/Servicos.jsx'
import Profissionais from './pages/Profissionais.jsx'
import Relatorio from './pages/Relatorio.jsx'
import Usuarios from './pages/Usuarios.jsx'
import MinhaConta from './pages/MinhaConta.jsx'

export default function App() {
  const { usuario, sair, ehAdmin, podeOperar } = useSessao()

  if (!usuario) return <Login />

  // Só aparece no menu o que o perfil pode usar; a API confere de novo do lado dela
  const menu = [
    { para: '/', texto: 'Agenda', mostrar: true },
    { para: '/agendamentos/novo', texto: 'Novo agendamento', mostrar: podeOperar },
    { para: '/clientes', texto: 'Clientes', mostrar: podeOperar },
    { para: '/servicos', texto: 'Serviços', mostrar: podeOperar },
    { para: '/profissionais', texto: 'Profissionais', mostrar: podeOperar },
    { para: '/relatorio', texto: 'Relatório', mostrar: ehAdmin },
    { para: '/usuarios', texto: 'Acessos', mostrar: ehAdmin },
  ]

  // Rota que o perfil não pode abrir volta para a agenda
  const so = (permitido, tela) => (permitido ? tela : <Navigate to="/" replace />)

  return (
    <>
      <header className="topo">
        <NavLink to="/" className="marca">
          <img src="/favicon.svg" alt="" />
          Tesoura
        </NavLink>
        <nav className="menu" aria-label="Menu principal">
          {menu.filter((item) => item.mostrar).map((item) => (
            <NavLink key={item.para} to={item.para} end={item.para === '/'}>
              {item.texto}
            </NavLink>
          ))}
        </nav>
        <div className="conta">
          <NavLink to="/conta" title="Minha conta">
            {usuario.nome} <span className="muted perfil">· {rotuloPerfil[usuario.perfil]}</span>
          </NavLink>
          <button className="pequeno" onClick={sair}>Sair</button>
        </div>
      </header>

      <main>
        <Routes>
          <Route path="/" element={<Agenda />} />
          <Route path="/agendamentos/novo" element={so(podeOperar, <NovoAgendamento />)} />
          <Route path="/agendamentos/:id" element={<AgendaDetalhe />} />
          <Route path="/clientes" element={so(podeOperar, <Clientes />)} />
          <Route path="/clientes/:id" element={so(podeOperar, <ClienteFicha />)} />
          <Route path="/servicos" element={so(podeOperar, <Servicos />)} />
          <Route path="/profissionais" element={so(podeOperar, <Profissionais />)} />
          <Route path="/relatorio" element={so(ehAdmin, <Relatorio />)} />
          <Route path="/usuarios" element={so(ehAdmin, <Usuarios />)} />
          <Route path="/conta" element={<MinhaConta />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  )
}
