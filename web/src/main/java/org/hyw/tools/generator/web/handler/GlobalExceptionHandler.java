package org.hyw.tools.generator.web.handler;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.hyw.tools.generator.web.enums.StatusCode;
import org.hyw.tools.generator.web.model.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IOException.class)
    @ResponseBody
    public ResponseEntity<Result<?>> handleIOException(IOException e) {
        Result<?> errorResult = Result.error(StatusCode.DIR_CREATE_ERROR.getCode(),
            StringUtils.replace(e.getMessage(), "Directory", "目录").replace("could not be created", "无法创建"));
        return new ResponseEntity<>(errorResult, HttpStatus.INTERNAL_SERVER_ERROR); 
    }

}