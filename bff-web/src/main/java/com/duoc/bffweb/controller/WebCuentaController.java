package com.duoc.bffweb.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.bffweb.dto.CuentaWebResponse;
import com.duoc.bffweb.service.CuentaWebService;

@RestController
@RequestMapping("/api/web/cuentas")
public class WebCuentaController {

    private final CuentaWebService cuentaWebService;

    public WebCuentaController(CuentaWebService cuentaWebService) {
        this.cuentaWebService = cuentaWebService;
    }

    @GetMapping
    public List<CuentaWebResponse> obtenerCuentas() {
        return cuentaWebService.obtenerCuentas();
    }

    @GetMapping("/{id}")
    public CuentaWebResponse obtenerCuenta(
            @PathVariable Integer id) {

        return cuentaWebService.obtenerCuenta(id);
    }
}