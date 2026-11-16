package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.config.UsuarioLogado;
import br.com.leotricano.sistemasalao.dto.NovaSenhaRequest;
import br.com.leotricano.sistemasalao.dto.NovoUsuarioRequest;
import br.com.leotricano.sistemasalao.dto.UsuarioResponse;
import br.com.leotricano.sistemasalao.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints de {@code /usuarios} (#18): o dono do salão cria e remove acessos. Só ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** {@code GET /usuarios} → lista os acessos (sem senhas). */
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    /**
     * {@code POST /usuarios} → cria um acesso. Exemplo:
     * {@code {"nome":"William","login":"william","senha":"...","perfil":"PROFISSIONAL","profissionalId":2}}.
     * 400 se o login já existir ou faltar o profissional.
     */
    @PostMapping
    public UsuarioResponse criar(@Valid @RequestBody NovoUsuarioRequest dados) {
        return usuarioService.criar(dados);
    }

    /**
     * {@code PUT /usuarios/{id}/senha} → {@code {"novaSenha":"..."}}; o administrador cria uma senha
     * nova para quem esqueceu a dele (#46). 204, ou 404 se o usuário não existir.
     */
    @PutMapping("/{id}/senha")
    public ResponseEntity<Void> redefinirSenha(@PathVariable Long id, @Valid @RequestBody NovaSenhaRequest dados) {
        usuarioService.redefinirSenha(id, dados.novaSenha());
        return ResponseEntity.noContent().build();
    }

    /** {@code DELETE /usuarios/{id}} → remove o acesso; 204, 404 se não existir, 400 se for o próprio. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        usuarioService.remover(id, UsuarioLogado.de(jwt).usuarioId());
        return ResponseEntity.noContent().build();
    }
}
