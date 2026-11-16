package br.com.leotricano.sistemasalao.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração do login e dos perfis (#18, RNF01).
 *
 * <p>Aqui o login é <b>de verdade</b>: o teste chama {@code POST /auth/login}, recebe o token e o
 * manda no cabeçalho {@code Authorization: Bearer ...}, exatamente como o frontend faz. O
 * administrador inicial é criado pelo {@code PrimeiroAcesso} com a senha de teste de
 * {@code src/test/resources/application.properties}.
 */
@SpringBootTest
class SegurancaTest {

    private static final String SENHA_ADMIN = "senha-de-teste-123";

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void montarMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    // ---------- ajudantes ----------

    private ResultActions login(String login, String senha) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"%s\",\"senha\":\"%s\"}".formatted(login, senha)));
    }

    /** Entra e devolve o valor do cabeçalho Authorization pronto: "Bearer eyJ...". */
    private String entrar(String login, String senha) throws Exception {
        String json = login(login, senha).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    private long id(ResultActions resposta) throws Exception {
        return ((Number) JsonPath.read(resposta.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private ResultActions postJson(String token, String url, String json) throws Exception {
        return mvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // ---------- testes ----------

    @Test
    @DisplayName("Sem token, a API recusa com 401 no formato padrão de erro")
    void semTokenDa401() throws Exception {
        mvc.perform(get("/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    @Test
    @DisplayName("Login com senha errada dá 401; token adulterado também")
    void loginErradoETokenAdulterado() throws Exception {
        login("admin", "senha-errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Login ou senha incorretos."));

        String token = entrar("admin", SENHA_ADMIN);
        String adulterado = token.substring(0, token.length() - 3) + "abc";
        mvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login certo devolve o token, e com ele as rotas funcionam")
    void loginDevolveTokenQueFunciona() throws Exception {
        login("ADMIN ", SENHA_ADMIN) // maiúsculas e espaço não importam
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.perfil").value("ADMIN"));

        String token = entrar("admin", SENHA_ADMIN);
        mvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("admin"));
        mvc.perform(get("/relatorios/faturamento").param("de", "2030-01-01").param("ate", "2030-01-31")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Manter conectado: o login vale 30 dias em vez de 12 horas (#43)")
    void manterConectadoValeTrintaDias() throws Exception {
        String curto = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"admin\",\"senha\":\"%s\"}".formatted(SENHA_ADMIN)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String longo = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"admin\",\"senha\":\"%s\",\"manterConectado\":true}".formatted(SENHA_ADMIN)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Instant agora = Instant.now();
        Instant expiraCurto = Instant.parse(JsonPath.read(curto, "$.expiraEm"));
        Instant expiraLongo = Instant.parse(JsonPath.read(longo, "$.expiraEm"));
        assertThat(expiraCurto).isBefore(agora.plus(Duration.ofHours(13)));
        assertThat(expiraLongo).isAfter(agora.plus(Duration.ofDays(29)));
    }

    @Test
    @DisplayName("Recepção agenda e cadastra clientes, mas não vê o faturamento nem cria serviços (403)")
    void recepcaoNaoVeFaturamento() throws Exception {
        var recepcao = jwt().jwt(t -> t.subject("recepcao").claim("uid", 99L).claim("perfil", "RECEPCAO"))
                .authorities(new SimpleGrantedAuthority("ROLE_RECEPCAO"));

        mvc.perform(post("/clientes").with(recepcao).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Cliente da Recepção\",\"email\":\"rec@email.com\",\"telefone\":\"21988887777\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/relatorios/faturamento").with(recepcao).param("de", "2030-01-01").param("ate", "2030-01-31"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mvc.perform(post("/servicos").with(recepcao).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Escova\",\"duracaoMinutos\":30,\"preco\":40}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Profissional entra com o próprio login e vê só os próprios horários")
    void profissionalVeSoAPropriaAgenda() throws Exception {
        String admin = entrar("admin", SENHA_ADMIN);

        // O dono cadastra um serviço, dois profissionais e um cliente
        long corte = id(postJson(admin, "/servicos", "{\"nome\":\"Corte Seg\",\"duracaoMinutos\":60,\"preco\":80}"));
        long joao = id(postJson(admin, "/profissionais", "{\"nome\":\"João Seg\",\"especialidade\":\"Barbeiro\"}"));
        long maria = id(postJson(admin, "/profissionais", "{\"nome\":\"Maria Seg\",\"especialidade\":\"Cabeleireira\"}"));
        mvc.perform(post("/profissionais/{p}/servicos/{s}", joao, corte).header(HttpHeaders.AUTHORIZATION, admin));
        mvc.perform(post("/profissionais/{p}/servicos/{s}", maria, corte).header(HttpHeaders.AUTHORIZATION, admin));
        long cliente = id(postJson(admin, "/clientes", "{\"nome\":\"Cliente Seg\",\"email\":\"seg@email.com\",\"telefone\":\"21977776666\"}"));

        // Um atendimento com o João e outro com a Maria, no mesmo dia
        long agendaJoao = id(postJson(admin, "/agendas", "{\"cliente\":{\"id\":%d},\"dataHora\":\"2030-08-05T09:00:00\"}".formatted(cliente)));
        mvc.perform(post("/agendas/{id}/itens", agendaJoao).param("servicoId", "" + corte).param("profissionalId", "" + joao)
                .header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isOk());
        long agendaMaria = id(postJson(admin, "/agendas", "{\"cliente\":{\"id\":%d},\"dataHora\":\"2030-08-05T11:00:00\"}".formatted(cliente)));
        mvc.perform(post("/agendas/{id}/itens", agendaMaria).param("servicoId", "" + corte).param("profissionalId", "" + maria)
                .header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isOk());

        // O dono cria o acesso do João
        postJson(admin, "/usuarios", """
                {"nome":"João Seg","login":"joao.seg","senha":"senha-joao-1","perfil":"PROFISSIONAL","profissionalId":%d}
                """.formatted(joao)).andExpect(status().isOk());
        String tokenJoao = entrar("joao.seg", "senha-joao-1");

        // O João vê o dia todo, mas só a agenda dele, mesmo pedindo a da Maria na URL
        mvc.perform(get("/agendas").param("de", "2030-08-05").param("ate", "2030-08-05")
                        .param("profissionalId", "" + maria).header(HttpHeaders.AUTHORIZATION, tokenJoao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(agendaJoao));
        mvc.perform(get("/agendas/{id}", agendaMaria).header(HttpHeaders.AUTHORIZATION, tokenJoao))
                .andExpect(status().isNotFound());

        // E não pode cadastrar clientes nem ver o faturamento
        postJson(tokenJoao, "/clientes", "{\"nome\":\"X\",\"email\":\"x@email.com\",\"telefone\":\"1\"}")
                .andExpect(status().isForbidden());
        mvc.perform(get("/relatorios/faturamento").param("de", "2030-08-01").param("ate", "2030-08-31")
                        .header(HttpHeaders.AUTHORIZATION, tokenJoao))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Usuários: login repetido dá 400; trocar senha exige a senha atual")
    void cadastroDeUsuarioETrocaDeSenha() throws Exception {
        String admin = entrar("admin", SENHA_ADMIN);
        String novo = "{\"nome\":\"Paula Recepção\",\"login\":\"paula\",\"senha\":\"senha-paula-1\",\"perfil\":\"RECEPCAO\"}";
        postJson(admin, "/usuarios", novo).andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("RECEPCAO"));
        postJson(admin, "/usuarios", novo).andExpect(status().isBadRequest());

        String paula = entrar("paula", "senha-paula-1");
        mvc.perform(put("/auth/senha").header(HttpHeaders.AUTHORIZATION, paula).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"errada\",\"novaSenha\":\"senha-nova-1\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/auth/senha").header(HttpHeaders.AUTHORIZATION, paula).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"senha-paula-1\",\"novaSenha\":\"senha-nova-1\"}"))
                .andExpect(status().isNoContent());

        login("paula", "senha-paula-1").andExpect(status().isUnauthorized());
        login("paula", "senha-nova-1").andExpect(status().isOk());
    }
}
