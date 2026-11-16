package br.com.leotricano.sistemasalao.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração das rotas principais da API (#26).
 *
 * <p>Diferente dos testes de unidade, aqui <b>nada é falso</b>: o {@code @SpringBootTest} sobe a
 * aplicação inteira (controllers, services, repositórios, TratadorDeErros, Security) ligada ao banco
 * H2 em memória (ver {@code src/test/resources/application.properties}). O {@code MockMvc} faz as
 * requisições HTTP sem precisar abrir uma porta de rede, como se fosse o Postman.
 *
 * <p>Cada teste cria os próprios dados, em datas diferentes de 2030, para um não atrapalhar o outro.
 *
 * <p>Login (#18): toda requisição destes testes vai como o dono do salão (perfil ADMIN). O
 * {@code jwt()} do spring-security-test simula um token já conferido. As regras de cada perfil
 * são testadas em {@code SegurancaTest}.
 */
@SpringBootTest
class RotasPrincipaisTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    /** Monta o MockMvc com a segurança ligada e um ADMIN logado em toda requisição ({@code defaultRequest}). */
    @BeforeEach
    void entrarComoAdmin() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .apply(springSecurity())
                .defaultRequest(get("/").with(jwt()
                        .jwt(token -> token.subject("admin").claim("uid", 1L).claim("perfil", "ADMIN"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .build();
    }

    // ---------- ajudantes: cada um faz uma chamada e devolve o id criado ----------

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions putJson(String url, String json) throws Exception {
        return mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Lê o campo {@code id} do JSON da resposta. */
    private long id(ResultActions resposta) throws Exception {
        String json = resposta.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private long criarCliente(String nome) throws Exception {
        return id(postJson("/clientes", """
                {"nome":"%s","email":"%s@email.com","telefone":"21999990000"}
                """.formatted(nome, nome.toLowerCase().replace(" ", "."))).andExpect(status().isOk()));
    }

    private long criarServico(String nome, int minutos, String preco) throws Exception {
        return id(postJson("/servicos", """
                {"nome":"%s","duracaoMinutos":%d,"preco":%s}
                """.formatted(nome, minutos, preco)).andExpect(status().isOk()));
    }

    /** Cadastra o profissional e já vincula o serviço que ele faz. */
    private long criarProfissional(String nome, long servicoId) throws Exception {
        long id = id(postJson("/profissionais", """
                {"nome":"%s","especialidade":"Cabeleireiro"}
                """.formatted(nome)).andExpect(status().isOk()));
        mvc.perform(post("/profissionais/{p}/servicos/{s}", id, servicoId)).andExpect(status().isOk());
        return id;
    }

    private long criarAgenda(long clienteId, String dataHora) throws Exception {
        return id(postJson("/agendas", """
                {"cliente":{"id":%d},"dataHora":"%s"}
                """.formatted(clienteId, dataHora))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGENDADO")));
    }

    private ResultActions adicionarServico(long agendaId, long servicoId, long profissionalId) throws Exception {
        return mvc.perform(post("/agendas/{id}/itens", agendaId)
                .param("servicoId", String.valueOf(servicoId))
                .param("profissionalId", String.valueOf(profissionalId)));
    }

    // ---------- testes ----------

    @Test
    @DisplayName("Fluxo completo: cadastros → agendamento → conflito 409 → pagamento → conclusão → relatório e histórico")
    void fluxoCompletoDoAtendimento() throws Exception {
        long ana = criarCliente("Ana Paula");
        long bia = criarCliente("Bia Souza");
        long corte = criarServico("Corte", 60, "80.00");
        long william = criarProfissional("William", corte);

        // Agendamento da Ana às 14:00, com corte de 60 minutos → termina 15:00
        long agendaAna = criarAgenda(ana, "2030-03-10T14:00:00");
        adicionarServico(agendaAna, corte, william)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.total").value(80.0))
                .andExpect(jsonPath("$.dataHoraFim").value("2030-03-10T15:00:00"));

        // Bia às 14:30 com o mesmo profissional → conflito de horário
        long agendaBia = criarAgenda(bia, "2030-03-10T14:30:00");
        adicionarServico(agendaBia, corte, william)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem", containsString("William")));

        // Pagamento e conclusão do atendimento da Ana
        mvc.perform(post("/agendas/{id}/pagamentos", agendaAna).param("valor", "80.00").param("formaPagamento", "PIX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formaPagamento").value("PIX"));
        putJson("/agendas/" + agendaAna + "/status", "\"CONCLUIDO\"")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"));

        // Agenda do dia filtrada pelo William: só a da Ana (a da Bia ficou sem serviço)
        mvc.perform(get("/agendas").param("de", "2030-03-10").param("ate", "2030-03-10")
                        .param("profissionalId", String.valueOf(william)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(agendaAna));

        // Relatório de março/2030
        mvc.perform(get("/relatorios/faturamento").param("de", "2030-03-01").param("ate", "2030-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atendimentos").value(1))
                .andExpect(jsonPath("$.clientesAtendidos").value(1))
                .andExpect(jsonPath("$.faturamento").value(80.0))
                .andExpect(jsonPath("$.ticketMedio").value(80.0))
                .andExpect(jsonPath("$.totalRecebido").value(80.0))
                .andExpect(jsonPath("$.porProfissional[0].nome").value("William"))
                .andExpect(jsonPath("$.porFormaPagamento[2].formaPagamento").value("PIX"))
                .andExpect(jsonPath("$.porFormaPagamento[2].total").value(80.0));

        // Histórico da Ana
        mvc.perform(get("/clientes/{id}/historico", ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAtendimentos").value(1))
                .andExpect(jsonPath("$.totalGasto").value(80.0));
    }

    @Test
    @DisplayName("Remarcar e cancelar: remarca para horário livre, cancela e não deixa remarcar cancelado")
    void remarcarECancelar() throws Exception {
        long cliente = criarCliente("Carol Lima");
        long escova = criarServico("Escova", 45, "50.00");
        long pro = criarProfissional("Rafaela", escova);
        long agenda = criarAgenda(cliente, "2030-04-10T10:00:00");
        adicionarServico(agenda, escova, pro).andExpect(status().isOk());

        putJson("/agendas/" + agenda + "/remarcar", "{\"dataHora\":\"2030-04-11T10:00:00\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataHora").value("2030-04-11T10:00:00"));

        putJson("/agendas/" + agenda + "/status", "\"CANCELADO\"")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        putJson("/agendas/" + agenda + "/remarcar", "{\"dataHora\":\"2030-04-12T10:00:00\"}")
                .andExpect(status().isBadRequest());

        // Cancelada não entra no faturamento
        mvc.perform(get("/relatorios/faturamento").param("de", "2030-04-01").param("ate", "2030-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atendimentos").value(0));
    }

    @Test
    @DisplayName("Cadastro de cliente com e-mail inválido volta 400 com mensagem")
    void clienteInvalido() throws Exception {
        postJson("/clientes", "{\"nome\":\"Sem Email\",\"email\":\"nao-e-email\",\"telefone\":\"21999990000\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    @Test
    @DisplayName("Agendamento que não existe volta 404 no formato padrão de erro")
    void agendaInexistente() throws Exception {
        mvc.perform(get("/agendas/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem", containsString("999999")));
    }

    @Test
    @DisplayName("Relatório sem datas ou com período invertido volta 400")
    void relatorioComPeriodoInvalido() throws Exception {
        mvc.perform(get("/relatorios/faturamento")).andExpect(status().isBadRequest());
        mvc.perform(get("/relatorios/faturamento").param("de", "2030-05-31").param("ate", "2030-05-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Não apaga cliente que tem agendamento (409)")
    void naoApagaClienteComAgendamento() throws Exception {
        long cliente = criarCliente("Duda Reis");
        criarAgenda(cliente, "2030-06-10T09:00:00");

        mvc.perform(delete("/clientes/{id}", cliente))
                .andExpect(status().isConflict());
    }
}
