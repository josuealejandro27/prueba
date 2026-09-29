package com.proyecto.servicios.entity.mongo;

import com.proyecto.servicios.model.catalogo.ProductoDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Documento de MongoDB que actúa exclusivamente como caché
 * del catálogo de productos obtenido del servicio externo.
 * La base de datos relacional (PostgreSQL) no se modifica con esta información.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "catalogo_productos_cache")
public class CatalogoProductosCache {

    /** Llave fija: el sistema solo almacena un snapshot del catálogo. */
    public static final String LLAVE_UNICA = "CATALOGO_PRODUCTOS";

    @Id
    private String id;

    @Indexed
    @Field("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Field("estatus_origen")
    private String estatusOrigen;

    @Field("mensaje_origen")
    private String mensajeOrigen;

    @Field("productos")
    private List<ProductoDTO> productos;
}
