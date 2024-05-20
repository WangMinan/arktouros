package edu.npu.arktouros.model.exception;

import edu.npu.arktouros.model.vo.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author : [wangminan]
 * @description : 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class ArktourosExceptionHandler {

    @ExceptionHandler(ArktourosException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R handleArktourosException(ArktourosException e){
        log.error("捕获自定义异常ArktourosException:{}", e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R doException(Exception e){
        log.error("捕获全局异常:{}",e.getMessage());
        return R.error(e.getMessage());
    }
}
