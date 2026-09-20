package com.duoc.bffmobile.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import com.duoc.bffmobile.dto.CuentaBackendResponse;
import com.duoc.bffmobile.dto.CuentaMobileResponse;

@Service
public class CuentaMobileService {

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public CuentaMobileService(
            @Value("${bankxyz.backend.url}") String backendUrl,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this.restClient = RestClient.builder()
                .baseUrl(backendUrl)
                .build();

        this.circuitBreaker = circuitBreakerFactory.create("circuitBreaker");
    }

    public List<CuentaMobileResponse> obtenerCuentas() {

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
                            .map(this::convertirAMobile)
                            .toList();
                },

                throwable -> obtenerCuentasFallback(throwable));
    }

    private List<CuentaMobileResponse> obtenerCuentasFallback(Throwable e) {

        System.out.println(
                "CIRCUIT BREAKER ACTIVADO: " + e.getMessage());

        return List.of();
    }

    public CuentaMobileResponse obtenerCuenta(Integer id) {

        return circuitBreaker.run(
                () -> {
                    CuentaBackendResponse cuenta = restClient.get()
                            .uri("/api/cuentas/{id}", id)
                            .retrieve()
                            .body(CuentaBackendResponse.class);

                    return convertirAMobile(cuenta);
                },
                throwable -> obtenerCuentaFallback(id, throwable));
    }

    private CuentaMobileResponse obtenerCuentaFallback(
            Integer id,
            Throwable e) {

        System.out.println("CIRCUIT BREAKER ACTIVADO: " + e.getMessage());

        return new CuentaMobileResponse(
                id,
                "Servicio no disponible",
                "N/A",
                BigDecimal.ZERO,
                "TEMPORALMENTE_NO_DISPONIBLE");
    }

    private CuentaMobileResponse convertirAMobile(
            CuentaBackendResponse cuenta) {

        return new CuentaMobileResponse(
                cuenta.cuentaId(),
                cuenta.nombre(),
                cuenta.tipo(),
                cuenta.saldoFinal(),
                cuenta.estado());
    }
}