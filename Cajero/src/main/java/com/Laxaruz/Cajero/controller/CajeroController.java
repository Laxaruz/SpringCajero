package com.Laxaruz.Cajero.controller;
import com.Laxaruz.Cajero.Entity.Cliente;
import com.Laxaruz.Cajero.Entity.Cuenta;
import com.Laxaruz.Cajero.dto.TransferenciaForm;
import com.Laxaruz.Cajero.repository.CuentaRepository;
import com.Laxaruz.Cajero.services.ClienteService;
import com.Laxaruz.Cajero.services.CuentaService;
import com.Laxaruz.Cajero.services.MovimientoService;
import com.Laxaruz.Cajero.services.RetiroService;
import com.Laxaruz.Cajero.util.AuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cajero")
public class CajeroController {
    private static final Logger logger = LoggerFactory.getLogger(CajeroController.class);
    
    private final ClienteService clienteService;
    private final CuentaService cuentaService;
    private final MovimientoService movimientoService;
    private final RetiroService retiroService;
    private final CuentaRepository cuentaRepository;

    @GetMapping("/login")
    public String loginForm() {
        return "cajero/login";
    }    @PostMapping("/login")
    public String login(@RequestParam String numeroCuenta, @RequestParam String pin,
                        HttpSession session, Model model, HttpServletRequest request) {
        
        AuditLogger.PerformanceTimer timer = new AuditLogger.PerformanceTimer("LOGIN_ATTEMPT");
        String clientIp = getClientIpAddress(request);
        
        logger.info("Intento de login para cuenta: {} desde IP: {}", numeroCuenta, clientIp);
        
        try {
            var cuenta = cuentaService.buscarPorNumero(numeroCuenta);
            if (cuenta.isEmpty()) {
                AuditLogger.logLoginAttempt(numeroCuenta, clientIp, false, "CUENTA_NO_EXISTE");
                model.addAttribute("error", "La cuenta no existe");
                return "cajero/login";
            }

            Cliente cliente = cuenta.get().getCliente();
            
            if (cliente.isBloqueado()) {
                AuditLogger.logLoginAttempt(numeroCuenta, clientIp, false, "CUENTA_BLOQUEADA");
                model.addAttribute("error", "La cuenta está bloqueada");
                return "cajero/login";
            }
            
            if (!cliente.getPin().equals(pin)) {
                clienteService.incrementarIntento(cliente);
                logger.warn("PIN incorrecto para cuenta: {}. Intentos: {}", numeroCuenta, cliente.getIntentos());
                
                if (cliente.getIntentos() >= 3) {
                    clienteService.bloquearCliente(cliente);
                    AuditLogger.logAccountBlock(numeroCuenta, "EXCESO_INTENTOS_FALLIDOS", "SISTEMA_AUTOMATICO");
                    AuditLogger.logLoginAttempt(numeroCuenta, clientIp, false, "CUENTA_BLOQUEADA_POR_INTENTOS");
                    model.addAttribute("error", "La cuenta está bloqueada por intentos fallidos");
                } else {
                    AuditLogger.logLoginAttempt(numeroCuenta, clientIp, false, "PIN_INCORRECTO");
                    model.addAttribute("error", "Pin Incorrecto");
                }
                return "cajero/login";
            }
            
            // Login exitoso
            clienteService.reiniciarIntentos(cliente);
            session.setAttribute("cliente", cliente);
            
            AuditLogger.logLoginAttempt(numeroCuenta, clientIp, true, "LOGIN_EXITOSO");
            logger.info("Login exitoso para cuenta: {} desde IP: {}", numeroCuenta, clientIp);
            
            timer.finish("Login exitoso");
            return "redirect:/cajero/menu";
            
        } catch (Exception e) {
            logger.error("Error durante el login para cuenta: {}", numeroCuenta, e);
            AuditLogger.logCriticalError("LOGIN_ERROR", "Error durante proceso de login", 
                "CajeroController.login", e);
            model.addAttribute("error", "Error interno del sistema. Intenta de nuevo.");
            timer.finish("Error en login");
            return "cajero/login";
        }
    }
    
