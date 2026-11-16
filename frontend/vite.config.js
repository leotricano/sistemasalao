import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Configuração do Vite, a ferramenta que roda e empacota o React (#19).
//
// - Em desenvolvimento (npm run dev), as telas abrem em http://localhost:5173 e toda chamada à
//   API é repassada (proxy) para o Spring em http://localhost:8080. Assim o navegador acha que
//   tudo vem do mesmo endereço e não há problema de CORS.
// - No build (npm run build), as telas prontas vão para src/main/resources/static do Spring.
//   Aí a própria API serve o sistema inteiro em http://localhost:8080, sem precisar do Node.
const API = 'http://localhost:8080'
const rotasDaApi = ['/auth', '/usuarios', '/clientes', '/servicos', '/profissionais', '/agendas', '/relatorios']

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: Object.fromEntries(rotasDaApi.map((rota) => [rota, API])),
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
})
