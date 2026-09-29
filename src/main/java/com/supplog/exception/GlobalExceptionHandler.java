package com.supplog.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.supplog.service.admin.audit.AdminAuditService;
import com.supplog.service.user.impl.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageHelper messageHelper;
    private final AdminAuditService adminAuditService;

    public GlobalExceptionHandler(
            MessageHelper messageHelper,
            AdminAuditService adminAuditService
    ) {
        this.messageHelper = messageHelper;
        this.adminAuditService = adminAuditService;

    }

    //Resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        recordFailedAdminAction(request, ex);
        String localMessage = messageHelper.getMessage(ex.getMessage(), ex.getArgs());

        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(),
                "RESOURCE_NOT_FOUND",
                localMessage,
                LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    //business exception
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {
        recordFailedAdminAction(request, ex);

        String localMessage = messageHelper.getMessage(ex.getMessage(), ex.getArgs());

        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                "BUSINESS_RULE_VIOLATION",
                localMessage,
                LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    private void recordFailedAdminAction(
            HttpServletRequest request,
            Exception exception
    ) {
        if (request == null
                || !request.getRequestURI().startsWith("/api/v1/admin/")
                || "GET".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails admin)) {
            return;
        }

        try {
            adminAuditService.record(
                    admin.getId(),
                    resolveAdminAction(request),
                    resolveAdminResourceType(request.getRequestURI()),
                    resolveResourceId(request.getRequestURI()),
                    null,
                    null,
                    "request_failed",
                    false,
                    exception.getClass().getSimpleName()
            );
        } catch (RuntimeException auditException) {
            log.error("Failed to record admin audit entry", auditException);
        }
    }

    private String resolveAdminAction(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (path.contains("/users/")) {
            if (path.endsWith("/deactivate")) return "USER_DEACTIVATION_FAILED";
            if (path.endsWith("/activate")) return "USER_ACTIVATION_FAILED";
            if (path.endsWith("/role")) return "USER_ROLE_UPDATE_FAILED";
            if (path.endsWith("/updateProfile")) return "USER_PROFILE_UPDATE_FAILED";
            if (path.endsWith("/password")) return "USER_PASSWORD_RESET_FAILED";
        }

        if (path.contains("/supplements/")) {
            if (path.endsWith("/deactivate")) return "SUPPLEMENT_DEACTIVATION_FAILED";
            if (path.endsWith("/activate")) return "SUPPLEMENT_ACTIVATION_FAILED";
            return "SUPPLEMENT_UPDATE_FAILED";
        }

        if (path.contains("/routines/")) {
            if (path.endsWith("/deactivate")) return "ROUTINE_DEACTIVATION_FAILED";
            if (path.endsWith("/activate")) return "ROUTINE_ACTIVATION_FAILED";
            return "ROUTINE_UPDATE_FAILED";
        }

        if (path.contains("/executions/")) {
            return "EXECUTION_CORRECTION_FAILED";
        }

        if (path.contains("/support-relationships/")) {
            return "SUPPORT_RELATIONSHIP_INTERVENTION_FAILED";
        }

        return "ADMIN_MUTATION_FAILED";
    }

    private String resolveAdminResourceType(String path) {
        String remainder = path.substring("/api/v1/admin/".length());
        int separatorIndex = remainder.indexOf('/');
        String segment = separatorIndex >= 0
                ? remainder.substring(0, separatorIndex)
                : remainder;

        return switch (segment) {
            case "users" -> "USER";
            case "supplements" -> "SUPPLEMENT";
            case "routines" -> "ROUTINE";
            case "executions" -> "ROUTINE_EXECUTION";
            case "support-relationships" -> "SUPPORT_RELATIONSHIP";
            default -> segment.isBlank()
                    ? "ADMIN"
                    : segment.toUpperCase().replace('-', '_');
        };
    }

    private Long resolveResourceId(String path) {
        for (String segment : path.split("/")) {
            try {
                return Long.valueOf(segment);
            } catch (NumberFormatException ignored) {
                // Resource id olmayan path parçaları atlanır.
            }
        }
        return null;
    }


    //method valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        recordFailedAdminAction(request, ex);
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                messageHelper.getMessage("validation.error"),
                LocalDateTime.now(),
                errors
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    //auth exception
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex
    ) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_CREDENTIALS",
                messageHelper.getMessage("auth.credentials.invalid"),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    //like validation
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        recordFailedAdminAction(request, ex);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_REQUEST_BODY",
                messageHelper.getMessage("request.body.invalid"),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        recordFailedAdminAction(request, ex);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "DATA_INTEGRITY_VIOLATION",
                messageHelper.getMessage("data.integrity.violation"),
                LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse>
    handleMethodValidation(
            HandlerMethodValidationException ex,
            HttpServletRequest request
    ) {
        recordFailedAdminAction(request, ex);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "METHOD_VALIDATION_ERROR",
                messageHelper.getMessage(
                        "validation.id.positive"
                ),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {

        log.error("Unexpected exception occurred", ex);
        recordFailedAdminAction(request, ex);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                messageHelper.getMessage("internal.server.error"),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        recordFailedAdminAction(request, ex);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_PARAMETER_TYPE",
                messageHelper.getMessage("request.parameter.type.invalid"),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}
