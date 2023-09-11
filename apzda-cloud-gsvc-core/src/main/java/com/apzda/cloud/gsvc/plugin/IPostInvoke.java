package com.apzda.cloud.gsvc.plugin;

import com.apzda.cloud.gsvc.core.ServiceMethod;
import org.springframework.web.servlet.function.ServerRequest;

/**
 * @author fengz
 */
public interface IPostInvoke extends IPlugin {

    Object postInvoke(ServerRequest request, Object requestObj, Object data, ServiceMethod method);

}
