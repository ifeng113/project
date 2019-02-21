package com.eairlv.sanner.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author lv
 * @create 2018-06-13 14:54
 * @desc
 **/
@Data
@ConfigurationProperties(ScanRunnerProperties.SCAN_RUNNER_PREFIX)
public class ScanRunnerProperties {

    public static final String SCAN_RUNNER_PREFIX = "scanner";

    /**
     * 网络协议
     */
    private String protocol = "http";

    /**
     * 注册目标微服务
     */
    private String service = "DODO-SERVER-APP-MANAGER-V2";

    /**
     * 注册目标微服务接口
     */
    private String url = "/appManager/microservice/resource";

    /**
     * 是否开启扫描器
     */
    private Boolean enable = true;

    /**
     * 黑名单禁止配置项
     */
    private ScanInterceptor prohibit = new ScanInterceptor();

    /**
     * 允许JWT访问配置项
     */
    private ScanInterceptor jwt = new ScanInterceptor();

    /**
     * 允许直接访问配置项
     */
    private ScanInterceptor allow = new ScanInterceptor();

    public static class ScanInterceptor {

        /**
         * url，多个以;号分隔。如需指定请求方式在url后加'#'号加方式，如：'/a/b#GET'
         */
        private String url;

        /**
         * url前缀匹配，多个以;号分隔
         */
        private String prefix;

        /**
         * 包名（需全名），多个以;号分隔
         */
        private String packageName;

        /**
         * 类名（含包名），多个以;号分隔
         */
        private String className;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }
    }
}
