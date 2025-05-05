package com.Laxaruz.Cajero.repository;

import com.Laxaruz.Cajero.Entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository //Después de Jpa se ponen los datos de la entidad que se trabaja y el tipo de dato del Id
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByIdentificacion(String identificacion);
}
