package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.Usuario;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByClienteId(Long clienteId);

    List<Usuario> findByCorreoContainingIgnoreCase(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByClienteId(Long clienteId);
}
