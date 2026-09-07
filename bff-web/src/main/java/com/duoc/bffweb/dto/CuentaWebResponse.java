package com.duoc.bffweb.dto;

import java.math.BigDecimal;

public record CuentaWebResponse(
        Integer cuentaId,
        String nombreCliente,
        Integer edadCliente,
        String tipoCuenta,
        BigDecimal saldoInicial,
        BigDecimal tasaInteres,
        BigDecimal interesGenerado,
        BigDecimal saldoActual,
        String estado
) {
}