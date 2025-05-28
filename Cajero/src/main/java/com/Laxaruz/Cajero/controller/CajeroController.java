package com.Laxaruz.Cajero.controller;
import com.Laxaruz.Cajero.Entity.Cliente;
import com.Laxaruz.Cajero.Entity.Cuenta;
import com.Laxaruz.Cajero.dto.TransferenciaForm;
import com.Laxaruz.Cajero.repository.ClienteRepository;
import com.Laxaruz.Cajero.repository.CuentaRepository;
import com.Laxaruz.Cajero.repository.MovimientoRepository;
import com.Laxaruz.Cajero.services.ClienteService;
import com.Laxaruz.Cajero.services.CuentaService;
import com.Laxaruz.Cajero.services.MovimientoService;
import com.Laxaruz.Cajero.services.RetiroService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cajero")
public class CajeroController {
    private final ClienteService clienteService;
    private final CuentaService cuentaService;
    private final CuentaRepository cuentaRepository;
    private final MovimientoService movimientoService;
    private final RetiroService retiroService;

    @GetMapping
    public String loginForm() {
        return "cajero/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String numeroCuenta,
                        @RequestParam String pin,
                        HttpSession session,
                        Model model) {
        var cuenta = cuentaService.buscarPorNumero(numeroCuenta);
        if (cuenta.isEmpty()) {
            model.addAttribute("error", "Cuenta no encontrada.");
            return "cajero/login";
        }

        Cliente cliente = cuenta.get().getCliente();

        if (cliente.isBloqueado()) {
            model.addAttribute("error", "Cuenta bloqueada.");
            return "cajero/login";
        }

        if (!cliente.getPin().equals(pin)) {
            clienteService.incrementarIntento(cliente);
            if (cliente.getIntentos() >= 3) {
                clienteService.bloquearCliente(cliente);
                model.addAttribute("error", "Cuenta bloqueada por intentos fallidos.");
            } else {
                model.addAttribute("error", "PIN incorrecto.");
            }
            return "cajero/login";
        }

        clienteService.reiniciarIntentos(cliente);
        session.setAttribute("cliente", cliente);
        return "redirect:/cajero/menu";
    }

    @GetMapping("/menu")
    public String menu(HttpSession session, Model model) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        if (cliente == null) return "redirect:/cajero";

        model.addAttribute("cliente", cliente);
        model.addAttribute("cuentas", cuentaService.buscarPorCliente(cliente));
        return "cajero/menu";
    }

    @GetMapping("/consultas")
    public String consultas(Model model, HttpSession session) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        model.addAttribute("cuentas", cuentaService.buscarPorCliente(cliente));
        return "cajero/consultas";
    }

    @GetMapping("/movimientos/{numero}")
    public String movimientos(@PathVariable String numero, Model model, HttpSession session) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        if (cliente == null) return "redirect:/cajero";

        try {
            var movimientos = movimientoService.buscarPorCuenta(numero);
            model.addAttribute("movimientos", movimientos);
            return "cajero/movimientos";
        } catch (Exception e) {
            model.addAttribute("error", "No fue posible obtener los movimientos: " + e.getMessage());
            return "cajero/consultas";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/cajero";
    }

    @GetMapping("/retiro")
    public String mostrarFormularioRetiro(Model model, HttpSession session) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        model.addAttribute("cuentas", cuentaService.buscarPorCliente(cliente));
        return "cajero/retiro";
    }

    @PostMapping("/retiro")
    public String realizarRetiro(@RequestParam String identificacion,
                                 @RequestParam String numero,
                                 @RequestParam double monto,
                                 RedirectAttributes redirectAttributes) {
        try {
            retiroService.realizarRetiro(identificacion, numero, monto);
            redirectAttributes.addFlashAttribute("mensaje", "Retiro exitoso");
            return "redirect:/cajero/menu";
        } catch(RuntimeException e){
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cajero/retiro";
        }
    }

    @GetMapping("/transferir")
    public String mostrarFormularioTransferencia(Model model) {
        model.addAttribute("transferenciaForm", new TransferenciaForm());
        return "cajero/transferir";
    }

    @PostMapping("/transferir")
    public String transferir(@RequestParam String numeroCuentaDestino,
                             @RequestParam double monto,
                             HttpSession session,
                             Model model) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        if (cliente == null) return "redirect:/cajero";

        Cuenta origen = cuentaService.buscarPorCliente(cliente)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontró cuenta origen"));

        try {
            Cuenta destino = cuentaService.buscarPorNumero(numeroCuentaDestino)
                    .orElseThrow(() -> new RuntimeException("Cuenta destino no encontrada"));

            if (movimientoService.realizarTransferencia(origen, destino, monto)) {
                model.addAttribute("mensaje", "Transferencia realizada con éxito");
            } else {
                model.addAttribute("error", "Saldo insuficiente para realizar la transferencia");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
        }

        return "cajero/transferir";
    }
}

