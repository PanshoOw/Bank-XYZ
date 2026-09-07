package com.duoc.bffatm.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.bffatm.dto.RetiroAtmRequest;
import com.duoc.bffatm.dto.RetiroAtmResponse;
import com.duoc.bffatm.dto.SaldoAtmResponse;
import com.duoc.bffatm.service.CuentaAtmService;

@RestController
@RequestMapping("/api/atm/cuentas")
public class AtmCuentaController {

    private final CuentaAtmService cuentaAtmService;

    public AtmCuentaController(
            CuentaAtmService cuentaAtmService) {

        this.cuentaAtmService = cuentaAtmService;
    }

    @GetMapping("/{id}/saldo")
    public SaldoAtmResponse obtenerSaldo(
            @PathVariable Integer id) {

        return cuentaAtmService.obtenerSaldo(id);
    }

    @PostMapping("/{id}/retiro")
    public RetiroAtmResponse retirar(
            @PathVariable Integer id,
            @RequestBody RetiroAtmRequest request) {

        return cuentaAtmService.retirar(id, request);
    }
}