package com.Laxaruz.Cajero.services;

import com.Laxaruz.Cajero.Entity.Cliente;
import com.Laxaruz.Cajero.Entity.Cuenta;
import com.Laxaruz.Cajero.repository.ClienteRepository;
import com.Laxaruz.Cajero.repository.CuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RetiroService {
    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final MovimientoService movimientoService;

    public String realizarRetiro(String identificacion, String numeroCuenta,double monto){
        Cliente cliente = clienteRepository.findByIdentificacion(identificacion)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        Cuenta cuenta = cuentaRepository.findByNumero(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

        if(!cuenta.getCliente().equals(cliente)){
            return "La cuenta no pertenece al cliente";
        }
        if(cliente.isBloqueado()){
            return "El cliente o su cuenta está bloqueado";
        }
        boolean exito = movimientoService.realizarRetiro(cuenta,monto);
        if(!exito){
            throw new RuntimeException("No se puede realizar retiro");
        }
        return "redirect:/cajero/menu?mensaje=Retiro realizado con éxito";
    }

}
