package org.hyw.tools.generator.web.handler;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.hyw.tools.generator.web.enums.StatusCode;
import org.hyw.tools.generator.web.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IOException.class)
    @ResponseBody
    public ResponseEntity<Result<?>> handleIOException(IOException e) {
        logger.error("处理 IO 异常: {}", e.getMessage(), e);
        String errorMessage = StringUtils.replace(e.getMessage(), "Directory", "目录")
            .replace("could not be created", "无法创建");
        Result<?> errorResult = Result.error(StatusCode.DIR_CREATE_ERROR.getCode(), errorMessage);
        return new ResponseEntity<>(errorResult, HttpStatus.INTERNAL_SERVER_ERROR); 
    }

}