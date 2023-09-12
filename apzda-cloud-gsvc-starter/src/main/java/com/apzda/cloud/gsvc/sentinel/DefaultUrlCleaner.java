package com.apzda.cloud.gsvc.sentinel;

import com.apzda.cloud.adapter.servlet.callback.UrlCleaner;

/**
 * @author fengz
 */
public class DefaultUrlCleaner implements UrlCleaner {

    @Override
    public String clean(String originUrl) {

        return originUrl;
    }

}
