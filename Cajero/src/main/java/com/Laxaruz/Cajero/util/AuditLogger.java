package com.Laxaruz.Cajero.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Utilidad para logging de auditoría y debug en la aplicación bancaria
 */
public class AuditLogger {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("BANKING_AUDIT");
    private static final Logger debugLogger = LoggerFactory.getLogger("DEBUG");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * Log de transacciones bancarias para auditoría
     */
    public static void logBankingTransaction(String transactionType, String accountNumber, 
                                           double amount, String clientId, String result) {
        String transactionId = UUID.randomUUID().toString().substring(0, 8);
        
        MDC.put("transactionId", transactionId);
        MDC.put("transactionType", transactionType);
        MDC.put("timestamp", LocalDateTime.now().format(formatter));
        
        try {
            auditLogger.info("BANKING_TRANSACTION | ID: {} | TYPE: {} | ACCOUNT: {} | AMOUNT: {} | CLIENT: {} | RESULT: {} | TIMESTAMP: {}", 
                transactionId, transactionType, accountNumber, amount, clientId, result, LocalDateTime.now().format(formatter));
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Log de intentos de login para seguridad
     */
    public static void logLoginAttempt(String accountNumber, String clientIp, boolean success, String reason) {
        String attemptId = UUID.randomUUID().toString().substring(0, 8);
        
        MDC.put("attemptId", attemptId);
        MDC.put("eventType", "LOGIN_ATTEMPT");
        
        try {
            auditLogger.info("LOGIN_ATTEMPT | ID: {} | ACCOUNT: {} | IP: {} | SUCCESS: {} | REASON: {} | TIMESTAMP: {}", 
                attemptId, accountNumber, clientIp, success, reason, LocalDateTime.now().format(formatter));
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Log de cambios de PIN para seguridad
     */
    public static void logPinChange(String accountNumber, String clientId, String clientIp, boolean success) {
        String changeId = UUID.randomUUID().toString().substring(0, 8);
        
        MDC.put("changeId", changeId);
        MDC.put("eventType", "PIN_CHANGE");
        
        try {
            auditLogger.info("PIN_CHANGE | ID: {} | ACCOUNT: {} | CLIENT: {} | IP: {} | SUCCESS: {} | TIMESTAMP: {}", 
                changeId, accountNumber, clientId, clientIp, success, LocalDateTime.now().format(formatter));
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Log de bloqueos de cuenta
     */
    public static void logAccountBlock(String accountNumber, String reason, String adminUser) {
        String blockId = UUID.randomUUID().toString().substring(0, 8);
        
        MDC.put("blockId", blockId);
        MDC.put("eventType", "ACCOUNT_BLOCK");
        
        try {
            auditLogger.info("ACCOUNT_BLOCK | ID: {} | ACCOUNT: {} | REASON: {} | ADMIN: {} | TIMESTAMP: {}", 
                blockId, accountNumber, reason, adminUser, LocalDateTime.now().format(formatter));
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Log de errores críticos del sistema
     */
    public static void logCriticalError(String errorType, String description, String location, Exception ex) {
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        
        MDC.put("errorId", errorId);
        MDC.put("errorType", errorType);
        MDC.put("severity", "CRITICAL");
        
        try {
            auditLogger.error("CRITICAL_ERROR | ID: {} | TYPE: {} | LOCATION: {} | DESCRIPTION: {} | TIMESTAMP: {}", 
                errorId, errorType, location, description, LocalDateTime.now().format(formatter));
            
            if (ex != null) {
                auditLogger.error("CRITICAL_ERROR | ID: {} | EXCEPTION_DETAILS:", errorId, ex);
            }
        } finally {
            MDC.clear();
        }
    }

    /**
     * Log de performance para operaciones lentas
     */
    public static void logPerformanceWarning(String operation, long executionTimeMs, String details) {
        if (executionTimeMs > 1000) { // Solo loggear si toma más de 1 segundo
            String perfId = UUID.randomUUID().toString().substring(0, 8);
            
            MDC.put("perfId", perfId);
            MDC.put("operation", operation);
            
            try {
                auditLogger.warn("PERFORMANCE_WARNING | ID: {} | OPERATION: {} | EXECUTION_TIME: {}ms | DETAILS: {} | TIMESTAMP: {}", 
                    perfId, operation, executionTimeMs, details, LocalDateTime.now().format(formatter));
            } finally {
                MDC.clear();
            }
        }
    }
    
    /**
     * Obtener IP real del cliente
     */
    private static String getClientIpAddress(HttpServletRequest request) {
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
    
    /**
     * Helper method para medir tiempo de ejecución
     */
    public static class PerformanceTimer {
        private final long startTime;
        private final String operation;
        
        public PerformanceTimer(String operation) {
            this.operation = operation;
            this.startTime = System.currentTimeMillis();
        }
        
        public void finish(String details) {
            long executionTime = System.currentTimeMillis() - startTime;
            logPerformanceWarning(operation, executionTime, details);
        }
    }
}
