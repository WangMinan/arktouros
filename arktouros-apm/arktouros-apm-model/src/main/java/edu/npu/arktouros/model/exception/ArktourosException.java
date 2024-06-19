package edu.npu.arktouros.model.exception;

import edu.npu.arktouros.model.common.ResponseCodeEnum;
import lombok.Getter;

/**
 * @author : [wangminan]
 * @description : 自定义异常类
 */
@SuppressWarnings("CallToPrintStackTrace")
@Getter
public class ArktourosException extends RuntimeException{

    private final ResponseCodeEnum code;
    private final String message;

    public ArktourosException(Throwable cause) {
        super(cause);
        cause.printStackTrace();
        this.code = ResponseCodeEnum.SERVER_ERROR;
        this.message = cause.getMessage();
    }

    public ArktourosException(Throwable cause, String message) {
        super(cause);
        cause.printStackTrace();
        this.code = ResponseCodeEnum.SERVER_ERROR;
        this.message = message;
    }

    public ArktourosException(Throwable cause, ResponseCodeEnum code, String message) {
        super(cause);
        cause.printStackTrace();
        this.code = code;
        this.message = message;
    }

    public ArktourosException(String message) {
        super(message);
        this.message = message;
        this.code = ResponseCodeEnum.SERVER_ERROR;
    }

    public ArktourosException(ResponseCodeEnum responseCodeEnum, String message) {
        super(message);
        this.code = responseCodeEnum;
        this.message = message;
    }
}
