package br.com.leotricano.sistemasalao.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração do "esqueci minha senha" (#42).
 *
 * <p>O {@code @MockitoBean} troca o enviador de e-mail de verdade por um mock: nenhum e-mail sai,
 * mas o teste consegue pegar a mensagem que <i>seria</i> enviada e ler o código dentro dela, como
 * a pessoa faria na caixa de entrada.
 */
@SpringBootTest
class RecuperacaoSenhaTest {

    @Autowired
    private WebApplicationContext contexto;

    @MockitoBean
    private JavaMailSender enviadorDeEmail;

    private MockMvc mvc;

    @BeforeEach
    void montarMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        reset(enviadorDeEmail);
    }

    // ---------- ajudantes ----------

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String entrar(String login, String senha) throws Exception {
        String json = postJson("/auth/login", "{\"login\":\"%s\",\"senha\":\"%s\"}".formatted(login, senha))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.token");
    }

    /** O dono cria um acesso de recepção com o e-mail informado. */
    private void criarUsuario(String login, String email) throws Exception {
        String admin = entrar("admin", "senha-de-teste-123");
        mvc.perform(post("/usuarios").header(HttpHeaders.AUTHORIZATION, admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Teste %s","login":"%s","email":%s,"senha":"senha-antiga-1","perfil":"RECEPCAO"}
                                """.formatted(login, login, email == null ? "null" : "\"" + email + "\"")))
                .andExpect(status().isOk());
    }

    /** Pega o último e-mail "enviado" pelo mock e tira o código de 6 dígitos dele. */
    private SimpleMailMessage ultimoEmail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(enviadorDeEmail, atLeastOnce()).send(captor.capture());
        return captor.getValue();
    }

    private String codigoDo(SimpleMailMessage email) {
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(email.getText());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    // ---------- testes ----------

    @Test
    @DisplayName("Fluxo completo: pede o código pelo e-mail, erra uma vez, acerta e entra com a senha nova")
    void fluxoCompleto() throws Exception {
        criarUsuario("carol.rec", "carol@salao.com");

        mvc.perform(get("/auth/opcoes")).andExpect(status().isOk()).andExpect(jsonPath("$.recuperacaoPorEmail").value(true));

        postJson("/auth/esqueci-senha", "{\"loginOuEmail\":\"Carol@Salao.com\"}").andExpect(status().isNoContent());
        SimpleMailMessage email = ultimoEmail();
        assertThat(email.getTo()).containsExactly("carol@salao.com");
        String codigo = codigoDo(email);
        String errado = codigo.equals("000000") ? "111111" : "000000";

        postJson("/auth/redefinir-senha", """
                {"loginOuEmail":"carol.rec","codigo":"%s","novaSenha":"senha-nova-123"}""".formatted(errado))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Código inválido ou vencido. Peça um código novo."));

        postJson("/auth/redefinir-senha", """
                {"loginOuEmail":"carol.rec","codigo":"%s","novaSenha":"senha-nova-123"}""".formatted(codigo))
                .andExpect(status().isNoContent());

        postJson("/auth/login", "{\"login\":\"carol.rec\",\"senha\":\"senha-antiga-1\"}").andExpect(status().isUnauthorized());
        entrar("carol.rec", "senha-nova-123");

        // O código já foi usado e não serve de novo
        postJson("/auth/redefinir-senha", """
                {"loginOuEmail":"carol.rec","codigo":"%s","novaSenha":"outra-senha-1"}""".formatted(codigo))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Login inexistente ou sem e-mail: responde igual (204) e não manda e-mail nenhum")
    void naoRevelaQuemExiste() throws Exception {
        criarUsuario("sem.email", null);

        postJson("/auth/esqueci-senha", "{\"loginOuEmail\":\"ninguem-com-esse-login\"}").andExpect(status().isNoContent());
        postJson("/auth/esqueci-senha", "{\"loginOuEmail\":\"sem.email\"}").andExpect(status().isNoContent());

        verify(enviadorDeEmail, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Depois de 5 tentativas erradas, nem o código certo funciona")
    void bloqueiaDepoisDeCincoErros() throws Exception {
        criarUsuario("davi.rec", "davi@salao.com");
        postJson("/auth/esqueci-senha", "{\"loginOuEmail\":\"davi.rec\"}").andExpect(status().isNoContent());
        String codigo = codigoDo(ultimoEmail());
        String errado = codigo.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            postJson("/auth/redefinir-senha", """
                    {"loginOuEmail":"davi.rec","codigo":"%s","novaSenha":"senha-nova-123"}""".formatted(errado))
                    .andExpect(status().isBadRequest());
        }
        postJson("/auth/redefinir-senha", """
                {"loginOuEmail":"davi.rec","codigo":"%s","novaSenha":"senha-nova-123"}""".formatted(codigo))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A pessoa logada cadastra o próprio e-mail; e-mail de outro usuário dá 400")
    void cadastraOProprioEmail() throws Exception {
        criarUsuario("eva.rec", null);
        criarUsuario("fabi.rec", "fabi@salao.com");
        String eva = entrar("eva.rec", "senha-antiga-1");

        mvc.perform(put("/auth/me").header(HttpHeaders.AUTHORIZATION, eva).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Eva@Salao.com \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("eva@salao.com"));

        mvc.perform(put("/auth/me").header(HttpHeaders.AUTHORIZATION, eva).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"fabi@salao.com\"}"))
                .andExpect(status().isBadRequest());
    }
}
