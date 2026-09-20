package com.duoc.bffatm.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import com.duoc.bffatm.dto.CuentaBackendResponse;
import com.duoc.bffatm.dto.RetiroAtmRequest;
import com.duoc.bffatm.dto.RetiroAtmResponse;
import com.duoc.bffatm.dto.RetiroBackendResponse;
import com.duoc.bffatm.dto.SaldoAtmResponse;

@Service
public class CuentaAtmService {

        private final RestClient restClient;
        private final CircuitBreaker circuitBreaker;

        public CuentaAtmService(
                        @Value("${bankxyz.backend.url}") String backendUrl,
                        CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

                this.restClient = RestClient.builder()
                                .baseUrl(backendUrl)
                                .build();

                this.circuitBreaker = circuitBreakerFactory.create("circuitBreaker");
        }

        public SaldoAtmResponse obtenerSaldo(Integer id) {

                return circuitBreaker.run(
                                () -> {

                                        CuentaBackendResponse cuenta = restClient.get()
                                                        .uri("/api/cuentas/{id}", id)
                                                        .retrieve()
                                                        .body(CuentaBackendResponse.class);

                                        return new SaldoAtmResponse(
                                                        cuenta.cuentaId(),
                                                        cuenta.saldoFinal(),
                                                        cuenta.estado());
                                },
                                throwable -> obtenerSaldoFallback(id, throwable));
        }

        public SaldoAtmResponse obtenerSaldoFallback(
                        Integer id,
                        Throwable e) {

                System.out.println("CIRCUIT BREAKER ACTIVADO: " + e.getMessage());

                return new SaldoAtmResponse(
                                id,
                                BigDecimal.ZERO,
                                "TEMPORALMENTE_NO_DISPONIBLE");
        }

        public RetiroAtmResponse retirar(
                        Integer id,
                        RetiroAtmRequest request) {

                return circuitBreaker.run(
                                () -> {

                                        RetiroBackendResponse respuesta = restClient.post()
                                                        .uri("/api/cuentas/{id}/retiro", id)
                                                        .body(request)
                                                        .retrieve()
                                                        .body(RetiroBackendResponse.class);

                                        return new RetiroAtmResponse(
                                                        respuesta.cuentaId(),
                                                        respuesta.montoRetirado(),
                                                        respuesta.saldoDisponible(),
                                                        respuesta.mensaje());
                                },

                                throwable -> retirarFallback(id, request, throwable));
        }

        private RetiroAtmResponse retirarFallback(
                        Integer id,
                        RetiroAtmRequest request,
                        Throwable e) {

                System.out.println(
                                "CIRCUIT BREAKER ACTIVADO: " + e.getMessage());

                return new RetiroAtmResponse(
                                id,
                                request.monto(),
                                BigDecimal.ZERO,
                                "Servicio temporalmente no disponible");
        }
}