package br.com.leotricano.sistemasalao.service;

import br.com.leotricano.sistemasalao.dto.AgendaResponse;
import br.com.leotricano.sistemasalao.dto.HistoricoClienteResponse;
import br.com.leotricano.sistemasalao.exception.RecursoNaoEncontradoException;
import br.com.leotricano.sistemasalao.model.Cliente;
import br.com.leotricano.sistemasalao.model.StatusAgendamento;
import br.com.leotricano.sistemasalao.repository.AgendaRepository;
import br.com.leotricano.sistemasalao.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service = o tratamento dos dados. Fica entre o controller (HTTP) e o repository (banco), e é
 * onde moram as regras de negócio. Este cuida do cadastro de clientes (RF01) e do histórico de
 * atendimentos de cada um (RF06).
 *
 * <p>Diferente dos outros métodos, {@link #historico} já devolve um DTO: o resumo (o que conta
 * como atendimento, como somar o gasto) é regra de negócio, então é montado aqui e não no controller.
 *
 * <p>Convenção de erros de todos os services (#13): quando algo dá errado, o método lança uma
 * exceção do pacote {@code exception} (nunca devolve {@code null}). O {@code TratadorDeErros}
 * transforma a exceção na resposta HTTP certa.
 */
@Service // registra a classe no Spring, que cria uma instância e a injeta onde for pedida
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AgendaRepository agendaRepository; // usado pelo histórico (#16)

    /**
     * Injeção de dependência pelo construtor: o Spring vê que o service precisa de um
     * ClienteRepository e passa a instância que ele mesmo criou. Ninguém faz {@code new} aqui.
     */
    public ClienteService(ClienteRepository clienteRepository, AgendaRepository agendaRepository) {
        this.clienteRepository = clienteRepository;
        this.agendaRepository = agendaRepository;
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    /** Cria o cliente. O {@code save} faz INSERT quando o id é nulo e UPDATE quando já existe. */
    public Cliente salvar(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    /**
     * Remove o cliente.
     *
     * @throws RecursoNaoEncontradoException se o id não existir (404)
     * @throws org.springframework.dao.DataIntegrityViolationException se o cliente tiver agendamentos;
     *         o banco recusa por causa da chave estrangeira e o TratadorDeErros responde 409
     */
    public void deletar(Long id) {
        buscar(id); // garante o 404; o deleteById sozinho não avisa quando o id não existe
        clienteRepository.deleteById(id);
    }

    /**
     * Atualiza nome, e-mail e telefone de um cliente existente.
     * Busca o registro do banco e copia os campos novos para ele, em vez de salvar o objeto que
     * veio na requisição, para não depender de o JSON trazer o id certo.
     *
     * @throws RecursoNaoEncontradoException se o id não existir (404)
     */
    public Cliente atualizar(Long id, Cliente dadosNovos) {
        Cliente cliente = buscar(id);
        cliente.setNome(dadosNovos.getNome());
        cliente.setEmail(dadosNovos.getEmail());
        cliente.setTelefone(dadosNovos.getTelefone());

        return clienteRepository.save(cliente);
    }

    /**
     * Histórico de atendimentos do cliente (#16, RF06): todas as agendas dele, da mais recente para
     * a mais antiga, com um resumo no topo.
     *
     * <p>As agendas são buscadas uma vez só, todas. O resumo é calculado sobre as CONCLUÍDAS, e o
     * filtro de status vale só para a lista. Assim, pedir {@code ?status=CANCELADO} mostra os
     * cancelamentos sem zerar o total gasto do cliente.
     *
     * @param status se informado, a lista traz só agendas nesse status; {@code null} traz todas
     * @throws RecursoNaoEncontradoException se o cliente não existir (404). Cliente sem nenhuma
     *         agenda não é erro: volta lista vazia e totais zerados.
     */
    public HistoricoClienteResponse historico(Long id, StatusAgendamento status) {
        Cliente cliente = buscar(id);

        // Já vêm da mais recente para a mais antiga (ver o nome do método no AgendaRepository)
        List<AgendaResponse> agendas = agendaRepository.findByClienteIdOrderByDataHoraDescIdDesc(id).stream()
                .map(AgendaResponse::from)
                .toList();

        // Só o que foi de fato atendido entra no resumo
        List<AgendaResponse> concluidas = agendas.stream()
                .filter(agenda -> agenda.status() == StatusAgendamento.CONCLUIDO)
                .toList();

        // Como a lista está em ordem decrescente, a primeira concluída é a mais recente
        LocalDateTime ultimoAtendimento = concluidas.isEmpty() ? null : concluidas.get(0).dataHora();

        // Soma o total (precoCobrado dos itens) de cada agenda concluída
        BigDecimal totalGasto = concluidas.stream()
                .map(AgendaResponse::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // O filtro de status só recorta a lista, depois do resumo já calculado
        List<AgendaResponse> listaFiltrada = status == null
                ? agendas
                : agendas.stream().filter(agenda -> agenda.status() == status).toList();

        return new HistoricoClienteResponse(cliente.getId(), cliente.getNome(), concluidas.size(),
                ultimoAtendimento, totalGasto, listaFiltrada);
    }

    /**
     * Busca pelo id ou lança 404. {@code orElseThrow} abre o Optional: se tiver o cliente, devolve;
     * se estiver vazio, lança a exceção criada pela função passada.
     */
    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente " + id + " não encontrado."));
    }
}
