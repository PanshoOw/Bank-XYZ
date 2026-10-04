package com.duoc.bffweb.service;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.duoc.bffweb.dto.CuentaBackendResponse;
import com.duoc.bffweb.dto.CuentaWebResponse;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

@Service
public class CuentaWebService {

    private static final Logger log =
            LoggerFactory.getLogger(CuentaWebService.class);

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Bulkhead bulkhead;

    public CuentaWebService(
            @Value("${bankxyz.backend.url}") String backendUrl,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory,
            RetryRegistry retryRegistry,
            BulkheadRegistry bulkheadRegistry) {

        this.restClient = RestClient.builder()
                .baseUrl(backendUrl)
                .build();

        this.circuitBreaker =
                circuitBreakerFactory.create("circuitBreaker");

        this.retry =
                retryRegistry.retry("backendRead");

        this.bulkhead =
                bulkheadRegistry.bulkhead("backendRead");
    }

    public CuentaWebResponse obtenerCuenta(Integer id) {

        Supplier<CuentaWebResponse> consulta = () -> {

            CuentaBackendResponse cuenta = restClient.get()
                    .uri("/api/cuentas/{id}", id)
                    .retrieve()
                    .body(CuentaBackendResponse.class);

            return convertirAWeb(cuenta);
        };

        Supplier<CuentaWebResponse> conBulkhead =
                Bulkhead.decorateSupplier(bulkhead, consulta);

        Supplier<CuentaWebResponse> conRetry =
                Retry.decorateSupplier(retry, conBulkhead);

        return circuitBreaker.run(
                conRetry,
                throwable -> obtenerCuentaFallback(id, throwable));
    }

    public CuentaWebResponse obtenerCuentaFallback(
            Integer id,
            Throwable e) {

        log.warn(
                "Fallback Web al consultar la cuenta {}: {}",
                id,
                e.getMessage());

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

        Supplier<List<CuentaWebResponse>> consulta = () -> {

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
        };

        Supplier<List<CuentaWebResponse>> conBulkhead =
                Bulkhead.decorateSupplier(bulkhead, consulta);

        Supplier<List<CuentaWebResponse>> conRetry =
                Retry.decorateSupplier(retry, conBulkhead);

        return circuitBreaker.run(
                conRetry,
                this::obtenerCuentasFallback);
    }

    public List<CuentaWebResponse> obtenerCuentasFallback(
            Throwable e) {

        log.warn(
                "Fallback Web al consultar cuentas: {}",
                e.getMessage());

        return List.of();
    }

    private CuentaWebResponse convertirAWeb(
            CuentaBackendResponse cuenta) {

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