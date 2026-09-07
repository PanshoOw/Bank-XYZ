package com.duoc.bffmobile.dto;

import java.math.BigDecimal;

public record CuentaMobileResponse(
        Integer cuentaId,
        String nombre,
        String tipoCuenta,
        BigDecimal saldoActual,
        String estado
) {
}