package com.forpets.global.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.forpets.domain.post.exception.PostErrorCode;
import com.forpets.domain.review.exception.ReviewErrorCode;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
/**
 * 컨트롤러 전역에서 발생한 예외를 공통 응답 형식으로 변환합니다.
 * 예상 가능한 예외는 warn 로그로 남기고, 예상하지 못한 예외는 error 로그로 남깁니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();

        log.warn("[BusinessException] code={}, message={}, path={}",
                errorCode.getCode(), exception.getMessage(), request.getRequestURI());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(ErrorResponse.of(errorCode, exception.getMessage(), request.getRequestURI())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        Throwable cause = exception.getCause();

        if (cause instanceof InvalidFormatException ife
                && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {

            String fieldName = ife.getPath().get(0).getFieldName();

            // enum 공통 처리
            String message = String.format("올바르지 않은 %s 값입니다", fieldName);
            log.warn("[InvalidEnumException] fieldName={}, path={}", fieldName, request.getRequestURI());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.fail(ErrorResponse.of(CommonErrorCode.VALIDATION_FAILED, message, request.getRequestURI())));
        }

        String message = "요청 데이터 형식이 올바르지 않습니다.";
        log.warn("[HttpMessageNotReadableException] message={}, path={}", message, request.getRequestURI());

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorResponse.of(CommonErrorCode.VALIDATION_FAILED, message, request.getRequestURI())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse(CommonErrorCode.VALIDATION_FAILED.getMessage());

        log.warn("[ValidationException] message={}, path={}", message, request.getRequestURI());

        if (request.getRequestURI().startsWith("/api/reviews")) {
            ErrorCode errorCode = CommonErrorCode.VALIDATION_FAILED;
            for (var fieldError : exception.getBindingResult().getFieldErrors()) {
                if ("rating".equals(fieldError.getField())) {
                    errorCode = ReviewErrorCode.INVALID_RATING;
                    message = fieldError.getDefaultMessage();
                    break;
                }
                if ("reviewComment".equals(fieldError.getField())) {
                    errorCode = ReviewErrorCode.INVALID_REVIEW_COMMENT;
                    message = fieldError.getDefaultMessage();
                    break;
                }
            }

            return ResponseEntity
                    .status(errorCode.getStatus())
                    .body(ApiResponse.fail(ErrorResponse.of(errorCode, message, request.getRequestURI())));
        }

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorResponse.of(CommonErrorCode.VALIDATION_FAILED, message, request.getRequestURI())));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        log.warn("[MethodArgumentTypeMismatchException] paramName={}, value={}, path={}",
                exception.getName(), exception.getValue(), request.getRequestURI());

        // 시터 검색 API면 INVALID_SEARCH_CONDITION
        if (request.getRequestURI().startsWith("/api/sitters")) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.fail(ErrorResponse.of(SitterErrorCode.INVALID_SEARCH_CONDITION, request.getRequestURI())));
        }

        // 공고 API 파라미터별 에러코드 분기 추가
        String paramName = exception.getName();
        ErrorCode errorCode = switch (paramName) {
            case "careType" -> PostErrorCode.INVALID_CARE_TYPE;
            case "status" -> PostErrorCode.INVALID_POST_STATUS;
            case "region" -> PostErrorCode.INVALID_SEARCH_CONDITION;  // 또는 CommonErrorCode
            default -> CommonErrorCode.INVALID_PARAMETER;
        };

        // 그 외 API는 공통 에러코드
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorResponse.of(errorCode , request.getRequestURI())));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        String message = exception.getConstraintViolations()
                .stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(CommonErrorCode.VALIDATION_FAILED.getMessage());

        log.warn("[ConstraintViolationException] message={}, path={}", message, request.getRequestURI());

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorResponse.of(CommonErrorCode.VALIDATION_FAILED, message, request.getRequestURI())));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        String message = exception.getParameterName() + " 파라미터는 필수입니다.";

        log.warn("[MissingRequestParameter] message={}, path={}", message, request.getRequestURI());

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(ErrorResponse.of(CommonErrorCode.INVALID_REQUEST, message, request.getRequestURI())));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        log.warn("[MethodNotAllowed] method={}, path={}", exception.getMethod(), request.getRequestURI());

        return ResponseEntity
                .status(CommonErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(ApiResponse.fail(ErrorResponse.of(CommonErrorCode.METHOD_NOT_ALLOWED, request.getRequestURI())));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        log.warn("[AccessDeniedException] message={}, path={}", exception.getMessage(), request.getRequestURI());

        return ResponseEntity
                .status(CommonErrorCode.FORBIDDEN.getStatus())
                .body(ApiResponse.fail(ErrorResponse.of(CommonErrorCode.FORBIDDEN, request.getRequestURI())));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        log.debug("[NoResourceFoundException] message={}, path={}", exception.getMessage(), request.getRequestURI());

        return ResponseEntity
                .status(CommonErrorCode.NOT_FOUND.getStatus())
                .body(ApiResponse.fail(ErrorResponse.of(CommonErrorCode.NOT_FOUND, request.getRequestURI())));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn("[DataIntegrityViolationException] message={}, path={}", exception.getMessage(), request.getRequestURI());

        return ResponseEntity
                .status(CommonErrorCode.CONFLICT.getStatus())
                .body(ApiResponse.fail(ErrorResponse.of(CommonErrorCode.CONFLICT, request.getRequestURI())));
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handlePessimisticLockingFailureException(
            PessimisticLockingFailureException exception,
            HttpServletRequest request
    ) {
        log.warn("[PessimisticLockingFailureException] path={}", request.getRequestURI(), exception);

        return ResponseEntity
                .status(CommonErrorCode.CONFLICT.getStatus())
                .body(ApiResponse.fail(ErrorResponse.of(
                        CommonErrorCode.CONFLICT,
                        request.getRequestURI()
                )));
    }

//    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
//    public ResponseEntity<ApiResponse<Void>> handleObjectOptimisticLockingFailureException(
//            ObjectOptimisticLockingFailureException exception,
//            HttpServletRequest request)
//    {
//        log.warn("[ObjectOptimisticLockingFailureException] path={}", request.getRequestURI(), exception);
//
//        return ResponseEntity
//                .status(CommonErrorCode.CONFLICT.getStatus())
//                .body(ApiResponse.fail(ErrorResponse.of(
//                        CommonErrorCode.CONFLICT,
//                        request.getRequestURI()
//                )));
//    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("[UnhandledException] path={}", request.getRequestURI(), exception);

        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI())));
    }
}
