package com.duoc.bffmobile.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.duoc.bffmobile.dto.CuentaBackendResponse;
import com.duoc.bffmobile.dto.CuentaMobileResponse;

@Service
public class CuentaMobileService {

    private final RestClient restClient;

    public CuentaMobileService(
            @Value("${bankxyz.backend.url}") String backendUrl) {

        this.restClient = RestClient.builder()
                .baseUrl(backendUrl)
                .build();
    }

    public List<CuentaMobileResponse> obtenerCuentas() {

        CuentaBackendResponse[] cuentas = restClient.get()
                .uri("/api/cuentas")
                .retrieve()
                .body(CuentaBackendResponse[].class);

        if (cuentas == null) {
            return List.of();
        }

        return Arrays.stream(cuentas)
                .map(this::convertirAMobile)
                .toList();
    }

    public CuentaMobileResponse obtenerCuenta(Integer id) {

        CuentaBackendResponse cuenta = restClient.get()
                .uri("/api/cuentas/{id}", id)
                .retrieve()
                .body(CuentaBackendResponse.class);

        return convertirAMobile(cuenta);
    }

    private CuentaMobileResponse convertirAMobile(
            CuentaBackendResponse cuenta) {

        return new CuentaMobileResponse(
                cuenta.cuentaId(),
                cuenta.nombre(),
                cuenta.tipo(),
                cuenta.saldoFinal(),
                cuenta.estado()
        );
    }
}