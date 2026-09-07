package com.duoc.bffmobile.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.bffmobile.dto.CuentaMobileResponse;
import com.duoc.bffmobile.service.CuentaMobileService;

@RestController
@RequestMapping("/api/mobile/cuentas")
public class MobileCuentaController {

    private final CuentaMobileService cuentaMobileService;

    public MobileCuentaController(
            CuentaMobileService cuentaMobileService) {

        this.cuentaMobileService = cuentaMobileService;
    }

    @GetMapping
    public List<CuentaMobileResponse> obtenerCuentas() {
        return cuentaMobileService.obtenerCuentas();
    }

    @GetMapping("/{id}")
    public CuentaMobileResponse obtenerCuenta(
            @PathVariable Integer id) {

        return cuentaMobileService.obtenerCuenta(id);
    }
}