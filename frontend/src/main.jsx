// Ponto de entrada do React (#19): coloca o App dentro da <div id="root"> do index.html.
//
// HashRouter: as rotas ficam depois do "#" na URL (ex.: /#/clientes). Assim o Spring sempre
// entrega o mesmo index.html e não confunde a tela /clientes com a rota /clientes da API.

import React from 'react'
import ReactDOM from 'react-dom/client'
import { HashRouter } from 'react-router-dom'
import App from './App.jsx'
import { SessaoProvider } from './sessao.jsx'
import './estilos.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <HashRouter>
      <SessaoProvider>
        <App />
      </SessaoProvider>
    </HashRouter>
  </React.StrictMode>,
)
