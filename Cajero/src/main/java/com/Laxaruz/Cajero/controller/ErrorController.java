package com.Laxaruz.Cajero.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(ErrorController.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Obtener el código de estado del error
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        
        // Obtener información adicional del error
        String errorMessage = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Exception exception = (Exception) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        
        // Log detallado del error por parte del ErrorController
        logErrorDetails(request, status, errorMessage, requestUri, exception);
        
        // Valores por defecto
        String title = "Oops! Algo salió mal";
        String message = "Hemos encontrado un problema inesperado";
        String description = "Nuestro equipo técnico ha sido notificado y está trabajando para solucionarlo.";
        String errorCode = "500";
        String iconClass = "fas fa-exclamation-triangle";
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            errorCode = String.valueOf(statusCode);
            
            switch (statusCode) {
                case 404:
                    title = "Página no encontrada";
                    message = "La página que buscas no existe";
                    description = "Es posible que la URL esté mal escrita o que la página haya sido movida o eliminada.";
                    iconClass = "fas fa-search";
                    break;
                case 403:
                    title = "Acceso denegado";
                    message = "No tienes permisos para acceder a esta página";
                    description = "Si crees que deberías tener acceso, por favor contacta al administrador del sistema.";
                    iconClass = "fas fa-lock";
                    break;
                case 400:
                    title = "Solicitud incorrecta";
                    message = "La solicitud no pudo ser procesada";
                    description = "Verifica que todos los datos estén completos y correctos.";
                    iconClass = "fas fa-exclamation-circle";
                    break;                case 405:
                    title = "Método no permitido";
                    message = "La operación solicitada no está permitida";
                    description = "El servidor no admite el método utilizado para esta solicitud.";
                    iconClass = "fas fa-ban";
                    break;
                case 503:
                    title = "Servicio no disponible";
                    message = "El servicio está temporalmente fuera de línea";
                    description = "Estamos realizando mantenimiento. Por favor, inténtalo de nuevo en unos minutos.";
                    iconClass = "fas fa-tools";
                    break;
                case 500:
                default:
                    title = "Error interno del servidor";
                    message = "Algo salió mal en nuestros servidores";
                    description = "Nuestro equipo técnico ha sido notificado automáticamente y está trabajando para resolver el problema.";
                    iconClass = "fas fa-server";
                    break;
            }
        }
        
        // Añadir información al modelo
        model.addAttribute("errorCode", errorCode);
        model.addAttribute("title", title);
        model.addAttribute("message", message);
        model.addAttribute("description", description);
        model.addAttribute("iconClass", iconClass);
        model.addAttribute("requestUri", requestUri);
          // Si es un error específico, usar template específico, sino usar el genérico
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            if (statusCode == 404) {
                return "error/404";
            } else if (statusCode == 403) {
                return "error/403";
            } else if (statusCode == 503) {
                return "error/503";
            }
        }
          return "error/error";
    }
    
    /**
     * Log detallado de errores manejados por ErrorController
     */
    private void logErrorDetails(HttpServletRequest request, Object status, String errorMessage, 
                               String requestUri, Exception exception) {
        
        logger.info("=== ERROR CAPTURADO POR ErrorController ===");
        logger.info("Timestamp: {}", LocalDateTime.now().format(formatter));
        logger.info("Status Code: {}", status);
        logger.info("Error Message: {}", errorMessage);
        logger.info("Request URI: {}", requestUri);
        logger.info("Request URL: {}", request.getRequestURL());
        logger.info("HTTP Method: {}", request.getMethod());
        logger.info("Remote Address: {}", request.getRemoteAddr());
        logger.info("User-Agent: {}", request.getHeader("User-Agent"));
        logger.info("Referer: {}", request.getHeader("Referer"));
        
        // Log parámetros de request
        logger.info("--- Request Parameters ---");
        request.getParameterMap().forEach((key, values) -> {
            if (key.toLowerCase().contains("password") || 
                key.toLowerCase().contains("pin") || 
                key.toLowerCase().contains("clave")) {
                logger.info("  {}: [HIDDEN]", key);
            } else {
                logger.info("  {}: {}", key, String.join(", ", values));
            }
        });
        
        // Log atributos de request importantes
        logger.info("--- Request Attributes ---");
        logger.info("  ERROR_STATUS_CODE: {}", request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
        logger.info("  ERROR_EXCEPTION_TYPE: {}", request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE));
        logger.info("  ERROR_MESSAGE: {}", request.getAttribute(RequestDispatcher.ERROR_MESSAGE));
        logger.info("  ERROR_REQUEST_URI: {}", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        logger.info("  ERROR_SERVLET_NAME: {}", request.getAttribute(RequestDispatcher.ERROR_SERVLET_NAME));
        
        // Log información de sesión si existe
        if (request.getSession(false) != null) {
            logger.info("--- Session Information ---");
            logger.info("  Session ID: {}", request.getSession().getId());
            logger.info("  Is New: {}", request.getSession().isNew());
            logger.info("  Creation Time: {}", new java.util.Date(request.getSession().getCreationTime()));
            logger.info("  Last Accessed: {}", new java.util.Date(request.getSession().getLastAccessedTime()));
        }
        
        // Si hay una excepción, loggear detalles
        if (exception != null) {
            logger.error("--- Exception Details ---");
            logger.error("Exception Type: {}", exception.getClass().getName());
            logger.error("Exception Message: {}", exception.getMessage());
            logger.error("Stack Trace:", exception);
        }
        
        logger.info("=== FIN ERROR ErrorController ===");
    }
}
