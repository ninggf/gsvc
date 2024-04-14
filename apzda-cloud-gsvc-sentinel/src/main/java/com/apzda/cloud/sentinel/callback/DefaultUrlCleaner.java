package com.apzda.cloud.sentinel.callback;

import com.apzda.cloud.adapter.spring.callback.UrlCleaner;

/**
 * @author fengz
 */
public class DefaultUrlCleaner implements UrlCleaner {

    @Override
    public String clean(String originUrl) {

        return originUrl;
    }

}
