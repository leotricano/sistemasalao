package br.com.leotricano.sistemasalao.model;

/**
 * Perfil de acesso de um usuário do sistema (#18, RNF01), seguindo as personas da Tarefa 2.
 * <ul>
 *   <li>{@code ADMIN}: o dono do salão. Pode tudo, inclusive o relatório de faturamento, os
 *       cadastros de serviços e profissionais e os logins.</li>
 *   <li>{@code RECEPCAO}: atende o balcão e o telefone. Agenda, cadastra clientes, registra
 *       pagamentos; não vê o faturamento nem mexe em serviços, profissionais e logins.</li>
 *   <li>{@code PROFISSIONAL}: só consulta a própria agenda.</li>
 * </ul>
 * As regras de cada rota ficam no {@code SecurityConfig}.
 */
public enum Perfil {
    ADMIN, RECEPCAO, PROFISSIONAL
}
