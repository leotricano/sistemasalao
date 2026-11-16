package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.model.Perfil;
import br.com.leotricano.sistemasalao.service.UsuarioService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recuperar o acesso sem e-mail (#46): primeiro acesso pela tela, administrador redefinindo a
 * senha de alguém e a "chave reserva" do computador do salão.
 *
 * <p>{@code app.admin.senha} vazia: o sistema sobe <b>sem nenhum usuário</b>, como numa instalação nova.
 */
@SpringBootTest
@TestPropertySource(properties = "app.admin.senha=")
class AcessoSemEmailTest {

    private static final String DONO = """
            {"nome":"Dono do Salão","login":"dono","senha":"senha-do-dono-1"}""";

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private UsuarioService usuarioService;

    private MockMvc mvc;

    @BeforeEach
    void montarMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String entrar(String login, String senha) throws Exception {
        String json = postJson("/auth/login", "{\"login\":\"%s\",\"senha\":\"%s\"}".formatted(login, senha))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    /** Os testes rodam em qualquer ordem: o primeiro que precisar do dono cria a conta dele. */
    private String tokenDoDono() throws Exception {
        if (usuarioService.semUsuarios()) {
            postJson("/auth/primeiro-acesso", DONO).andExpect(status().isOk());
        }
        return entrar("dono", "senha-do-dono-1");
    }

    @Test
    @DisplayName("Primeiro acesso: só sem usuários, só do próprio computador, e já entra")
    void primeiroAcesso() throws Exception {
        if (usuarioService.semUsuarios()) {
            mvc.perform(get("/auth/opcoes")).andExpect(jsonPath("$.primeiroAcesso").value(true));

            // De outro aparelho da rede: recusado, mesmo sem nenhum usuário
            mvc.perform(post("/auth/primeiro-acesso").contentType(MediaType.APPLICATION_JSON).content(DONO)
                            .with(req -> { req.setRemoteAddr("192.168.1.30"); return req; }))
                    .andExpect(status().isForbidden());
            assertThat(usuarioService.semUsuarios()).isTrue();

            // Do próprio computador (o MockMvc usa 127.0.0.1): cria o ADMIN e devolve o token
            postJson("/auth/primeiro-acesso", DONO)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.usuario.perfil").value("ADMIN"));
        }

        // Com usuários, ninguém mais vira "dono" por aqui
        mvc.perform(get("/auth/opcoes")).andExpect(jsonPath("$.primeiroAcesso").value(false));
        postJson("/auth/primeiro-acesso", """
                {"nome":"Intruso","login":"intruso","senha":"senha-qualquer"}""")
                .andExpect(status().isBadRequest());
        assertThat(usuarioService.buscarPorLogin("intruso")).isEmpty();
    }

    @Test
    @DisplayName("O administrador cria uma senha nova para a recepção; a recepção não pode fazer o mesmo")
    void administradorRedefineSenha() throws Exception {
        String dono = tokenDoDono();
        String json = mvc.perform(post("/usuarios").header(HttpHeaders.AUTHORIZATION, dono).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Gabi","login":"gabi.rec","senha":"senha-antiga-1","perfil":"RECEPCAO"}"""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Integer idGabi = JsonPath.read(json, "$.id");

        mvc.perform(put("/usuarios/{id}/senha", idGabi).header(HttpHeaders.AUTHORIZATION, dono)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"novaSenha\":\"123\"}"))
                .andExpect(status().isBadRequest()); // curta demais
        mvc.perform(put("/usuarios/{id}/senha", idGabi).header(HttpHeaders.AUTHORIZATION, dono)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"novaSenha\":\"senha-nova-123\"}"))
                .andExpect(status().isNoContent());

        postJson("/auth/login", "{\"login\":\"gabi.rec\",\"senha\":\"senha-antiga-1\"}").andExpect(status().isUnauthorized());
        String gabi = entrar("gabi.rec", "senha-nova-123");

        // A recepção não troca a senha do dono
        Long idDono = usuarioService.buscarPorLogin("dono").orElseThrow().id();
        mvc.perform(put("/usuarios/{id}/senha", idDono).header(HttpHeaders.AUTHORIZATION, gabi)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"novaSenha\":\"tomei-a-conta\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Chave reserva do computador: troca a senha de quem existe e cria ADMIN para login novo")
    void chaveReservaDoComputador() throws Exception {
        tokenDoDono();

        assertThat(usuarioService.definirSenhaPeloComputador("Dono ", "ignorado", "senha-recuperada-1")).isFalse();
        entrar("dono", "senha-recuperada-1");
        usuarioService.definirSenhaPeloComputador("dono", "ignorado", "senha-do-dono-1"); // volta como estava

        assertThat(usuarioService.definirSenhaPeloComputador("leo.reserva", "Leonardo", "senha-do-leo-1")).isTrue();
        assertThat(usuarioService.buscarPorLogin("leo.reserva").orElseThrow().perfil()).isEqualTo(Perfil.ADMIN);
        entrar("leo.reserva", "senha-do-leo-1");
    }
}
