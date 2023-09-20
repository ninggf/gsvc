package com.apzda.cloud.gsvc.plugin;

import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

/**
 * @author fengz
 */
public interface IPostInvoke extends IPlugin {

    /**
     * @param requestObj 请求对象
     * @param returnObj Flu
     * @param method 当前执行的方法
     * @return 返回对象
     */
    Object postInvoke(Mono<JsonNode> requestObj, Object returnObj, ServiceMethod method);

}
