package br.com.leotricano.sistemasalao.controller;

import br.com.leotricano.sistemasalao.dto.RelatorioFaturamentoResponse;
import br.com.leotricano.sistemasalao.service.RelatorioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Endpoints de {@code /relatorios} (#17, RF07). Erros vêm do service e são montados pelo
 * {@code TratadorDeErros}.
 */
@RestController
@RequestMapping("/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    /**
     * {@code GET /relatorios/faturamento?de=2026-11-01&ate=2026-11-30} → fechamento do mês:
     * atendimentos, clientes atendidos, faturamento, ticket médio, recebido, totais por profissional
     * e por forma de pagamento. {@code &profissionalId=2} limita aos serviços desse profissional.
     * <ul>
     *   <li>400 se faltar data, se {@code de} for depois de {@code ate} ou se o período passar de um ano;</li>
     *   <li>404 se o profissional não existir.</li>
     * </ul>
     * Aqui as datas são {@code required = false} de propósito: quem confere a falta é o service,
     * para a mensagem de erro ser a mesma em português da consulta da agenda.
     */
    @GetMapping("/faturamento")
    public RelatorioFaturamentoResponse faturamento(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Long profissionalId) {
        return relatorioService.faturamento(de, ate, profissionalId);
    }
}
