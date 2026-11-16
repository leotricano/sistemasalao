package br.com.leotricano.sistemasalao.config;

import br.com.leotricano.sistemasalao.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Garante que dá para entrar no sistema na primeira vez (#18, #46). Um {@code ApplicationRunner}
 * roda uma vez, logo depois que a aplicação sobe.
 *
 * <p>Se o banco ainda não tem nenhum usuário:
 * <ul>
 *   <li>com {@code ADMIN_SENHA} definida, cria o administrador com o login de {@code ADMIN_LOGIN}
 *       (padrão "admin") e essa senha;</li>
 *   <li>sem ela, não cria nada: a tela de login mostra "Criar a conta do dono", que só funciona
 *       no computador onde o sistema roda (ver {@code AuthController.primeiroAcesso}).</li>
 * </ul>
 */
@Component
public class PrimeiroAcesso implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PrimeiroAcesso.class);

    private final UsuarioService usuarioService;
    private final String login;
    private final String senha;

    public PrimeiroAcesso(UsuarioService usuarioService,
                          @Value("${app.admin.login:admin}") String login,
                          @Value("${app.admin.senha:}") String senha) {
        this.usuarioService = usuarioService;
        this.login = login;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption(RedefinirSenhaPeloComputador.OPCAO)) {
            return; // modo do redefinir-senha-dono.cmd: ele mesmo cuida das contas
        }
        if (senha != null && !senha.isBlank()) {
            if (usuarioService.criarAdministradorSeNaoHouverUsuarios(login, senha)) {
                log.info("Primeiro acesso: administrador '{}' criado com a senha de ADMIN_SENHA.", login);
            }
        } else if (usuarioService.semUsuarios()) {
            log.warn("Nenhum usuário ainda. Abra http://localhost:8080 NESTE computador para criar a conta do dono.");
        }
    }
}
