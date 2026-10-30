package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByCorreo(String correo);

    @Query("SELECT u FROM Usuario u JOIN FETCH u.cliente WHERE LOWER(u.correo) = LOWER(:correo)")
    Optional<Usuario> findByCorreoWithCliente(@Param("correo") String correo);

    Optional<Usuario> findByClienteId(Long clienteId);

    boolean existsByCorreo(String correo);

    boolean existsByClienteId(Long clienteId);
}
