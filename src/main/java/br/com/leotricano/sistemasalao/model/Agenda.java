package br.com.leotricano.sistemasalao.model;

import jakarta.persistence.*;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Um agendamento: um cliente, num dia e horário, com um ou mais serviços (RF03). Tabela {@code agenda}.
 *
 * <p>Os serviços ficam em {@link ItemAgendamento}, e cada item tem o seu profissional. Assim, uma
 * mesma visita pode ter corte com um cabeleireiro e pedicure com uma manicure.
 * Os pagamentos ficam em {@link Pagamento}, que aponta para esta agenda.
 */
@Entity
public class Agenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Muitas agendas para um cliente. No banco vira a coluna {@code cliente_id} (chave estrangeira). */
    @ManyToOne
    @NotNull
    private Cliente cliente;

    /** Início do atendimento. O término não é guardado: é calculado por {@link #calcularFim()}. */
    @NotNull
    private LocalDateTime dataHora;

    /**
     * Grava o nome do enum ("AGENDADO") em vez da posição (0, 1, 2), que quebraria se a ordem mudasse.
     * Começa como AGENDADO: o JSON de criação não precisa mandar o status, e o AgendaService ignora
     * qualquer outro valor enviado. Depois, só muda pelas transições permitidas (ver StatusAgendamento).
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    private StatusAgendamento status = StatusAgendamento.AGENDADO;

    /**
     * Serviços deste agendamento.
     * <ul>
     *   <li>{@code mappedBy = "agenda"}: quem guarda a ligação no banco é o campo {@code agenda} do item
     *       (coluna {@code agenda_id} na tabela {@code item_agendamento}).</li>
     *   <li>{@code cascade = ALL}: salvar ou apagar a agenda salva ou apaga os itens junto.</li>
     *   <li>{@code orphanRemoval = true}: tirar um item desta lista apaga o item do banco.</li>
     * </ul>
     * Atenção: o item aponta de volta para a agenda. Devolver esta entidade direto como JSON causava
     * recursão infinita; por isso os controllers devolvem {@code AgendaResponse}.
     */
    @OneToMany(mappedBy = "agenda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemAgendamento> itens = new ArrayList<>();

    /** Construtor vazio exigido pelo JPA e pelo Jackson. */
    protected Agenda() {}

    /**
     * Soma a duração de todos os serviços desta agenda. Ex.: corte (60) + coloração (120) = 180.
     * Os serviços acontecem em sequência, então a agenda inteira ocupa os profissionais dela
     * durante esse tempo todo.
     */
    public int calcularDuracaoTotalMinutos() {
        return itens.stream()
                .mapToInt(item -> item.getServico().getDuracaoMinutos())
                .sum();
    }

    /**
     * Horário de término: início + duração total. Ex.: 14:00 + 180 min = 17:00.
     * É usado na regra de conflito de horário (#12) e devolvido no JSON como {@code dataHoraFim}.
     */
    public LocalDateTime calcularFim() {
        return dataHora.plusMinutes(calcularDuracaoTotalMinutos());
    }

    // Getters e setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public StatusAgendamento getStatus() {
        return status;
    }

    public void setStatus(StatusAgendamento status) {
        this.status = status;
    }

    public List<ItemAgendamento> getItens() {
        return itens;
    }

    public void setItens(List<ItemAgendamento> itens) {
        this.itens = itens;
    }
}
