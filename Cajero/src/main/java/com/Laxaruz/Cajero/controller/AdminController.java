package com.Laxaruz.Cajero.controller;

import com.Laxaruz.Cajero.Entity.Cliente;
import com.Laxaruz.Cajero.Entity.Cuenta;
import com.Laxaruz.Cajero.Entity.TipoCuenta;
import com.Laxaruz.Cajero.services.ClienteService;
import com.Laxaruz.Cajero.services.CuentaService;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin") // "https" = protocolo de transferencia de hipertexto seguro
public class AdminController {
    private final ClienteService clienteService;
    private final CuentaService cuentaService;

    //Creación de rutas, lo que va despues de /admin
    @GetMapping
    public String adminHome() {

        return "admin/index"; // Nombre de la vista
    }

    @GetMapping("/crear-cliente")
    public String mostrarFomularioCliente(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "admin/crear-cliente";
    }
    @PostMapping("/crear-cliente")
    public String crearCliente(@ModelAttribute Cliente cliente) {
        clienteService.crearCliente(cliente);
        return "redirect:/admin"; // Redirige a la página de inicio del admin
    }
    @GetMapping("/crear-cuenta")
    public String mostrarFormularioCuenta(Model model) {
        model.addAttribute("cuenta", new Cuenta());
        return "admin/crear-cuenta";
    }
    @PostMapping("/crear-cuenta")
    public String crearCuenta(@RequestParam String identificacion,
                              @RequestParam String numero,
                              @RequestParam TipoCuenta tipo,
                              @RequestParam double saldo) {
        Cliente cliente = clienteService.buscarPorIdentificacion(identificacion).orElseThrow();
        cuentaService.crearCuenta(cliente,numero,tipo,saldo);

        return "redirect:/admin"; // Redirige a la página de inicio del admin
    }
    @GetMapping("/desbloquear")
    public String mostrarDesbloqueo() {
        return "admin/desbloquear";
    }
    @PostMapping("/desbloquear")
    public String desbloquearCuenta(@RequestParam String identificacion,
                                    @RequestParam String nuevoPin){
        clienteService.desbloquearCliente(identificacion,nuevoPin);
        return "redirect:/admin";
    }
}
