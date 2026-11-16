// Formatação de dinheiro e datas em português (#19).
//
// A API trabalha com datas sem fuso, como "2026-11-18T10:00:00". Por isso as contas de dia são
// feitas com textos "aaaa-mm-dd", sem new Date(texto), que poderia mudar o dia por causa do fuso.

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export const dinheiro = (valor) => moeda.format(Number(valor ?? 0))

/** "2026-11-18T10:30:00" → "10:30" */
export const hora = (dataHora) => (dataHora ?? '').slice(11, 16)

/** "2026-11-18" ou "2026-11-18T10:30:00" → "18/11/2026" */
export const dataBR = (texto) => {
  const [a, m, d] = (texto ?? '').slice(0, 10).split('-')
  return d ? `${d}/${m}/${a}` : ''
}

/** Date → "aaaa-mm-dd" no horário local */
const paraISO = (data) =>
  `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`

const deISO = (iso) => {
  const [a, m, d] = iso.split('-').map(Number)
  return new Date(a, m - 1, d)
}

export const hoje = () => paraISO(new Date())

export const somarDias = (iso, dias) => {
  const data = deISO(iso)
  data.setDate(data.getDate() + dias)
  return paraISO(data)
}

/** Segunda-feira da semana do dia informado (o salão conta a semana de segunda a domingo). */
export const segundaDaSemana = (iso) => {
  const diaDaSemana = (deISO(iso).getDay() + 6) % 7 // segunda = 0 ... domingo = 6
  return somarDias(iso, -diaDaSemana)
}

const nomesDia = ['domingo', 'segunda', 'terça', 'quarta', 'quinta', 'sexta', 'sábado']
export const nomeDoDia = (iso) => nomesDia[deISO(iso).getDay()]

/** "2026-11-18" → "quarta, 18/11" */
export const diaCurto = (iso) => `${nomeDoDia(iso)}, ${dataBR(iso).slice(0, 5)}`

/** Primeiro e último dia do mês de "aaaa-mm" */
export const limitesDoMes = (mes) => {
  const [a, m] = mes.split('-').map(Number)
  return { de: `${mes}-01`, ate: paraISO(new Date(a, m, 0)) }
}

export const minutosEmTexto = (minutos) => {
  const h = Math.floor(minutos / 60)
  const m = minutos % 60
  if (!h) return `${m} min`
  return m ? `${h}h${String(m).padStart(2, '0')}` : `${h}h`
}

export const rotuloStatus = { AGENDADO: 'Agendado', CONCLUIDO: 'Concluído', CANCELADO: 'Cancelado' }
export const rotuloForma = { DINHEIRO: 'Dinheiro', CARTAO: 'Cartão', PIX: 'Pix' }
export const rotuloPerfil = { ADMIN: 'Administrador', RECEPCAO: 'Recepção', PROFISSIONAL: 'Profissional' }
