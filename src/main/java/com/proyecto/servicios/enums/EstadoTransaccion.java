package com.proyecto.servicios.enums;

/**
 * Estados del ciclo de vida de una transacción financiera.
 */
public enum EstadoTransaccion {
    /** Transacción registrada, validaciones sintácticas aprobadas. */
    INICIADA,

    /** Saldo reservado mediante bloqueo pesimista en base de datos; petición enviada al proveedor. */
    EN_PROCESO,

    /** El proveedor confirmó la operación exitosamente (código 0 o 01); débito consolidado. */
    EXITOSA,

    /** Transacción rechazada por saldo insuficiente, validación o respuesta denegada del proveedor. */
    RECHAZADA,

    /** Timeout de comunicación con el proveedor; transacción en custodia para conciliación. */
    TIMEOUT,

    /** Transacción reversada o cancelada, fondos devueltos a la cuenta bancaria. */
    REVERTIDA
}

