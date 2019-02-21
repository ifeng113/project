package com.eairlv.sanner.entity;

import com.eairlv.sanner.config.ScanRunner;
import com.eairlv.sanner.config.ScanRunnerProperties;
import com.google.gson.Gson;
import com.eairlv.sanner.config.ScanRunnerAutoConfiguration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Slf4j
public class ScanInterceptorTransducer {

    public ScanInterceptorTransducer(ScanRunnerProperties.ScanInterceptor scanInterceptor) {
        if (scanInterceptor.getUrl() != null){
            String[] urlAndMethods = scanInterceptor.getUrl().split(";");
            for (String urlAndMethod: urlAndMethods){
                if (urlAndMethod.split("#").length == 1){
                    url.add(UrlAndMethod.builder()
                            .url(ScanRunner.urlFormat(urlAndMethod))
                            .method(ScanRunnerAutoConfiguration.METHOD_ALL)
                            .build());
                } else if(urlAndMethod.split("#").length == 2) {
                    url.add(UrlAndMethod.builder()
                            .url(ScanRunner.urlFormat(urlAndMethod.split("#")[0]))
                            .method(urlAndMethod.split("#")[1])
                            .build());
                } else {
                    log.warn("scanInterceptor format error: " + new Gson().toJson(urlAndMethods));
                    url = new ArrayList<>();
                    prefix = new ArrayList<>();
                    className = new ArrayList<>();
                    packageName = new ArrayList<>();
                    return;
                }
            }
        }
        if (scanInterceptor.getPrefix() != null){
            prefix = Arrays.asList(scanInterceptor.getPrefix().split(";"));
        }
        if (scanInterceptor.getClassName() != null){
            className = Arrays.asList(scanInterceptor.getClassName().split(";"));
        }
        if (scanInterceptor.getPackageName() != null){
            packageName = Arrays.asList(scanInterceptor.getPackageName().split(";"));
        }
    }

    private List<UrlAndMethod> url = new ArrayList<>();

    private List<String> prefix = new ArrayList<>();

    private List<String> className = new ArrayList<>();

    private List<String> packageName = new ArrayList<>();
}
