package com.castorama.atg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * General business-rule violation.
 * ATG analogy: a pipeline processor returning PipelineConstants.STOP_CHAIN_EXECUTION
 * and adding a FormError to the form handler — the chain is halted and the user
 * sees a meaningful error.
 *
 * <p>Examples: adding an out-of-stock item to cart, attempting checkout on an
 * empty cart, registering with a duplicate email.</p>
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
