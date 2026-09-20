package com.duoc.bffweb.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import com.duoc.bffweb.dto.CuentaBackendResponse;
import com.duoc.bffweb.dto.CuentaWebResponse;

@Service
public class CuentaWebService {

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public CuentaWebService(
            @Value("${bankxyz.backend.url}") String backendUrl,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this.restClient = RestClient.builder()
                .baseUrl(backendUrl)
                .build();

        this.circuitBreaker = circuitBreakerFactory.create("circuitBreaker");
    }

    public CuentaWebResponse obtenerCuenta(Integer id) {

        return circuitBreaker.run(
                () -> {

                    CuentaBackendResponse cuenta = restClient.get()
                            .uri("/api/cuentas/{id}", id)
                            .retrieve()
                            .body(CuentaBackendResponse.class);

                    return convertirAWeb(cuenta);
                },

                throwable -> obtenerCuentaFallback(id, throwable));
    }

    public CuentaWebResponse obtenerCuentaFallback(
            Integer id,
            Throwable e) {

        System.out.println("CIRCUIT BREAKER ACTIVADO: " + e.getMessage());

        return new CuentaWebResponse(
                id,
                "Servicio no disponible",
                0,
                "N/A",
                null,
                null,
                null,
                null,
                "TEMPORALMENTE_NO_DISPONIBLE");
    }

    public List<CuentaWebResponse> obtenerCuentas() {

        return circuitBreaker.run(
                () -> {

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
                },

                throwable -> obtenerCuentasFallback(throwable));
    }

    public List<CuentaWebResponse> obtenerCuentasFallback(Throwable e) {

        System.out.println(
                "CIRCUIT BREAKER ACTIVADO: " + e.getMessage());

        return List.of();
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
                cuenta.estado());
    }
}