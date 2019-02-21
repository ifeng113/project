package com.eairlv.sanner.config;

import com.eairlv.sanner.entity.ScanInterceptorTransducer;
import com.eairlv.sanner.entity.ScanResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.eairlv.sanner.entity.MappingUrl;
import com.eairlv.sanner.entity.UrlAndMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.Resource;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @author lv
 * @create 2018-05-16 9:46
 * @desc
 * todo 系统自带前缀类型的url处理，如：/webjar/** ???
 * todo 优化：获取系统所有mapping可使用spring-boot-starter-actuator的Bean接口（需注意spring boot版本与Bean接口）
 **/
@Configuration
@EnableConfigurationProperties(ScanRunnerProperties.class)
@Slf4j
public class ScanRunnerAutoConfiguration {

    public static final String PROHIBIT = "PROHIBIT";
    public static final String JWT = "JWT";
    public static final String ALLOW = "ALLOW";

    public static final String METHOD_ALL = "ALL";

    @Value("${spring.application.name}")
    private String microServiceName;

    @Autowired
    public ScanRunnerProperties scanRunnerProperties;

    /**
     * 注解@Resource，解决scannerRestTemplate错误依赖主程序注入的restTemplate
     * 注解@Lazy，解决主程序@Autowired注入restTemplate产生依赖循环
     * 影响：主程序如直接使用@Autowired注入restTemplate，则会默认使用scannerRestTemplate
     * 解决方法：主程序使用@Resource，或使用@Autowired+@Qualifier
     */
    @Resource
    @Lazy
    private RestTemplate scannerRestTemplate;

    @Autowired
    private ScanRunner scanRunner;

    @Autowired
    private RequestMappingHandlerMapping requestMapping;

    @Autowired
    public EndpointMapping endpointMapping;

    private static final ScanResult scanResult = new ScanResult();

    /**
     * 支持微服务名访问
     * 支持https
     * 使用Ribbon不使用Feign避免了Feign说设置的熔断时间过短导致注册失败
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    @Bean
    @LoadBalanced
    public RestTemplate scannerRestTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();

        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLSocketFactory(csf)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();

        requestFactory.setHttpClient(httpClient);
        return new RestTemplate(requestFactory);
    }

    /**
     *
     * @return
     * @throws IOException
     */
    @Bean
    public ScanRunner scanRunner() throws IOException {
        ScanRunner scanRunner = new ScanRunner();
        scanRunner.startScan();
        return scanRunner;
    }

    /**
     * 端点资源映射器
     * @return
     */
    @Bean
    EndpointMapping endpointMapping(){
        return new EndpointMapping();
    }

    /**
     * 扫描启动器
     * 待项目启动完成再开始扫描，防止项目其他Bean未初始化完成
     * @return
     */
    @Bean
    @ConditionalOnBean(ScanRunner.class)
    ScannerStarter scannerStarter(){
        return new ScannerStarter(this);
    }

    /**
     * 微服务配置权限过滤器
     */
    public void permissionFilter(Map<RequestMappingInfo, HandlerMethod> endpointMapping){
        List<MappingUrl> mappingUrlList = new ArrayList<>();
        mappingHandler(mappingUrlList, requestMapping.getHandlerMethods());
        if (endpointMapping != null){
            mappingHandler(mappingUrlList, endpointMapping);
        }
        interceptHandler(mappingUrlList, scanRunner.getMappingUrlList(),
                new ScanInterceptorTransducer(scanRunnerProperties.getProhibit()),
                new ScanInterceptorTransducer(scanRunnerProperties.getJwt()),
                new ScanInterceptorTransducer(scanRunnerProperties.getAllow()));
    }

