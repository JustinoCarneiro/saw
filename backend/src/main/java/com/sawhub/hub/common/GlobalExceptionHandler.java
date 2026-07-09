package com.sawhub.hub.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.DateTimeException;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "Forbidden", "Você não tem acesso a este módulo.", request.getRequestURI()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNotFound(NoSuchElementException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "Not Found", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "Bad Request", ex.getMessage(), request.getRequestURI()));
    }

    // Violação de máquina de estado (ex.: liquidar conta já liquidada) é rejeição de regra de
    // negócio, não erro interno — sem isto virava 500 genérico (achado da revisão de segurança
    // do E14), escondendo do cliente a diferença entre "seu pedido é inválido" e "quebrou aqui".
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleConflict(IllegalStateException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "Conflict", ex.getMessage(), request.getRequestURI()));
    }

    // Lock otimista (@Version, achado da revisão de segurança do E14): duas requisições
    // concorrentes sobre a mesma linha (ex.: duplo-clique em "Liquidar conta") — a segunda
    // perde a corrida e recebe 409 em vez de silenciosamente corromper o dado.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "Conflict",
                        "Este registro foi alterado por outra operação simultânea. Recarregue e tente novamente.",
                        request.getRequestURI()));
    }

    // Ex.: ?ano=2026&mes=13 no DRE/dashboard de faturamento (YearMonth.of rejeita mês inválido).
    @ExceptionHandler(DateTimeException.class)
    public ResponseEntity<ApiError> handleDateTime(DateTimeException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "Bad Request", "Data ou período inválido.", request.getRequestURI()));
    }

    // @Validated em @RequestParam (ex.: @Min/@Max no ano/mês do DRE) lança isto, não
    // MethodArgumentNotValidException (que é só pra @RequestBody).
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "Bad Request", "Parâmetro inválido.", request.getRequestURI()));
    }

    // Upload de áudio da ata (M06) acima de spring.servlet.multipart.max-file-size — sem isto
    // virava 500 genérico (achado da revisão de segurança do M06, mesma classe do achado H14
    // sobre erro de negócio vs. erro interno).
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiError.of(413, "Payload Too Large", "Arquivo excede o tamanho máximo permitido.", request.getRequestURI()));
    }
}
