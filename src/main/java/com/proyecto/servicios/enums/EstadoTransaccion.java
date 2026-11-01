package com.proyecto.servicios.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados del ciclo de vida de una transacción financiera con máquina de estados explícita.
 */
public enum EstadoTransaccion {
    /** Transacción registrada, validaciones sintácticas aprobadas. */
    INICIADA,

    /** Saldo reservado mediante bloqueo pesimista en base de datos; petición en curso hacia el proveedor. */
    EN_PROCESO,

    /** El proveedor confirmó la operación exitosamente (código 0 o 01); débito consolidado. */
    EXITOSA,

    /** Transacción rechazada por validación o respuesta denegada del proveedor; saldo restituido. */
    RECHAZADA,

    /** Timeout de comunicación con el proveedor; transacción en custodia para conciliación. */
    TIMEOUT,

    /** Transacción reversada o cancelada por recuperación/conciliación, fondos devueltos a la cuenta bancaria. */
    REVERTIDA;

    /**
     * Indica si el estado es terminal y no admite nuevas liquidaciones financieras.
     */
    public boolean esTerminal() {
        return this == EXITOSA || this == RECHAZADA || this == REVERTIDA;
    }

    /**
     * Verifica si la transición hacia el estado {@code destino} es válida dentro de la máquina de estados.
     */
    public boolean puedeTransicionarA(EstadoTransaccion destino) {
        if (destino == null || this == destino) {
            return false;
        }
        return switch (this) {
            case INICIADA -> EnumSet.of(EN_PROCESO, RECHAZADA, REVERTIDA).contains(destino);
            case EN_PROCESO -> EnumSet.of(EXITOSA, RECHAZADA, TIMEOUT, REVERTIDA).contains(destino);
            case TIMEOUT -> EnumSet.of(EXITOSA, REVERTIDA).contains(destino);
            case EXITOSA, RECHAZADA, REVERTIDA -> false;
        };
    }

    public Set<EstadoTransaccion> transicionesPermitidas() {
        return switch (this) {
            case INICIADA -> EnumSet.of(EN_PROCESO, RECHAZADA, REVERTIDA);
            case EN_PROCESO -> EnumSet.of(EXITOSA, RECHAZADA, TIMEOUT, REVERTIDA);
            case TIMEOUT -> EnumSet.of(EXITOSA, REVERTIDA);
            case EXITOSA, RECHAZADA, REVERTIDA -> EnumSet.noneOf(EstadoTransaccion.class);
        };
    }
}


