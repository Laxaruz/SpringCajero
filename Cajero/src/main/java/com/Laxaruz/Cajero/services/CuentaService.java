package com.Laxaruz.Cajero.services;

import com.Laxaruz.Cajero.Entity.Cliente;
import com.Laxaruz.Cajero.Entity.Cuenta;
import com.Laxaruz.Cajero.Entity.TipoCuenta;
import com.Laxaruz.Cajero.repository.CuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CuentaService {
    private final CuentaRepository cuentaRepository;

    public void crearCuenta(Cliente cliente, String numeroCuenta, TipoCuenta tipo, Double saldoInicial){
        Cuenta cuenta = Cuenta.builder()
                .cliente(cliente)
                .numero(numeroCuenta)
                .tipo(tipo)
                .saldo(saldoInicial)
                .build();
        cuentaRepository.save(cuenta);
    }
    public Optional<Cuenta> buscarPorNumero(String numero){
        return cuentaRepository.findByNumero(numero);
    }
    public double consultarSaldo(Cuenta cuenta){
        return cuenta.getSaldo();
    }
    public List<Cuenta> obtenerCuentasCliente(Cliente cliente){
        return cliente.getCuentas();
    }
    public void actualizarSaldo(Cuenta cuenta, double nuevoSaldo){
        cuenta.setSaldo(nuevoSaldo);
        cuentaRepository.save(cuenta);
    }
    public List<Cuenta> buscarPorCliente(Cliente cliente) {
        return cuentaRepository.findByCliente(cliente);}

    public Cuenta obtenerCuentaPorClienteActual (String username){
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void consignar(Cliente cliente, double monto) {
        List<Cuenta> cuentas = buscarPorCliente(cliente);
        if (cuentas.isEmpty()) {
            throw new RuntimeException("El cliente no tiene cuentas para consignar");
        }
        Cuenta cuenta = cuentas.get(0); // Puedes ajustar si quieres otra lógica
        double nuevoSaldo = cuenta.getSaldo() + monto;
        cuenta.setSaldo(nuevoSaldo);
        cuentaRepository.save(cuenta);
    }
}
