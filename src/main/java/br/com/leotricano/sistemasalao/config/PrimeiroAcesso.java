package br.com.leotricano.sistemasalao.config;

import br.com.leotricano.sistemasalao.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Garante que dá para entrar no sistema na primeira vez (#18). Um {@code ApplicationRunner} roda
 * uma vez, logo depois que a aplicação sobe.
 *
 * <p>Se o banco ainda não tem nenhum usuário, cria o administrador com o login de
 * {@code ADMIN_LOGIN} (padrão "admin") e a senha de {@code ADMIN_SENHA}. Sem {@code ADMIN_SENHA},
 * sorteia uma senha e mostra no console uma única vez. Nos dois casos, troque a senha no primeiro acesso.
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
        boolean sorteada = senha == null || senha.isBlank();
        String senhaInicial = sorteada ? sortearSenha() : senha;

        if (usuarioService.criarAdministradorSeNaoHouverUsuarios(login, senhaInicial)) {
            if (sorteada) {
                log.warn("Primeiro acesso: administrador criado. Login: {}  Senha: {}  (anote e troque no primeiro acesso)",
                        login, senhaInicial);
            } else {
                log.info("Primeiro acesso: administrador '{}' criado com a senha de ADMIN_SENHA.", login);
            }
        }
    }

    /** 10 caracteres sem os que se confundem (0/O, 1/l/I). */
    private static String sortearSenha() {
        String letras = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789";
        SecureRandom sorteio = new SecureRandom();
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            senha.append(letras.charAt(sorteio.nextInt(letras.length())));
        }
        return senha.toString();
    }
}
