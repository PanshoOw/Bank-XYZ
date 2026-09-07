package com.duoc.bffweb.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.duoc.bffweb.dto.CuentaBackendResponse;
import com.duoc.bffweb.dto.CuentaWebResponse;

@Service
public class CuentaWebService {

    private final RestClient restClient;

    public CuentaWebService(
            @Value("${bankxyz.backend.url}") String backendUrl) {

        this.restClient = RestClient.builder()
                .baseUrl(backendUrl)
                .build();
    }

    public CuentaWebResponse obtenerCuenta(Integer id) {

        CuentaBackendResponse cuenta = restClient.get()
                .uri("/api/cuentas/{id}", id)
                .retrieve()
                .body(CuentaBackendResponse.class);

        return convertirAWeb(cuenta);
    }

    public List<CuentaWebResponse> obtenerCuentas() {

        CuentaBackendResponse[] cuentas = restClient.get()
                .uri("/api/cuentas")
                .retrieve()
                .body(CuentaBackendResponse[].class);

        if (cuentas == null) {
            return List.of();
        }

        return Arrays.stream(cuentas)
                .map(this::convertirAWeb)
                .toList();
    }

    private CuentaWebResponse convertirAWeb(CuentaBackendResponse cuenta) {

        return new CuentaWebResponse(
                cuenta.cuentaId(),
                cuenta.nombre(),
                cuenta.edad(),
                cuenta.tipo(),
                cuenta.saldoInicial(),
                cuenta.tasaInteres(),
                cuenta.interesCalculado(),
                cuenta.saldoFinal(),
                cuenta.estado()
        );
    }
}