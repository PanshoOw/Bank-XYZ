package com.duoc.demo.Processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.duoc.demo.Legacy.LegacyCuentas;
import com.duoc.demo.Modern.ModernCuentas;

public class LegacyCuentasProcessor
        implements ItemProcessor<LegacyCuentas, ModernCuentas> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String DEPOSITO = "deposito";
    private static final String RETIRO = "retiro";
    private static final String COMPRA = "compra";

    private static final String ESTADO_VALIDA = "VALIDA";
    private static final String ESTADO_ANOMALIA = "ANOMALIA";

    @Override
    public ModernCuentas process(LegacyCuentas item) {

        Integer cuentaId = Integer.valueOf(item.cuentaId().trim());
        LocalDate fecha = LocalDate.parse(item.fecha().trim(), FORMATTER);
        String transaccion = item.transaccion().trim().toLowerCase();
        BigDecimal monto = new BigDecimal(item.monto().trim());
        String descripcion = item.descripcion().trim();

        String estado = ESTADO_VALIDA;
        String detalleValidacion = null;

        switch (transaccion) {

            case DEPOSITO -> {
                if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                    estado = ESTADO_ANOMALIA;
                    detalleValidacion =
                            "Deposito con monto menor o igual a cero";
                }
            }

            case RETIRO -> {
                if (monto.compareTo(BigDecimal.ZERO) >= 0) {
                    estado = ESTADO_ANOMALIA;
                    detalleValidacion =
                            "Retiro con monto mayor o igual a cero";
                }
            }

            case COMPRA -> {
                if (monto.compareTo(BigDecimal.ZERO) >= 0) {
                    estado = ESTADO_ANOMALIA;
                    detalleValidacion =
                            "Compra con monto mayor o igual a cero";
                }
            }

            default -> {
                estado = ESTADO_ANOMALIA;
                detalleValidacion =
                        "Tipo de transaccion no soportado";
            }
        }

        return new ModernCuentas(
                cuentaId,
                fecha,
                transaccion,
                monto,
                descripcion,
                estado,
                detalleValidacion
        );
    }
}