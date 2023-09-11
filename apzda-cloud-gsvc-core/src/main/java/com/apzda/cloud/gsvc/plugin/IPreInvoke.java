package com.apzda.cloud.gsvc.plugin;

import com.apzda.cloud.gsvc.core.ServiceMethod;
import org.springframework.web.servlet.function.ServerRequest;

/**
 * @author fengz
 */
public interface IPreInvoke extends IPlugin {

    Object preInvoke(ServerRequest request, Object data, ServiceMethod method);

}
