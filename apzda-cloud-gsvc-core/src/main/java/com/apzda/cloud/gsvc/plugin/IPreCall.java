/*
 * This file is part of gsvc created at 2023/9/10 by ningGf.
 */
package com.apzda.cloud.gsvc.plugin;

import com.apzda.cloud.gsvc.core.ServiceMethod;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author fengz
 */
public interface IPreCall extends IPlugin {

    @NonNull
    WebClient.RequestBodySpec preCall(@NonNull WebClient.RequestBodySpec request, @Nullable Object data,
            @Nullable ServiceMethod method);

}
