package br.com.leotricano.sistemasalao.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Monta objetos prontos para os testes de unidade (#26).
 *
 * <p>Fica no mesmo pacote das entidades ({@code model}) de propósito: os construtores de Agenda,
 * Cliente, Servico e Profissional são {@code protected}, e {@code protected} também libera o acesso
 * para classes do mesmo pacote. Assim os testes conseguem criar entidades sem mudar o código de produção.
 */
public final class Fabrica {

    private Fabrica() {}

    public static Cliente cliente(Long id, String nome) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setNome(nome);
        cliente.setEmail(nome.toLowerCase().replace(" ", ".") + "@email.com");
        cliente.setTelefone("21999990000");
        return cliente;
    }

    public static Servico servico(Long id, String nome, int duracaoMinutos, String preco) {
        Servico servico = new Servico();
        servico.setId(id);
        servico.setNome(nome);
        servico.setDuracaoMinutos(duracaoMinutos);
        servico.setPreco(new BigDecimal(preco));
        return servico;
    }

    /** Profissional que já faz os serviços informados (vínculo profissional_servico). */
    public static Profissional profissional(Long id, String nome, Servico... servicos) {
        Profissional profissional = new Profissional();
        profissional.setId(id);
        profissional.setNome(nome);
        profissional.setEspecialidade("Cabeleireiro");
        for (Servico servico : servicos) {
            profissional.getServicos().add(servico);
        }
        return profissional;
    }

    public static Agenda agenda(Long id, Cliente cliente, LocalDateTime dataHora, StatusAgendamento status) {
        Agenda agenda = new Agenda();
        agenda.setId(id);
        agenda.setCliente(cliente);
        agenda.setDataHora(dataHora);
        agenda.setStatus(status);
        return agenda;
    }

    /** Inclui um serviço na agenda com o preço atual do serviço, como o AgendaService faz. */
    public static ItemAgendamento item(Long id, Agenda agenda, Servico servico, Profissional profissional) {
        ItemAgendamento item = new ItemAgendamento();
        item.setId(id);
        item.setAgenda(agenda);
        item.setServico(servico);
        item.setProfissional(profissional);
        item.setPrecoCobrado(servico.getPreco());
        agenda.getItens().add(item);
        return item;
    }

    public static Pagamento pagamento(Long id, Agenda agenda, String valor, FormaPagamento forma) {
        Pagamento pagamento = new Pagamento();
        pagamento.setId(id);
        pagamento.setAgenda(agenda);
        pagamento.setValor(new BigDecimal(valor));
        pagamento.setFormaPagamento(forma);
        pagamento.setDataHora(LocalDateTime.now());
        return pagamento;
    }
}
