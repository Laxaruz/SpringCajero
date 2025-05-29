package com.Laxaruz.Cajero.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.UUID;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Método helper para generar información detallada del error
     */
    private void logDetailedError(HttpServletRequest request, Exception ex, String errorType) {
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        
        // Configurar MDC para contexto adicional
        MDC.put("errorId", errorId);
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        try {
            // Log información básica del error
            logger.error("=== ERROR DETALLADO [{}] ===", errorType);
            logger.error("Error ID: {}", errorId);
            logger.error("Timestamp: {}", LocalDateTime.now().format(formatter));
            logger.error("URL solicitada: {}", request.getRequestURL());
            logger.error("URI: {}", request.getRequestURI());
            logger.error("Método HTTP: {}", request.getMethod());
            logger.error("Dirección IP: {}", getClientIpAddress(request));
            logger.error("User-Agent: {}", request.getHeader("User-Agent"));
            
            // Log parámetros de la solicitud
            logger.error("--- Parámetros de la solicitud ---");
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                String paramValue = request.getParameter(paramName);
                // No loggear passwords o pins por seguridad
                if (paramName.toLowerCase().contains("password") || 
                    paramName.toLowerCase().contains("pin") || 
                    paramName.toLowerCase().contains("clave")) {
                    logger.error("  {}: [HIDDEN]", paramName);
                } else {
                    logger.error("  {}: {}", paramName, paramValue);
                }
            }
            
            // Log headers importantes
            logger.error("--- Headers importantes ---");
            logger.error("  Content-Type: {}", request.getContentType());
            logger.error("  Accept: {}", request.getHeader("Accept"));
            logger.error("  Referer: {}", request.getHeader("Referer"));
            
            // Log información de sesión
            HttpSession session = request.getSession(false);
            if (session != null) {
                logger.error("--- Información de sesión ---");
                logger.error("  Session ID: {}", session.getId());
                logger.error("  Creation Time: {}", new java.util.Date(session.getCreationTime()));
                logger.error("  Last Accessed: {}", new java.util.Date(session.getLastAccessedTime()));
                
                // Log atributos de sesión (sin datos sensibles)
                Enumeration<String> sessionAttributes = session.getAttributeNames();
                while (sessionAttributes.hasMoreElements()) {
                    String attrName = sessionAttributes.nextElement();
                    Object attrValue = session.getAttribute(attrName);
                    if (attrValue != null) {
                        logger.error("  {}: {}", attrName, attrValue.getClass().getSimpleName());
                    }
                }
            } else {
                logger.error("--- No hay sesión activa ---");
            }
            
            // Log detalles de la excepción
            logger.error("--- Detalles de la excepción ---");
            logger.error("Tipo de excepción: {}", ex.getClass().getName());
            logger.error("Mensaje: {}", ex.getMessage());
            
            // Log stack trace completo
            logger.error("--- Stack Trace ---", ex);
            
            // Si hay una causa, también la loggeamos
            Throwable cause = ex.getCause();
            int causeLevel = 1;
            while (cause != null && causeLevel <= 5) { // Limitar a 5 niveles para evitar loops
                logger.error("--- Causa nivel {} ---", causeLevel);
                logger.error("Tipo: {}", cause.getClass().getName());
                logger.error("Mensaje: {}", cause.getMessage());
                cause = cause.getCause();
                causeLevel++;
            }
            
            logger.error("=== FIN ERROR DETALLADO [{}] ===", errorType);
            
        } finally {
            // Limpiar MDC
            MDC.clear();
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
      @ExceptionHandler(NoHandlerFoundException.class)
    public ModelAndView handleNotFound(HttpServletRequest request, NoHandlerFoundException ex) {
        logDetailedError(request, ex, "404_NOT_FOUND");
        
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("errorCode", "404");
        mav.addObject("title", "Página no encontrada");
        mav.addObject("message", "La página que buscas no existe");
        mav.addObject("description", "Es posible que la URL esté mal escrita o que la página haya sido movida.");
        mav.addObject("iconClass", "fas fa-search");
        mav.addObject("requestUri", request.getRequestURI());
        
        return mav;
    }
    
    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public ModelAndView handleDatabaseError(HttpServletRequest request, Exception ex) {
        logDetailedError(request, ex, "DATABASE_ERROR");
        
        // Log adicional específico para errores de base de datos
        logger.error("=== ANÁLISIS ESPECÍFICO DE BD ===");
        if (ex instanceof SQLException sqlEx) {
            logger.error("SQL State: {}", sqlEx.getSQLState());
            logger.error("Error Code: {}", sqlEx.getErrorCode());
        }
        logger.error("===================================");
        
        ModelAndView mav = new ModelAndView("error/503");
        mav.addObject("errorCode", "503");
        mav.addObject("title", "Servicio no disponible");
        mav.addObject("message", "Problemas de conectividad con la base de datos");
        mav.addObject("description", "Estamos experimentando problemas técnicos. Nuestro equipo está trabajando para resolverlos.");
        mav.addObject("iconClass", "fas fa-database");
        mav.addObject("requestUri", request.getRequestURI());
        
        return mav;
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ModelAndView handleRuntimeException(HttpServletRequest request, RuntimeException ex) {
        logDetailedError(request, ex, "RUNTIME_EXCEPTION");
        
        ModelAndView mav = new ModelAndView("error/error");
        mav.addObject("errorCode", "500");
        mav.addObject("title", "Error interno del servidor");
        mav.addObject("message", "Ha ocurrido un error inesperado");
        mav.addObject("description", "Nuestro equipo técnico ha sido notificado y está trabajando para resolver el problema.");
        mav.addObject("iconClass", "fas fa-exclamation-triangle");
        mav.addObject("requestUri", request.getRequestURI());
        
        return mav;
    }
    
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(HttpServletRequest request, Exception ex) {
        logDetailedError(request, ex, "GENERIC_EXCEPTION");
        
        ModelAndView mav = new ModelAndView("error/error");
        mav.addObject("errorCode", "500");
        mav.addObject("title", "Error del sistema");
        mav.addObject("message", "Ha ocurrido un error inesperado");
        mav.addObject("description", "Nuestro equipo técnico ha sido notificado automáticamente.");
        mav.addObject("iconClass", "fas fa-exclamation-triangle");
        mav.addObject("requestUri", request.getRequestURI());
        
        return mav;
    }
}
