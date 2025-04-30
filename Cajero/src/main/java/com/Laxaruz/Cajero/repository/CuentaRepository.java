package com.Laxaruz.Cajero.repository;

import com.Laxaruz.Cajero.Entity.Cliente;
import com.Laxaruz.Cajero.Entity.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface CuentaRepository extends JpaRepository<Cuenta,Long> {

    Optional<Cuenta> findByNumero(String numero);
    List<Cuenta> findByCliente(Cliente cliente);
    Cuenta save(Cuenta cuenta);
}
