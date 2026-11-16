package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.config.UsuarioLogado;
import br.com.leotricano.sistemasalao.dto.AtualizarContaRequest;
import br.com.leotricano.sistemasalao.dto.EsqueciSenhaRequest;
import br.com.leotricano.sistemasalao.dto.LoginRequest;
import br.com.leotricano.sistemasalao.dto.LoginResponse;
import br.com.leotricano.sistemasalao.dto.PrimeiroAcessoRequest;
import br.com.leotricano.sistemasalao.dto.RedefinirSenhaRequest;
import br.com.leotricano.sistemasalao.dto.TrocarSenhaRequest;
import br.com.leotricano.sistemasalao.dto.UsuarioResponse;
import br.com.leotricano.sistemasalao.exception.AcessoNegadoException;
import br.com.leotricano.sistemasalao.service.RecuperacaoSenhaService;
import br.com.leotricano.sistemasalao.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * Endpoints de {@code /auth} (#18): entrar, saber quem está logado, trocar a própria senha e
 * recuperar a senha esquecida por e-mail (#42).
 * {@code @AuthenticationPrincipal Jwt jwt} entrega o token já conferido pelo Spring Security.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

    public AuthController(UsuarioService usuarioService, RecuperacaoSenhaService recuperacaoSenhaService) {
        this.usuarioService = usuarioService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
    }

    /** {@code POST /auth/login} → {@code {"login":"...","senha":"..."}}; 200 com o token ou 401. Única rota aberta. */
    @PostMapping("/login")
    public LoginResponse entrar(@Valid @RequestBody LoginRequest dados) {
        return usuarioService.entrar(dados);
    }

    /**
     * {@code GET /auth/opcoes} → o que a tela de login deve mostrar. Aberta, sem login.
     * Ex.: {@code {"recuperacaoPorEmail": true}} mostra o link "Esqueci minha senha".
     */
    @GetMapping("/opcoes")
    public Map<String, Boolean> opcoes() {
        return Map.of("recuperacaoPorEmail", recuperacaoSenhaService.emailConfigurado(),
                "primeiroAcesso", usuarioService.semUsuarios()); // #46: mostra a tela "Criar a conta do dono"
    }

    /**
     * {@code POST /auth/primeiro-acesso} → {@code {"nome","login","senha"}}; cria a conta do dono e
     * já entra (#46). Só funciona enquanto o sistema não tem nenhum usuário, e só pedido do próprio
     * computador onde o sistema roda (endereço 127.0.0.1): assim ninguém na rede do salão, ou de
     * fora, consegue se adiantar e virar "dono" de um sistema recém-instalado.
     *
     * @throws AcessoNegadoException (403) se o pedido vier de outro aparelho
     */
    @PostMapping("/primeiro-acesso")
    public LoginResponse primeiroAcesso(@Valid @RequestBody PrimeiroAcessoRequest dados, HttpServletRequest requisicao) {
        if (!doProprioComputador(requisicao)) {
            throw new AcessoNegadoException(
                    "A conta do dono só pode ser criada no computador onde o sistema está instalado. Abra http://localhost:8080 nele.");
        }
        usuarioService.criarPrimeiroAdministrador(dados.nome(), dados.login(), dados.senha());
        return usuarioService.entrar(new LoginRequest(dados.login(), dados.senha(), true));
    }

    private static boolean doProprioComputador(HttpServletRequest requisicao) {
        try {
            return InetAddress.getByName(requisicao.getRemoteAddr()).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * {@code POST /auth/esqueci-senha} → {@code {"loginOuEmail":"..."}}; manda o código por e-mail.
     * Responde 204 sempre que o e-mail do salão está configurado, existindo ou não o usuário (#42).
     */
    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest dados) {
        recuperacaoSenhaService.enviarCodigo(dados.loginOuEmail());
        return ResponseEntity.noContent().build();
    }

    /** {@code POST /auth/redefinir-senha} → {@code {"loginOuEmail","codigo","novaSenha"}}; 204, ou 400 se o código não servir. */
    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest dados) {
        recuperacaoSenhaService.redefinirSenha(dados.loginOuEmail(), dados.codigo(), dados.novaSenha());
        return ResponseEntity.noContent().build();
    }

    /** {@code PUT /auth/me} → {@code {"email":"..."}}; cadastra ou troca o próprio e-mail (#42). */
    @PutMapping("/me")
    public UsuarioResponse atualizarConta(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AtualizarContaRequest dados) {
        return usuarioService.atualizarEmail(UsuarioLogado.de(jwt).usuarioId(), dados.email());
    }

    /** {@code GET /auth/me} → dados de quem está logado (o frontend usa ao abrir). */
    @GetMapping("/me")
    public UsuarioResponse eu(@AuthenticationPrincipal Jwt jwt) {
        return usuarioService.buscar(UsuarioLogado.de(jwt).usuarioId());
    }

    /** {@code PUT /auth/senha} → {@code {"senhaAtual":"...","novaSenha":"..."}}; 204, ou 400 se a senha atual não bater. */
    @PutMapping("/senha")
    public ResponseEntity<Void> trocarSenha(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TrocarSenhaRequest dados) {
        usuarioService.trocarSenha(UsuarioLogado.de(jwt).usuarioId(), dados.senhaAtual(), dados.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
