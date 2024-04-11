package com.apzda.cloud.gsvc.exception;

import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

public interface IExceptionHandler {

    ServerResponse handle(Throwable error, ServerRequest request);

}
