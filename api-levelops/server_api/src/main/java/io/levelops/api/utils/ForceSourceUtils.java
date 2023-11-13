package io.levelops.api.utils;

import org.apache.commons.lang3.StringUtils;

public class ForceSourceUtils {
    /**
     * useES
     * @param forceSource
     * @return If forceSource=es then this function returns True which means force fetch from ES
     *          If forceSource=db then this function returns True which means force fetch from DB
     *          For everything else this function returns null which means no force, get data from wherever tenants is configured
     */
    public static Boolean useES(String forceSource) {
        if(StringUtils.equals(forceSource, "es")) {
            return true;
        }
        if(StringUtils.equals(forceSource, "db")) {
            return false;
        }
        return null;
    }
}
