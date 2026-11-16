package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.exception.RegraNegocioException;
import br.com.leotricano.sistemasalao.model.Usuario;
import br.com.leotricano.sistemasalao.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

/**
 * "Esqueci minha senha" (#42): a pessoa informa o login ou o e-mail, recebe um código de 6
 * dígitos por e-mail e cria uma senha nova.
 *
 * <p><b>Cuidados de segurança:</b>
 * <ul>
 *   <li>A resposta do pedido é sempre a mesma, exista ou não o usuário: senão, qualquer um
 *       descobriria quais logins e e-mails estão cadastrados.</li>
 *   <li>O código é guardado só como hash BCrypt, vale 15 minutos e aceita 5 tentativas.</li>
 *   <li>Um código novo só é gerado um minuto depois do anterior, para ninguém encher a caixa de
 *       e-mail de alguém apertando o botão várias vezes.</li>
 * </ul>
 *
 * <p>O envio precisa de um servidor de e-mail (SMTP) configurado nas variáveis {@code MAIL_HOST},
 * {@code MAIL_USUARIO} e {@code MAIL_SENHA}. Sem elas, o pedido responde 400 explicando que a
 * recuperação por e-mail não está ligada.
 */
@Service
public class RecuperacaoSenhaService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacaoSenhaService.class);
    private static final int MINUTOS_DE_VALIDADE = 15;
    private static final int MAXIMO_TENTATIVAS = 5;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> enviadorDeEmail;
    private final boolean emailConfigurado;
    private final String remetente;
    private final SecureRandom sorteio = new SecureRandom();

    /**
     * O {@code ObjectProvider} pega o enviador de e-mail só se ele existir: sem {@code MAIL_HOST},
     * o Spring nem cria o JavaMailSender e a aplicação continua subindo normalmente.
     */
    public RecuperacaoSenhaService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                                   ObjectProvider<JavaMailSender> enviadorDeEmail,
                                   @Value("${spring.mail.host:}") String servidor,
                                   @Value("${app.email.remetente:}") String remetente,
                                   @Value("${spring.mail.username:}") String usuarioDoEmail) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.enviadorDeEmail = enviadorDeEmail;
        this.emailConfigurado = servidor != null && !servidor.isBlank();
        this.remetente = remetente == null || remetente.isBlank() ? usuarioDoEmail : remetente;
    }

    /** As telas usam para mostrar ou esconder o link "Esqueci minha senha". */
    public boolean emailConfigurado() {
        return emailConfigurado;
    }

    /**
     * Gera e envia o código, se o login/e-mail existir e tiver e-mail cadastrado. Se não existir,
     * não faz nada, sem avisar (ver Javadoc da classe).
     *
     * @throws RegraNegocioException (400) se o e-mail do salão não estiver configurado ou o envio falhar
     */
    public void enviarCodigo(String loginOuEmail) {
        if (!emailConfigurado) {
            throw new RegraNegocioException(
                    "A recuperação por e-mail não está ligada neste salão. Peça ao administrador para redefinir sua senha.");
        }

        Optional<Usuario> encontrado = buscar(loginOuEmail).filter(u -> u.getEmail() != null);
        if (encontrado.isEmpty()) {
            return;
        }
        Usuario usuario = encontrado.get();

        // Pedido repetido em menos de um minuto: o código anterior continua valendo
        Instant expira = usuario.getCodigoRecuperacaoExpiraEm();
        if (expira != null && expira.minus(MINUTOS_DE_VALIDADE - 1, ChronoUnit.MINUTES).isAfter(Instant.now())) {
            return;
        }

        String codigo = String.format("%06d", sorteio.nextInt(1_000_000)); // 000000 a 999999
        usuario.setCodigoRecuperacaoHash(passwordEncoder.encode(codigo));
        usuario.setCodigoRecuperacaoExpiraEm(Instant.now().plus(MINUTOS_DE_VALIDADE, ChronoUnit.MINUTES));
        usuario.setCodigoRecuperacaoTentativas(0);
        usuarioRepository.save(usuario);

        SimpleMailMessage mensagem = new SimpleMailMessage();
        if (remetente != null && !remetente.isBlank()) {
            mensagem.setFrom(remetente);
        }
        mensagem.setTo(usuario.getEmail());
        mensagem.setSubject("Código para criar uma senha nova no Tesoura");
        mensagem.setText("""
                Olá, %s!

                Seu código para criar uma senha nova no sistema do salão é:

                    %s

                Ele vale por %d minutos. Se não foi você quem pediu, ignore este e-mail: sua senha continua a mesma.
                """.formatted(usuario.getNome(), codigo, MINUTOS_DE_VALIDADE));

        try {
            enviadorDeEmail.getObject().send(mensagem);
        } catch (MailException e) {
            log.error("Falha ao enviar o código de recuperação para o usuário {}", usuario.getLogin(), e);
            throw new RegraNegocioException("Não foi possível enviar o e-mail agora. Tente de novo em alguns minutos.");
        }
    }

    /**
     * Confere o código e troca a senha. Depois de usado, o código é apagado.
     *
     * @throws RegraNegocioException (400) se o código estiver errado, vencido ou esgotado
     */
    public void redefinirSenha(String loginOuEmail, String codigo, String novaSenha) {
        RegraNegocioException invalido = new RegraNegocioException(
                "Código inválido ou vencido. Peça um código novo.");

        Usuario usuario = buscar(loginOuEmail)
                .filter(u -> u.getCodigoRecuperacaoHash() != null)
                .orElseThrow(() -> invalido);

        boolean vencido = usuario.getCodigoRecuperacaoExpiraEm().isBefore(Instant.now());
        int tentativas = usuario.getCodigoRecuperacaoTentativas() == null ? 0 : usuario.getCodigoRecuperacaoTentativas();
        if (vencido || tentativas >= MAXIMO_TENTATIVAS) {
            limparCodigo(usuario);
            throw invalido;
        }

        if (!passwordEncoder.matches(codigo.trim(), usuario.getCodigoRecuperacaoHash())) {
            usuario.setCodigoRecuperacaoTentativas(tentativas + 1);
            usuarioRepository.save(usuario);
            throw invalido;
        }

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        limparCodigo(usuario);
    }

    private void limparCodigo(Usuario usuario) {
        usuario.setCodigoRecuperacaoHash(null);
        usuario.setCodigoRecuperacaoExpiraEm(null);
        usuario.setCodigoRecuperacaoTentativas(null);
        usuarioRepository.save(usuario);
    }

    /** Procura pelo e-mail (se tiver @) ou pelo login. */
    private Optional<Usuario> buscar(String loginOuEmail) {
        String texto = loginOuEmail.trim().toLowerCase(Locale.ROOT);
        return texto.contains("@")
                ? usuarioRepository.findByEmailIgnoreCase(texto)
                : usuarioRepository.findByLogin(texto);
    }
}
