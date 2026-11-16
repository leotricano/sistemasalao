package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.config.UsuarioLogado;
import br.com.leotricano.sistemasalao.dto.LoginRequest;
import br.com.leotricano.sistemasalao.dto.LoginResponse;
import br.com.leotricano.sistemasalao.dto.TrocarSenhaRequest;
import br.com.leotricano.sistemasalao.dto.UsuarioResponse;
import br.com.leotricano.sistemasalao.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de {@code /auth} (#18): entrar, saber quem está logado e trocar a própria senha.
 * {@code @AuthenticationPrincipal Jwt jwt} entrega o token já conferido pelo Spring Security.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** {@code POST /auth/login} → {@code {"login":"...","senha":"..."}}; 200 com o token ou 401. Única rota aberta. */
    @PostMapping("/login")
    public LoginResponse entrar(@Valid @RequestBody LoginRequest dados) {
        return usuarioService.entrar(dados);
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
