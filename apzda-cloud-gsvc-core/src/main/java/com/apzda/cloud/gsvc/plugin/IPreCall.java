/*
 * This file is part of gsvc created at 2023/9/10 by ningGf.
 */
package com.apzda.cloud.gsvc.plugin;

import com.apzda.cloud.gsvc.core.ServiceMethod;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author fengz
 */
public interface IPreCall extends IPlugin {

    WebClient.RequestBodySpec preCall(WebClient.RequestBodySpec request, Object data, ServiceMethod method);

}
