package io.levelops.auth.utils;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class ControllerMethodFinder {
    private final RequestMappingHandlerMapping handlerMapping;
    private Map<RequestMappingInfo, HandlerMethod> handlerMethods;

    public ControllerMethodFinder(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @PostConstruct
    public void init() {
        handlerMethods = handlerMapping.getHandlerMethods();
    }

    /**
     * Finds a controller method based on the provided request URL and HTTP request method.
     *
     * @param requestUrl The URL of the incoming request.
     * @param requestMethod The HTTP request method (e.g. GET, POST) of the incoming request.
     * @return The method that matches the request URL and method; null, if no matching method is found.
     *
     */
    public Method findControllerMethod(String requestUrl, String requestMethod) {
        for (RequestMappingInfo requestMappingInfo : handlerMethods.keySet()) {

            Set<String> reqMethods = requestMappingInfo.getMethodsCondition().getMethods()
                    .stream().map(r -> r.name()).collect(Collectors.toSet());

            if (reqMethods.contains(requestMethod)) {
                List<String> list = requestMappingInfo.getPatternsCondition().getMatchingPatterns(requestUrl);

                if (list.size() > 0 ) {
                    if(list.size() > 1){
                        log.warn("More than one matching pattern found for request url {}, will return current method anyway - {} ",requestUrl, handlerMethods.get(requestMappingInfo).getMethod());
                    }
                    return handlerMethods.get(requestMappingInfo).getMethod();
                }
            }
        }

        return null; // Method not found
    }

}
