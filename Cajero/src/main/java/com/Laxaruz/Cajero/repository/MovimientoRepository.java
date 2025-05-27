package com.Laxaruz.Cajero.repository;

import com.Laxaruz.Cajero.Entity.Cuenta;
import com.Laxaruz.Cajero.Entity.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento,Long> {

    List<Movimiento> findByCuenta(Cuenta cuenta);
    List<Movimiento> findByCuentaOrderByFechaDesc(Cuenta cuenta);

    List<Movimiento> findByCuentaAndMonto(Cuenta cuenta, double monto);
}
