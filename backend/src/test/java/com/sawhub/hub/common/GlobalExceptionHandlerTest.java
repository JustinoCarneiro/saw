package com.sawhub.hub.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/** Achado de auditoria de UX (22/07/2026) — sem handleMethodArgumentNotValid, uma falha de @Valid
 * num @RequestBody (ex.: @Pattern do CNPJ em AtualizarDadosContratoRequest) caía no ProblemDetail
 * padrão do Spring, sem o campo "message" que o frontend usa (getApiErrorMessage) — confirmado ao
 * vivo: POST com CNPJ="123" devolvia {timestamp,status,error,path} sem "message" nenhum, e o
 * usuário via sempre o fallback genérico da tela em vez da mensagem específica escrita na
 * anotação. */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MethodParameter methodParameter;
    @Mock
    private BindingResult bindingResult;
    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleMethodArgumentNotValidDevolveAMensagemDoCampoComErro() {
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "cnpj", "CNPJ deve estar no formato 00.000.000/0000-00 ou 14 dígitos")));
        when(request.getRequestURI()).thenReturn("/api/v1/admin/mentorados/x/dados-contrato");

        ResponseEntity<ApiError> resposta = handler.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(methodParameter, bindingResult), request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().message())
                .isEqualTo("CNPJ deve estar no formato 00.000.000/0000-00 ou 14 dígitos");
    }

    @Test
    void handleMethodArgumentNotValidJuntaVariasMensagensSemRepetir() {
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "nome", "não deve estar em branco"),
                new FieldError("request", "email", "deve ser um endereço de e-mail bem formado")));
        when(request.getRequestURI()).thenReturn("/api/v1/leads");

        ResponseEntity<ApiError> resposta = handler.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(methodParameter, bindingResult), request);

        assertThat(resposta.getBody().message())
                .isEqualTo("não deve estar em branco; deve ser um endereço de e-mail bem formado");
    }

    @Test
    void handleMethodArgumentNotValidSemMensagemNenhumaUsaFallbackGenerico() {
        when(bindingResult.getFieldErrors()).thenReturn(List.of(new FieldError("request", "campo", null)));
        when(request.getRequestURI()).thenReturn("/api/v1/x");

        ResponseEntity<ApiError> resposta = handler.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(methodParameter, bindingResult), request);

        assertThat(resposta.getBody().message()).isEqualTo("Dados inválidos.");
    }
}
