package br.com.leotricano.sistemasalao.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Segurança da API (#18, RNF01): login com token JWT e o que cada perfil pode fazer.
 *
 * <p><b>Como funciona o login com token:</b>
 * <ol>
 *   <li>{@code POST /auth/login} confere login e senha e devolve um <i>token JWT</i>: um texto
 *       assinado que diz "este é o usuário X, perfil Y, válido até tal hora".</li>
 *   <li>O frontend manda esse token em toda requisição: {@code Authorization: Bearer <token>}.</li>
 *   <li>Aqui, antes de chegar ao controller, o Spring confere a assinatura do token. Se alguém
 *       alterar uma letra (por exemplo, trocar o perfil para ADMIN), a assinatura não bate e a
 *       requisição é recusada.</li>
 * </ol>
 * A API não guarda sessão ({@code STATELESS}): tudo o que ela precisa saber vem no token.
 *
 * <p><b>Quem pode o quê</b> (a primeira regra que combinar com a requisição vale):
 * <pre>
 *   POST /auth/login, telas do frontend          qualquer um
 *   esqueci a senha, opções do login (#42)       qualquer um
 *   /auth/** (quem sou eu, trocar senha)         qualquer usuário logado
 *   /relatorios/**, /usuarios/**                 ADMIN
 *   GET /agendas e /agendas/{id}                 qualquer usuário logado (o PROFISSIONAL só vê as dele)
 *   GET /servicos/**, /profissionais/**          ADMIN e RECEPCAO
 *   alterar /servicos/**, /profissionais/**      ADMIN
 *   DELETE /clientes/**                          ADMIN
 *   todo o resto                                 ADMIN e RECEPCAO
 * </pre>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /** Telas do frontend, servidas pela própria API depois do {@code npm run build}. */
    private static final String[] TELAS = {"/", "/index.html", "/assets/**", "/favicon.svg", "/error",
            "/manifest.json", "/icone-*.png", "/apple-touch-icon.png"}; // atalho no celular (#43)

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/esqueci-senha", "/auth/redefinir-senha").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/opcoes").permitAll()
                        .requestMatchers(TELAS).permitAll()
                        .requestMatchers("/auth/**").authenticated()
                        .requestMatchers("/relatorios/**", "/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/agendas", "/agendas/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/servicos/**", "/profissionais/**").hasAnyRole("ADMIN", "RECEPCAO")
                        .requestMatchers("/servicos/**", "/profissionais/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/clientes/**").hasRole("ADMIN")
                        .anyRequest().hasAnyRole("ADMIN", "RECEPCAO")
                )
                // Lê o token do cabeçalho Authorization e confere a assinatura
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(perfilDoToken()))
                        .authenticationEntryPoint((req, res, e) ->
                                responderErro(res, 401, "Entre no sistema para continuar (token ausente, inválido ou vencido).")))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint((req, res, e) ->
                                responderErro(res, 401, "Entre no sistema para continuar."))
                        .accessDeniedHandler((req, res, e) ->
                                responderErro(res, 403, "Seu perfil não tem permissão para esta ação.")))
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF protege sessões guardadas em cookie. Aqui não há cookie: o token vai no cabeçalho.
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Transforma o campo {@code perfil} do token (ex.: "ADMIN") na permissão "ROLE_ADMIN", que é o
     * que o {@code hasRole("ADMIN")} das regras acima procura.
     */
    private JwtAuthenticationConverter perfilDoToken() {
        JwtGrantedAuthoritiesConverter permissoes = new JwtGrantedAuthoritiesConverter();
        permissoes.setAuthoritiesClaimName("perfil");
        permissoes.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(permissoes);
        return conversor;
    }

    /**
     * Chave que assina e confere os tokens (algoritmo HMAC-SHA256). Vem da variável de ambiente
     * {@code JWT_SECRET}. O SHA-256 transforma qualquer frase numa chave de 32 bytes, o tamanho
     * que o algoritmo exige. Sem a variável, sorteia uma chave: funciona, mas todos os tokens
     * deixam de valer quando a aplicação reinicia.
     */
    @Bean
    public SecretKey chaveJwt(@Value("${app.jwt.secret:}") String segredo) throws NoSuchAlgorithmException {
        byte[] bytes;
        if (segredo == null || segredo.isBlank()) {
            log.warn("JWT_SECRET não definida: usando uma chave sorteada. Os logins valem só até reiniciar a aplicação.");
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = MessageDigest.getInstance("SHA-256").digest(segredo.getBytes(StandardCharsets.UTF_8));
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /** Gera tokens (usado pelo AuthService no login). */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    /** Confere tokens (usado pelo Spring em toda requisição). */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey chaveJwt) {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** BCrypt: transforma a senha num hash que não pode ser desfeito (ver Usuario). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Erros de login e permissão acontecem antes do controller, então o TratadorDeErros não os vê.
     * Este método escreve a resposta no mesmo formato {@code {"status":..., "mensagem":...}}.
     */
    private static void responderErro(HttpServletResponse res, int status, String mensagem) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"mensagem\":\"" + mensagem + "\"}");
    }
}
