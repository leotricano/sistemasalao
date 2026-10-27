package br.com.leotricano.sistemasalao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração do Spring Security.
 *
 * <p>Só por ter o {@code spring-boot-starter-security} no {@code pom.xml}, o Spring bloqueia todos os
 * endpoints e pede login. Esta classe substitui esse comportamento padrão.
 *
 * <p><b>Estado atual: temporário.</b> Tudo está liberado para facilitar os testes no Postman.
 * TODO(#18): implementar autenticação de verdade (RNF01), com perfis de administrador e profissional.
 */
@Configuration // diz ao Spring que esta classe contém configurações
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Monta a "corrente de filtros" por onde toda requisição HTTP passa antes de chegar ao controller.
     * O {@code @Bean} entrega esta versão ao Spring, que passa a usá-la no lugar da padrão.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // qualquer requisição passa, sem login
                )

                // CSRF protege formulários HTML de sites; numa API REST testada pelo Postman ele
                // bloqueia todo POST/PUT/DELETE, então fica desligado.
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
