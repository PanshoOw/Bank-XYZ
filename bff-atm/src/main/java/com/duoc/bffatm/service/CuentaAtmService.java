package com.duoc.bffatm.service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.duoc.bffatm.dto.CuentaBackendResponse;
import com.duoc.bffatm.dto.RetiroAtmRequest;
import com.duoc.bffatm.dto.RetiroAtmResponse;
import com.duoc.bffatm.dto.RetiroBackendResponse;
import com.duoc.bffatm.dto.SaldoAtmResponse;

@Service
public class CuentaAtmService {

        private static final Logger log =
                LoggerFactory.getLogger(CuentaAtmService.class);
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

                log.warn("Circuit Breaker activado al consultar saldo de la cuenta {}: {}",
                        id,
                        e.getMessage());

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
                                                        "CONFIRMADA",
                                                        respuesta.mensaje());
                                },

                                throwable -> retirarFallback(id, throwable));
        }

        private RetiroAtmResponse retirarFallback(
                        Integer id,
                        Throwable e) {

                log.warn("Circuit Breaker activado al retirar de la cuenta {}: {}",
                        id,
                        e.getMessage());

                return new RetiroAtmResponse(
                        id,
                        null,
                        null,
                        "NO_VERIFICADA",
                        "No fue posible confirmar el resultado del retiro. Consulte el saldo antes de reintentar.");
        }
}