package com.duoc.bffatm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.duoc.bffatm.dto.CuentaBackendResponse;
import com.duoc.bffatm.dto.RetiroAtmRequest;
import com.duoc.bffatm.dto.RetiroAtmResponse;
import com.duoc.bffatm.dto.RetiroBackendResponse;
import com.duoc.bffatm.dto.SaldoAtmResponse;

@Service
public class CuentaAtmService {

        private final RestClient restClient;

        public CuentaAtmService(
                @Value("${bankxyz.backend.url}") String backendUrl) {

                this.restClient = RestClient.builder()
                        .baseUrl(backendUrl)
                        .build();
        }

        public SaldoAtmResponse obtenerSaldo(Integer id) {

                CuentaBackendResponse cuenta = restClient.get()
                        .uri("/api/cuentas/{id}", id)
                        .retrieve()
                        .body(CuentaBackendResponse.class);

                return new SaldoAtmResponse(
                        cuenta.cuentaId(),
                        cuenta.saldoFinal(),
                        cuenta.estado()
                );
        }

        public RetiroAtmResponse retirar(
                        Integer id,
                        RetiroAtmRequest request) {

                try {

                        RetiroBackendResponse respuesta = restClient.post()
                                .uri("/api/cuentas/{id}/retiro", id)
                                .body(request)
                                .retrieve()
                                .body(RetiroBackendResponse.class);

                        return new RetiroAtmResponse(
                                respuesta.cuentaId(),
                                respuesta.montoRetirado(),
                                respuesta.saldoDisponible(),
                                respuesta.mensaje()
                        );

                } catch (RestClientResponseException ex) {

                        throw new ResponseStatusException(
                                ex.getStatusCode(),
                                "No fue posible realizar el retiro"
                        );
                }
        }
}