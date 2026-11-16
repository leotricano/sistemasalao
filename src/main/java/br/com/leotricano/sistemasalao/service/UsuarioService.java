package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.dto.LoginRequest;
import br.com.leotricano.sistemasalao.dto.LoginResponse;
import br.com.leotricano.sistemasalao.dto.NovoUsuarioRequest;
import br.com.leotricano.sistemasalao.dto.UsuarioResponse;
import br.com.leotricano.sistemasalao.exception.NaoAutenticadoException;
import br.com.leotricano.sistemasalao.exception.RecursoNaoEncontradoException;
import br.com.leotricano.sistemasalao.exception.RegraNegocioException;
import br.com.leotricano.sistemasalao.model.Perfil;
import br.com.leotricano.sistemasalao.model.Profissional;
import br.com.leotricano.sistemasalao.model.Usuario;
import br.com.leotricano.sistemasalao.repository.ProfissionalRepository;
import br.com.leotricano.sistemasalao.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Login e cadastro de usuários (#18, RNF01).
 *
 * <p>O login é sempre guardado em minúsculas e sem espaços nas pontas, para "William " e
 * "william" serem o mesmo usuário.
 */
@Service
public class UsuarioService {

    /** Quanto tempo o token vale: um dia de trabalho no salão. Depois disso, é preciso entrar de novo. */
    private static final long HORAS_DE_VALIDADE = 12;

    /** Com "Manter conectado" (#43): 30 dias, para o celular de cada profissional não pedir senha todo dia. */
    private static final long DIAS_CONECTADO = 30;

    private final UsuarioRepository usuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, ProfissionalRepository profissionalRepository,
                          PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.profissionalRepository = profissionalRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    /**
     * Confere login e senha e gera o token. A mensagem de erro é a mesma para login inexistente e
     * senha errada, de propósito: dizer "esse login não existe" ajudaria quem tenta adivinhar.
     *
     * @throws NaoAutenticadoException (401) se o login não existir ou a senha não bater
     */
    public LoginResponse entrar(LoginRequest dados) {
        Usuario usuario = usuarioRepository.findByLogin(normalizar(dados.login()))
                .filter(u -> passwordEncoder.matches(dados.senha(), u.getSenhaHash()))
                .orElseThrow(() -> new NaoAutenticadoException("Login ou senha incorretos."));

        Instant agora = Instant.now();
        Instant expiraEm = Boolean.TRUE.equals(dados.manterConectado())
                ? agora.plus(DIAS_CONECTADO, ChronoUnit.DAYS)
                : agora.plus(HORAS_DE_VALIDADE, ChronoUnit.HOURS);

        // O que vai dentro do token. "sub" (subject) é o login; os outros campos são nossos.
        JwtClaimsSet.Builder campos = JwtClaimsSet.builder()
                .issuer("sistemasalao")
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .subject(usuario.getLogin())
                .claim("uid", usuario.getId())
                .claim("nome", usuario.getNome())
                .claim("perfil", usuario.getPerfil().name());
        if (usuario.getProfissional() != null) {
            campos.claim("profissionalId", usuario.getProfissional().getId());
        }

        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, campos.build())).getTokenValue();

        return new LoginResponse(token, expiraEm, UsuarioResponse.from(usuario));
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::from).toList();
    }

    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.from(buscarEntidade(id));
    }

    /**
     * Cria o acesso de alguém. Para PROFISSIONAL, liga o usuário ao profissional do salão, para que
     * ele veja só a própria agenda.
     *
     * @throws RegraNegocioException (400) se o login já existir, ou se faltar o profissional do perfil PROFISSIONAL
     */
    public UsuarioResponse criar(NovoUsuarioRequest dados) {
        String login = normalizar(dados.login());
        if (usuarioRepository.existsByLogin(login)) {
            throw new RegraNegocioException("Já existe um usuário com o login '" + login + "'.");
        }
        String email = emailOuNulo(dados.email());
        if (email != null && usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Já existe um usuário com o e-mail '" + email + "'.");
        }

        Profissional profissional = null;
        if (dados.perfil() == Perfil.PROFISSIONAL) {
            if (dados.profissionalId() == null) {
                throw new RegraNegocioException("Para o perfil PROFISSIONAL, informe qual profissional do salão é esta pessoa.");
            }
            profissional = profissionalRepository.findById(dados.profissionalId())
                    .orElseThrow(() -> new RegraNegocioException("Profissional " + dados.profissionalId() + " não encontrado."));
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dados.nome().trim());
        usuario.setLogin(login);
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(dados.senha()));
        usuario.setPerfil(dados.perfil());
        usuario.setProfissional(profissional);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /**
     * Troca a senha de quem está logado, pedindo a senha atual (se alguém deixar o computador
     * aberto, não consegue trocar a senha de outra pessoa).
     *
     * @throws RegraNegocioException (400) se a senha atual não bater
     */
    public void trocarSenha(Long usuarioId, String senhaAtual, String novaSenha) {
        Usuario usuario = buscarEntidade(usuarioId);
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new RegraNegocioException("A senha atual não confere.");
        }
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    /**
     * Cadastra, troca ou apaga (texto vazio) o e-mail de quem está logado (#42).
     *
     * @throws RegraNegocioException (400) se o e-mail já for de outro usuário
     */
    public UsuarioResponse atualizarEmail(Long usuarioId, String emailNovo) {
        Usuario usuario = buscarEntidade(usuarioId);
        String email = emailOuNulo(emailNovo);
        if (email != null) {
            usuarioRepository.findByEmailIgnoreCase(email)
                    .filter(outro -> !outro.getId().equals(usuarioId))
                    .ifPresent(outro -> {
                        throw new RegraNegocioException("Já existe um usuário com o e-mail '" + email + "'.");
                    });
        }
        usuario.setEmail(email);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /**
     * Remove um acesso.
     *
     * @throws RegraNegocioException (400) se for o próprio usuário logado: assim o salão nunca fica sem administrador por engano
     */
    public void remover(Long id, Long usuarioLogadoId) {
        if (id.equals(usuarioLogadoId)) {
            throw new RegraNegocioException("Você não pode remover o seu próprio acesso.");
        }
        usuarioRepository.delete(buscarEntidade(id));
    }

    /**
     * O administrador cria uma senha nova para alguém que esqueceu a dele (#46). Não pede a senha
     * antiga: quem esqueceu não sabe. Apaga também um código de "esqueci minha senha" pendente.
     */
    public void redefinirSenha(Long id, String novaSenha) {
        Usuario usuario = buscarEntidade(id);
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuario.setCodigoRecuperacaoHash(null);
        usuario.setCodigoRecuperacaoExpiraEm(null);
        usuario.setCodigoRecuperacaoTentativas(null);
        usuarioRepository.save(usuario);
    }

    /** {@code true} enquanto ninguém tem acesso: a tela de login mostra "Primeiro acesso" (#46). */
    public boolean semUsuarios() {
        return usuarioRepository.count() == 0;
    }

    /**
     * Primeiro acesso pela tela (#46): cria a conta do dono, só se o sistema ainda não tiver usuários.
     * {@code synchronized} impede que dois pedidos ao mesmo tempo criem dois "primeiros" donos.
     *
     * @throws RegraNegocioException (400) se já houver algum usuário
     */
    public synchronized UsuarioResponse criarPrimeiroAdministrador(String nome, String login, String senha) {
        if (!semUsuarios()) {
            throw new RegraNegocioException("O sistema já tem usuários. Entre com o seu login.");
        }
        return UsuarioResponse.from(salvarAdministrador(nome, login, senha));
    }

    /** Cria o primeiro administrador, se ainda não houver nenhum usuário. Ver {@code PrimeiroAcesso}. */
    public boolean criarAdministradorSeNaoHouverUsuarios(String login, String senha) {
        if (!semUsuarios()) {
            return false;
        }
        salvarAdministrador("Administrador", login, senha);
        return true;
    }

    /**
     * Usado pelo {@code redefinir-senha-dono.cmd}, que só roda no computador do salão (#46): se o
     * login existir, troca a senha; se não existir, cria um administrador com esse login.
     *
     * @param nome usado só quando o login não existe
     * @return {@code true} se criou uma conta nova, {@code false} se só trocou a senha
     */
    public boolean definirSenhaPeloComputador(String login, String nome, String senha) {
        var existente = usuarioRepository.findByLogin(normalizar(login));
        if (existente.isPresent()) {
            redefinirSenha(existente.get().getId(), senha);
            return false;
        }
        salvarAdministrador(nome, login, senha);
        return true;
    }

    /** Procura pelo login, para o script mostrar de quem é a conta antes de trocar a senha. */
    public Optional<UsuarioResponse> buscarPorLogin(String login) {
        return usuarioRepository.findByLogin(normalizar(login)).map(UsuarioResponse::from);
    }

    private Usuario salvarAdministrador(String nome, String login, String senha) {
        String loginLimpo = normalizar(login);
        if (usuarioRepository.existsByLogin(loginLimpo)) {
            throw new RegraNegocioException("Já existe um usuário com o login '" + loginLimpo + "'.");
        }
        Usuario admin = new Usuario();
        admin.setNome(nome.trim());
        admin.setLogin(loginLimpo);
        admin.setSenhaHash(passwordEncoder.encode(senha));
        admin.setPerfil(Perfil.ADMIN);
        return usuarioRepository.save(admin);
    }

    private Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + id + " não encontrado."));
    }

    /**
     * E-mail em minúsculas e sem espaços nas pontas; texto vazio vira {@code null} (sem e-mail).
     *
     * @throws RegraNegocioException (400) se, depois de limpo, não tiver formato de e-mail
     */
    static String emailOuNulo(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String limpo = email.trim().toLowerCase(Locale.ROOT);
        // Formato simples: algo@algo.algo, sem espaços. A confirmação de verdade é o código chegar.
        if (!limpo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new RegraNegocioException("E-mail inválido: " + email.trim());
        }
        return limpo;
    }

    private static String normalizar(String login) {
        return login.trim().toLowerCase(Locale.ROOT);
    }
}
