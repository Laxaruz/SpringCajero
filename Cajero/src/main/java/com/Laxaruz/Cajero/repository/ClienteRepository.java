package com.Laxaruz.Cajero.repository;

import com.Laxaruz.Cajero.Entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> fiindByIdentification(String identification);
}
