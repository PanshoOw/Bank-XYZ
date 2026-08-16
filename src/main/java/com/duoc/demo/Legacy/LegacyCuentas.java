package com.duoc.demo.Legacy;

public record LegacyCuentas(
        String cuentaId,
        String fecha,
        String transaccion,
        String monto,
        String descripcion
    ) 
{
}
