package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.config.UsuarioLogado;
import br.com.leotricano.sistemasalao.dto.AtualizarContaRequest;
import br.com.leotricano.sistemasalao.dto.EsqueciSenhaRequest;
import br.com.leotricano.sistemasalao.dto.LoginRequest;
import br.com.leotricano.sistemasalao.dto.LoginResponse;
import br.com.leotricano.sistemasalao.dto.RedefinirSenhaRequest;
import br.com.leotricano.sistemasalao.dto.TrocarSenhaRequest;
import br.com.leotricano.sistemasalao.dto.UsuarioResponse;
import br.com.leotricano.sistemasalao.service.RecuperacaoSenhaService;
import br.com.leotricano.sistemasalao.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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
        return Map.of("recuperacaoPorEmail", recuperacaoSenhaService.emailConfigurado());
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
