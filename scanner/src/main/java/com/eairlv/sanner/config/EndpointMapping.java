package com.eairlv.sanner.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.web.servlet.AbstractWebMvcEndpointHandlerMapping;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.util.Map;

@Slf4j
public class EndpointMapping implements ApplicationContextAware {

    public static final String MAPPING1_x = "org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping";
    public static final String MAPPING2_x = "org.springframework.boot.actuate.endpoint.web.servlet.AbstractWebMvcEndpointHandlerMapping";

    private ApplicationContext applicationContext;

    private static Map<RequestMappingInfo, HandlerMethod> result = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 获取端点的资源映射
     * @return
     */
    public Map<RequestMappingInfo, HandlerMethod> getEndpointMapping(){
        Object obj;
        try {
            if (Class.forName(MAPPING2_x) != null){
                obj = applicationContext.getBean(AbstractWebMvcEndpointHandlerMapping.class);
                if (obj != null){
                    result = ((AbstractWebMvcEndpointHandlerMapping) obj).getHandlerMethods();
                }
            }
        } catch (Exception e){
            log.warn("not find endpointMapping for spring boot 2.x");
        }
        try {
            if (Class.forName(MAPPING1_x) != null){
                obj = applicationContext.getBean(EndpointHandlerMapping.class);
                if (obj != null){
                    result = ((EndpointHandlerMapping) obj).getHandlerMethods();
                }
            }
        } catch (Exception e){
            log.warn("not find endpointMapping for spring boot 1.x");
        }
        if (result == null){
            log.warn("no endpointMapping");
        }
        return result;
    }
}