    /**
     * mapping处理器
     * @param mappingUrlList
     * @param handlerMethods
     */
    private void mappingHandler(List<MappingUrl> mappingUrlList, Map<RequestMappingInfo, HandlerMethod> handlerMethods) {
        for (Map.Entry<RequestMappingInfo, HandlerMethod> m : handlerMethods.entrySet()){
            RequestMappingInfo requestMappingInfo = m.getKey();
            String packageName = m.getValue().getMethod().getDeclaringClass().getPackage().getName();
            String className = m.getValue().getMethod().getDeclaringClass().getName();
            String methodName = m.getValue().getMethod().getName();
            PatternsRequestCondition urlCondition = requestMappingInfo.getPatternsCondition();
            RequestMethodsRequestCondition methodCondition = requestMappingInfo.getMethodsCondition();
            for (String url: urlCondition.getPatterns()){
                if (!methodCondition.getMethods().isEmpty()){
                    for (RequestMethod method: methodCondition.getMethods()){
                        mappingUrlList.add(MappingUrl.builder()
                                .packageName(packageName)
                                .className(className)
                                .methodName(methodName)
                                .url(url)
                                .method(method.toString())
                                .interceptType(ScanRunnerAutoConfiguration.PROHIBIT)
                                .build());
                    }
                } else {
                    mappingUrlList.add(MappingUrl.builder()
                            .packageName(packageName)
                            .className(className)
                            .methodName(methodName)
                            .url(url)
                            .method(METHOD_ALL)
                            .interceptType(ScanRunnerAutoConfiguration.PROHIBIT)
                            .build());
                }
            }
        }
    }

    /**
     * 微服务配置权限发送器
     */
    public void permissionSender(){
        new Thread(() -> {
            while (true){
                try {
                    HttpHeaders headers = new HttpHeaders();
                    // 解决中文乱码
                    List<HttpMessageConverter<?>> converterList = scannerRestTemplate.getMessageConverters();
                    HttpMessageConverter<?> converter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
                    converterList.add(0, converter);

                    headers.setContentType(MediaType.valueOf("application/json;UTF-8"));
                    HttpEntity<String> result = new HttpEntity<>(new Gson().toJson(scanResult), headers);
                    JsonObject jsonObject = new JsonParser().parse(scannerRestTemplate.postForObject(
                            scanRunnerProperties.getProtocol() + "://" + scanRunnerProperties.getService() + scanRunnerProperties.getUrl(),
                            result, String.class)).getAsJsonObject();
                    int code = jsonObject.get("code").getAsInt();
                    if (code == 0){
                        log.info("registration of internal micro service OK");
                        break;
                    } else {
                        log.warn(jsonObject.get("message").getAsString() + ", retrying......");
                    }
                } catch (Exception e){
                    log.warn("connecting " + scanRunnerProperties.getService() + " failed, retrying......");
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ignore) {}
            }
        }).start();
    }

