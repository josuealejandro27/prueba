package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;

import java.util.Optional;

public interface GestoPagoTokenService {

    void renovarToken();

    Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo);

    /**
     * Devuelve el token almacenado en PostgreSQL listo para usarlo como cabecera
     * Bearer. Si el token no existe o ya expiró según su {@code expires_in},
     * lo renueva automáticamente antes de devolverlo.
     *
     * @return el token crudo, o vacío si no hay token disponible.
     */
    Optional<String> obtenerTokenVigente();
}
