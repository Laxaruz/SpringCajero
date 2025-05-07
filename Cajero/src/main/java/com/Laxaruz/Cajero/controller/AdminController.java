package com.Laxaruz.Cajero.controller;

import com.Laxaruz.Cajero.services.ClienteService;
import com.Laxaruz.Cajero.services.CuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin") // "https" = protocolo de transferencia de hipertexto seguro
public class AdminController {
    private final ClienteService clienteService;
    private final CuentaService cuentaService;

    //Creación de rutas, lo que va despues de /admin
    @GetMapping
    public String adminHome(){
        return "admin/index"; // Nombre de la vista
    }
}