    /**
     * 拦截配置处理器
     * @param mappingUrlList
     * @param appointUrlList
     * @param prohibit
     * @param jwt
     * @param allow
     */
    private void interceptHandler(List<MappingUrl> mappingUrlList,
                                  List<MappingUrl> appointUrlList,
                                  ScanInterceptorTransducer prohibit,
                                  ScanInterceptorTransducer jwt,
                                  ScanInterceptorTransducer allow){
        // 增加jwt配置项
        Set<MappingUrl> jwtUrlSet = new HashSet<>();
        for (MappingUrl mappingUrl: mappingUrlList){
            interceptMatcher(jwt, jwtUrlSet, mappingUrl);
        }
        for (MappingUrl url: jwtUrlSet){
            url.setInterceptType(ScanRunnerAutoConfiguration.JWT);
        }
        appointUrlList.addAll(jwtUrlSet);

        // 增加allow配置项
        Set<MappingUrl> allowUrlSet = new HashSet<>();
        for (MappingUrl mappingUrl: mappingUrlList){
            interceptMatcher(allow, allowUrlSet, mappingUrl);
        }
        for (MappingUrl url: allowUrlSet){
            url.setInterceptType(ScanRunnerAutoConfiguration.ALLOW);
        }
        appointUrlList.addAll(allowUrlSet);

        // 排除prohibit配置项
        Set<MappingUrl> appointRemoveUrlSet = new HashSet<>();
        for (MappingUrl mappingUrl: appointUrlList){
            interceptMatcher(prohibit, appointRemoveUrlSet, mappingUrl);
        }
        for (MappingUrl mappingUrl: appointRemoveUrlSet){
            appointUrlList.remove(mappingUrl);
        }

        // 在禁止项中加入prohibit配置项
        mappingUrlList.addAll(appointRemoveUrlSet);

        // mapping分发
        Set<MappingUrl> mappingUrlSet = new HashSet<>();
        for (MappingUrl mappingUrl: mappingUrlList){
            for (MappingUrl appointUrl: appointUrlList){
                if (mappingUrl.getUrl().equals(appointUrl.getUrl())
                        && mappingUrl.getMethod().equals(appointUrl.getMethod())){
                    mappingUrlSet.add(mappingUrl);
                    break;
                }
            }
        }
        for (MappingUrl mappingUrl: mappingUrlSet){
            mappingUrlList.remove(mappingUrl);
        }

        for (MappingUrl mappingUrl: mappingUrlList) {
            scanResult.getUrlAndMethods().add(UrlAndMethod.builder()
                    .url(bracket(mappingUrl.getUrl()))
                    .method(mappingUrl.getMethod())
                    .interceptType(ScanRunnerAutoConfiguration.PROHIBIT)
                    .build());
        }

        for (MappingUrl mappingUrl: appointUrlList) {
            scanResult.getUrlAndMethods().add(UrlAndMethod.builder()
                    .url(bracket(mappingUrl.getUrl()))
                    .method(mappingUrl.getMethod())
                    .interceptType(mappingUrl.getInterceptType())
                    .build());
        }

        logScanResult(scanResult.getUrlAndMethods(), "ScanRunner");

        scanResult.setLock(UUID.randomUUID().toString());
        scanResult.setMicroServiceName(microServiceName);
    }

    /**
     * 打印扫描结果
     * @param urlAndMethods
     * @param description
     */
    private void logScanResult(List<UrlAndMethod> urlAndMethods, String description){
        log.info("---------------" + description + "---------------");
        for (UrlAndMethod urlAndMethod: urlAndMethods){
            log.info("url: " + urlAndMethod.getUrl() + " method: " + urlAndMethod.getMethod()
                    + " interceptType: " + urlAndMethod.getInterceptType());
        }
        log.info("---------------" + description + "---------------");
    }

    /**
     * 提取{}
     * @param location
     * @return
     */
    private String bracket(String location){
        int start = location.indexOf("{");
        int end = location.indexOf("}");
        if (start != -1){
            location = location.substring(0, start) + "*" + location.substring(end + 1, location.length());
            return bracket(location);
        }
        return location;
    }

    /**
     * 拦截匹配器
     * @param transducer
     * @param matchList
     * @param mappingUrl
     */
    private void interceptMatcher(ScanInterceptorTransducer transducer, Set<MappingUrl> matchList, MappingUrl mappingUrl) {
        if (!transducer.getUrl().isEmpty()){
            for (UrlAndMethod urlAndMethod: transducer.getUrl()){
                if (mappingUrl.getUrl().equals(urlAndMethod.getUrl())
                        && mappingUrl.getMethod().equals(urlAndMethod.getMethod())){
                    matchList.add(mappingUrl);
                }
            }
        }
        if (!transducer.getPrefix().isEmpty()){
            for (String prefix: transducer.getPrefix()){
                if (mappingUrl.getUrl().startsWith(prefix)){
                    matchList.add(mappingUrl);
                }
            }
        }
        if (!transducer.getClassName().isEmpty()){
            for (String className: transducer.getClassName()){
                if (mappingUrl.getClassName().equals(className)){
                    matchList.add(mappingUrl);
                }
            }
        }
        if (!transducer.getPackageName().isEmpty()){
            for (String packageName: transducer.getPackageName()){
                if (mappingUrl.getPackageName().equals(packageName)){
                    matchList.add(mappingUrl);
                }
            }
        }
    }
}
