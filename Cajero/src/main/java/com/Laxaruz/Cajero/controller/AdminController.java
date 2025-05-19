package com.Laxaruz.Cajero.controller;

import com.Laxaruz.Cajero.Entity.Cliente;
import com.Laxaruz.Cajero.services.ClienteService;
import com.Laxaruz.Cajero.services.CuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
    public String mostrarFomularioCliente(Model model1) {
        model1.addAttribute("cliente", new Cliente());
        return "admin/crear-cliente";
    }
    @PostMapping("/crear-cliente")
    public String crearCliente(@ModelAttribute Cliente cliente) {
        clienteService.crearCliente(cliente);
        return "redirect:/admin"; // Redirige a la página de inicio del admin
    }
}
