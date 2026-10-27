package br.com.leotricano.sistemasalao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação.
 *
 * <p>{@code @SpringBootApplication} liga três coisas de uma vez: a configuração automática do Spring
 * Boot (banco, servidor web, Jackson...), a leitura das configurações e a busca por componentes
 * ({@code @RestController}, {@code @Service}, {@code @Repository}, {@code @Configuration}) em todos os
 * pacotes abaixo de {@code br.com.leotricano.sistemasalao}. É por isso que as classes dos outros
 * pacotes são encontradas sem precisar registrá-las em lugar nenhum.
 */
@SpringBootApplication
public class SistemasalaoApplication {

	/** Sobe o servidor web embutido (porta 8080 por padrão) e conecta no PostgreSQL. */
	public static void main(String[] args) {
		SpringApplication.run(SistemasalaoApplication.class, args);
	}

}
