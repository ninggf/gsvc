package com.apzda.cloud.sentinel.callback;


import com.alibaba.csp.sentinel.adapter.web.common.UrlCleaner;

/**
 * @author fengz
 */
public class DefaultUrlCleaner implements UrlCleaner {

    @Override
    public String clean(String originUrl) {

        return originUrl;
    }

}