    /**
     * Obtener la dirección IP real del cliente
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    @GetMapping("/menu")
    public String menu(HttpSession session, Model model) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        if (cliente == null) {
            return "redirect:/cajero/login";
        }
        model.addAttribute("cliente", cliente);
        model.addAttribute("cuentas", cuentaService.buscarPorCliente(cliente));
        return "cajero/menu";
    }
    @GetMapping("/consultas")
    public String consultas(Model model, HttpSession session) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        if (cliente == null) {
            return "redirect:/cajero/login";
        }
        model.addAttribute("cuentas", cuentaService.buscarPorCliente(cliente));
        return "cajero/consultas";
    }
    //Ruta dinámica reciben parametros
    @GetMapping("/movimientos/{numero}")
    public String movimientos(@PathVariable String numero, Model model, HttpSession session) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        if (cliente == null) return "redirect:/cajero/login";

        try {
            var movimientos = movimientoService.buscarPorCuenta(numero);
            model.addAttribute("movimientos", movimientos);
            model.addAttribute("numeroCuenta", numero);
            return "cajero/movimientos";
        } catch (Exception e) {
            model.addAttribute("error", "No fue posible obtener los movimientos: " + e.getMessage());
            return "cajero/consultas";
        }
    }
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/cajero/login";
    }
    @GetMapping("/retiro")
    public String mostrarFormularioRetiro(Model model, HttpSession session) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        if (cliente == null) {
            return "redirect:/cajero/login";
        }
        model.addAttribute("cuentas", cuentaService.buscarPorCliente(cliente));
        return "cajero/retiro";
    }
    @PostMapping("/retiro")
    public String realizarRetiro(@RequestParam String identificacion, @RequestParam double monto,
                                 @RequestParam String numeroCuenta, RedirectAttributes redirectAttributes) {
        try {
            String resultado = retiroService.realizarRetiro(identificacion, numeroCuenta, monto);
            redirectAttributes.addFlashAttribute("mensaje", "Retiro exitoso");
            return resultado;
        }catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cajero/retiro";
        }
    }
    @GetMapping("/consignar")
    public String mostrarFormularioConsignacion(Model model, HttpSession session) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        if (cliente == null) {
            return "redirect:/cajero/login";
        }        model.addAttribute("cuentas", cuentaService.buscarPorCliente(cliente));
        return "cajero/consignar";
    }
    
    @PostMapping("/consignar")
    public String consignar(@RequestParam String numeroCuenta,
                            @RequestParam double monto,
                            Model model, HttpSession session) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        if (cliente == null) {
            return "redirect:/cajero/login";
        }
        
        try {
            Cuenta cuenta = cuentaRepository.findByNumero(numeroCuenta)
                    .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

            movimientoService.realizarConsignacion(cuenta, monto);
            model.addAttribute("mensaje", "Consignación exitosa. Nuevo saldo: " + cuenta.getSaldo());
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        // Volver a pasar las cuentas para que el template funcione correctamente
        model.addAttribute("cuentas", cuentaService.buscarPorCliente(cliente));
        return "cajero/consignar";
    }
    @GetMapping("/transferir")
    public String mostarFormularioTranferencia(Model model) {
        model.addAttribute("transferenciaForm",new TransferenciaForm());
        return "cajero/transferir";
    }
    @PostMapping("/transferir")
    public String transferir(@RequestParam String numeroCuentaDestino,
                             @RequestParam double monto,
                             HttpSession session,
                             Model model) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        if (cliente == null) return "redirect:/cajero/login";

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
    @GetMapping("/titular")
    @ResponseBody
    public Map<String, String> obtenerTitular(@RequestParam String numero){
        return cuentaService.buscarPorNumero(numero)
                .map(cuenta -> Map.of("nombre", cuenta.getCliente().getNombre()))
                .orElse(Map.of());
    }
    @GetMapping("/cambiar-clave")
    public String mostrarFormularioCambioClave() {
        return "cajero/cambiar-clave";
    }    @PostMapping("/cambiar-clave")
    public String cambiarClave(@RequestParam String claveActual,
                               @RequestParam String nuevaClave, @RequestParam String confirmarClave,
                               HttpSession session, Model model, HttpServletRequest request) {
        
        AuditLogger.PerformanceTimer timer = new AuditLogger.PerformanceTimer("CHANGE_PIN");
        String clientIp = getClientIpAddress(request);
        
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        if (cliente == null) {
            logger.warn("Intento de cambio de PIN sin sesión activa desde IP: {}", clientIp);
            return "redirect:/cajero/login";
        }
        
        String numeroCuenta = cuentaService.buscarPorCliente(cliente)
            .stream().findFirst().map(Cuenta::getNumero).orElse("UNKNOWN");
        
        logger.info("Intento de cambio de PIN para cliente ID: {} desde IP: {}", cliente.getId(), clientIp);
        
        try {
            if (!cliente.getPin().equals(claveActual)) {
                logger.warn("PIN actual incorrecto en cambio de clave para cliente: {}", cliente.getId());
                AuditLogger.logPinChange(numeroCuenta, cliente.getId().toString(), clientIp, false);
                model.addAttribute("error", "Clave actual incorrecta.");
                return "cajero/cambiar-clave";
            }
            
            if (!nuevaClave.equals(confirmarClave)) {
                logger.warn("Confirmación de PIN no coincide para cliente: {}", cliente.getId());
                model.addAttribute("error", "Las nuevas claves no coinciden.");
                return "cajero/cambiar-clave";
            }
            
            // Validaciones adicionales para el nuevo PIN
            if (nuevaClave.length() < 4) {
                model.addAttribute("error", "El PIN debe tener al menos 4 dígitos.");
                return "cajero/cambiar-clave";
            }
            
            if (!nuevaClave.matches("\\d+")) {
                model.addAttribute("error", "El PIN debe contener solo números.");
                return "cajero/cambiar-clave";
            }
            
            clienteService.cambiarPin(cliente, nuevaClave);
            session.setAttribute("cliente", cliente);
            
            AuditLogger.logPinChange(numeroCuenta, cliente.getId().toString(), clientIp, true);
            logger.info("PIN cambiado exitosamente para cliente: {} desde IP: {}", cliente.getId(), clientIp);
            
            model.addAttribute("mensaje", "Clave cambiada exitosamente.");
            timer.finish("Cambio de PIN exitoso");
            
        } catch (Exception e) {
            logger.error("Error durante cambio de PIN para cliente: {}", cliente.getId(), e);
            AuditLogger.logCriticalError("PIN_CHANGE_ERROR", "Error durante cambio de PIN", 
                "CajeroController.cambiarClave", e);
            model.addAttribute("error", "Error interno del sistema. Intenta de nuevo.");
            timer.finish("Error en cambio de PIN");
        }

        model.addAttribute("mensaje", "Clave cambiada exitosamente.");
        return "cajero/cambiar-clave";
    }
}