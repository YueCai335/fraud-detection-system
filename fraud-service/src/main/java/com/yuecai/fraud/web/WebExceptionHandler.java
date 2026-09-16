package com.yuecai.fraud.web;

import com.yuecai.fraud.batch.BatchJobNotFoundException;
import com.yuecai.fraud.prediction.BatchNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Page-side error mapping (the REST API has its own problem-detail handler). */
@ControllerAdvice(basePackageClasses = WebExceptionHandler.class)
public class WebExceptionHandler {

    @ExceptionHandler({BatchJobNotFoundException.class, BatchNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound() {
        return "error/404";
    }
}
