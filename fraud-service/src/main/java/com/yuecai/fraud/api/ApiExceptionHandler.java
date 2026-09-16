package com.yuecai.fraud.api;

import com.yuecai.fraud.batch.BatchJobNotFoundException;
import com.yuecai.fraud.modelclient.ModelServiceException;
import com.yuecai.fraud.storage.ObjectNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import com.yuecai.fraud.prediction.BatchNotFoundException;
import com.yuecai.fraud.prediction.EmptyBatchException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Maps domain exceptions to RFC 9457 problem details for the REST API. */
@RestControllerAdvice(basePackageClasses = PredictionController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler({IllegalArgumentException.class, MaxUploadSizeExceededException.class})
    ProblemDetail badRequest(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(EmptyBatchException.class)
    ProblemDetail emptyBatch(EmptyBatchException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setProperty("skipped", e.getSkipped());
        return pd;
    }

    @ExceptionHandler({BatchNotFoundException.class, BatchJobNotFoundException.class, ObjectNotFoundException.class})
    ProblemDetail notFound(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** e.g. retrying a job that is not FAILED, or downloading a result that does not exist yet. */
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflict(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Circuit breaker open: the model service has been failing; fail fast instead of piling on. */
    @ExceptionHandler(CallNotPermittedException.class)
    ProblemDetail circuitOpen(CallNotPermittedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Model service is temporarily unavailable (circuit open); try again shortly");
    }

    @ExceptionHandler(ModelServiceException.class)
    ProblemDetail modelDown(ModelServiceException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
}
