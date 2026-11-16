package br.com.leotricano.sistemasalao.config;

import br.com.leotricano.sistemasalao.dto.UsuarioResponse;
import br.com.leotricano.sistemasalao.service.UsuarioService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.Console;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Modo "chave reserva" do sistema (#46), usado pelo {@code redefinir-senha-dono.cmd}.
 *
 * <p>Quando a aplicação sobe com {@code --redefinir-senha} (ou {@code --redefinir-senha=login}),
 * ela não abre o site: pergunta no terminal o login e a senha nova, grava no banco e fecha.
 * Se o login não existir, cria um administrador com ele.
 *
 * <p><b>Por que isso é seguro:</b> só roda no computador onde o sistema está instalado, e precisa
 * da senha do banco ({@code DB_PASSWORD}), que só esse computador tem. É como o botão de "reset"
 * atrás do roteador: quem está fisicamente ali já tem acesso a tudo de qualquer jeito.
 */
@Component
public class RedefinirSenhaPeloComputador implements ApplicationRunner {

    public static final String OPCAO = "redefinir-senha";

    private final UsuarioService usuarioService;
    private final ConfigurableApplicationContext contexto;

    public RedefinirSenhaPeloComputador(UsuarioService usuarioService, ConfigurableApplicationContext contexto) {
        this.usuarioService = usuarioService;
        this.contexto = contexto;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(OPCAO)) {
            return; // subida normal do sistema
        }
        int codigoDeSaida = executar(args.getOptionValues(OPCAO));
        System.exit(SpringApplication.exit(contexto, () -> codigoDeSaida));
    }

    private int executar(List<String> valores) {
        Console console = System.console();
        if (console == null) {
            System.err.println("Rode pelo redefinir-senha-dono.cmd, numa janela do Prompt de Comando.");
            return 1;
        }
        console.printf("%n=== Redefinir senha de administrador (Tesoura) ===%n%n");

        String login = valores == null || valores.isEmpty() || valores.get(0).isBlank()
                ? console.readLine("Login: ")
                : valores.get(0);
        if (login == null || login.isBlank()) {
            console.printf("Nenhum login informado. Nada foi alterado.%n");
            return 1;
        }

        Optional<UsuarioResponse> existente = usuarioService.buscarPorLogin(login);
        String nome;
        if (existente.isPresent()) {
            nome = existente.get().nome();
            console.printf("Conta encontrada: %s (%s).%n", nome, existente.get().perfil());
        } else {
            console.printf("O login '%s' ainda não existe. Vou criar um ADMINISTRADOR com ele.%n", login.trim());
            nome = console.readLine("Nome da pessoa: ");
            if (nome == null || nome.isBlank()) {
                console.printf("Nome em branco. Nada foi alterado.%n");
                return 1;
            }
        }

        char[] senha = console.readPassword("Senha nova (não aparece ao digitar): ");
        char[] repetida = console.readPassword("Repita a senha nova: ");
        try {
            if (senha == null || senha.length < 6) {
                console.printf("A senha precisa ter pelo menos 6 caracteres. Nada foi alterado.%n");
                return 1;
            }
            if (!Arrays.equals(senha, repetida)) {
                console.printf("As duas senhas não são iguais. Nada foi alterado.%n");
                return 1;
            }
            boolean criou = usuarioService.definirSenhaPeloComputador(login, nome, new String(senha));
            console.printf(criou
                    ? "%nPronto: administrador '%s' criado. Entre no sistema com essa senha.%n"
                    : "%nPronto: a senha de '%s' foi trocada. Entre no sistema com a senha nova.%n", login.trim().toLowerCase());
            return 0;
        } finally {
            // Apaga a senha da memória assim que não precisa mais dela
            if (senha != null) Arrays.fill(senha, ' ');
            if (repetida != null) Arrays.fill(repetida, ' ');
        }
    }
}
