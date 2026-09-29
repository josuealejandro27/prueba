package com.proyecto.servicios.repositorys.mongo;

import com.proyecto.servicios.entity.mongo.CatalogoProductosCache;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de la caché de MongoDB del catálogo de productos.
 */
@Repository
public interface CatalogoProductosCacheRepository extends MongoRepository<CatalogoProductosCache, String> {
}
