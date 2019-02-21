package com.eairlv.sanner.config;

import com.eairlv.sanner.entity.MappingUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author lv
 * @create 2018-05-14 13:36
 * @desc
 **/
@Slf4j
public class ScanRunner {

    private List<String> classes = new ArrayList<>();

    private static final List<MappingUrl> mappingUrlList = new ArrayList<>();

    public List<MappingUrl> getMappingUrlList() {
        return mappingUrlList;
    }

    /**
     * 开始扫描
     * @throws IOException
     */
    public void startScan() throws IOException {
        log.info("ScanRunner is starting");
        ClassLoader cl = getClass().getClassLoader();
        URL url = cl.getResource("");
        String fileUrl = java.net.URLDecoder.decode(url.getFile(),"utf-8");
        log.info("ScanRunner location: " + fileUrl);
        File[] files = new File(fileUrl).listFiles();
        if (files != null){
            // DEBUG 运行
            findFiles(files);
            classes.forEach(names ->{
                Class cls = null;
                try {
                    names = "/" + names.replace("\\", "/");
                    names = names.replace(fileUrl, "");
                    cls = Class.forName(names.replace("/", "."));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                if (cls != null){
                    handleClass(cls);
                }
            });
        } else {
            // JAR 运行
            JarFile jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                try {
                    Class cls = Class.forName(entry.getName().replace("/", ".").replace(".class", ""));
                    handleClass(cls);
                } catch (ClassNotFoundException ignored) {}
            }
        }
    }

    /**
     * 遍历获取所有JAVA文件
     * @param files
     */
    private void findFiles(File[] files){
        for (File file: files) {
            try {
                if (file.isDirectory()){
                    findFiles(file.listFiles());
                } else {
                    if (file.getName().endsWith(".class")){
                        classes.add(file.getAbsolutePath().replace(".class", ""));
                    }
                }
            } catch (Exception e){
                log.warn(e.getMessage());
            }
        }
    }

    /**
     * 处理扫描出来的Class类文件
     * @param cls
     */
    private void handleClass(Class cls){
        Method[] methods = cls.getDeclaredMethods();
        RequestMapping requestMapping = (RequestMapping) cls.getAnnotation(RequestMapping.class);
        String[] classUrl = null;
        RequestMethod[] classMethod = new RequestMethod[0];
        if (requestMapping != null){
            classUrl = requestMapping.value();
            classMethod = requestMapping.method();
        }
        for (Method method:methods){
            if (method.getAnnotation(AccessPermission.class) != null){
                findAnnotationData(method, classUrl, classMethod, method.getAnnotation(AccessPermission.class).jwt());
            }
        }
    }

    /**
     * 找到接口中的URL、请求方式
     * @param method
     * @param classUrl
     * @param classMethod
     */
    private void findAnnotationData(Method method, String[] classUrl, RequestMethod[] classMethod, boolean jwt){
        String[] urls = null;
        RequestMethod[] methods = new RequestMethod[0];
        RequestMethod restMethod = null;
        if (method.getAnnotation(RequestMapping.class) != null){
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            urls = requestMapping.value();
            methods = requestMapping.method();
        } else {
            Annotation[] annotations = method.getAnnotations();
            boolean findFlag = false;
            for (Annotation annotation: annotations){
                if (annotation.annotationType().equals(GetMapping.class)){
                    GetMapping getMapping = method.getAnnotation(GetMapping.class);
                    urls = getMapping.value();
                    restMethod = RequestMethod.GET;
                    findFlag = true;
                    break;
                } else if (annotation.annotationType().equals(PostMapping.class)){
                    PostMapping postMapping = method.getAnnotation(PostMapping.class);
                    urls = postMapping.value();
                    restMethod = RequestMethod.POST;
                    findFlag = true;
                    break;
                } else if (annotation.annotationType().equals(DeleteMapping.class)){
                    DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
                    urls = deleteMapping.value();
                    restMethod = RequestMethod.DELETE;
                    findFlag = true;
                    break;
                } else if (annotation.annotationType().equals(PatchMapping.class)){
                    PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
                    urls = patchMapping.value();
                    restMethod = RequestMethod.PATCH;
                    findFlag = true;
                    break;
                } else if (annotation.annotationType().equals(PutMapping.class)){
                    PutMapping putMapping = method.getAnnotation(PutMapping.class);
                    urls = putMapping.value();
                    restMethod = RequestMethod.PUT;
                    findFlag = true;
                    break;
                }
            }
            if (!findFlag){
                log.warn(method.getDeclaringClass().getPackage().getName() + ":" + method.getDeclaringClass().getName()
                        + ":" + method.getName() + " not find mapping");
                return;
            }
        }
        String packageName = method.getDeclaringClass().getPackage().getName();
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        if (restMethod == null){
            loadRequestMapping(classUrl, urls, classMethod, methods, packageName, className, methodName, jwt);
        } else {
            loadAppointMapping(classUrl, urls, restMethod, packageName, className, methodName, jwt);
        }
    }

    /**
     * 装载全方式url
     * @param classUrl
     * @param urls
     * @param classMethod
     * @param methods
     * @param packageName
     * @param className
     * @param methodName
     * @param jwt
     */
    private void loadRequestMapping(String[] classUrl, String[] urls, RequestMethod[] classMethod, RequestMethod[] methods,
                                    String packageName, String className, String methodName, boolean jwt){
        RequestMethod[] requestMethods = null;
        if (methods.length == 0){
            if (classMethod.length != 0){
                requestMethods = classMethod;
            }
        } else {
            requestMethods = methods;
        }
        if (classUrl != null){
            for (String urlPrefix: classUrl){
                if (urls.length == 0){
                    if (requestMethods == null){
                        mappingUrlList.add(MappingUrl.builder()
                                .url(urlFormat(urlPrefix))
                                .method(ScanRunnerAutoConfiguration.METHOD_ALL)
                                .packageName(packageName)
                                .className(className)
                                .methodName(methodName)
                                .interceptType(jwt?ScanRunnerAutoConfiguration.JWT:ScanRunnerAutoConfiguration.ALLOW)
                                .build());
                    } else {
                        for (RequestMethod requestMethod: requestMethods){
                            mappingUrlList.add(MappingUrl.builder()
                                    .url(urlFormat(urlPrefix))
                                    .method(requestMethod.toString())
                                    .packageName(packageName)
                                    .className(className)
                                    .methodName(methodName)
                                    .interceptType(jwt?ScanRunnerAutoConfiguration.JWT:ScanRunnerAutoConfiguration.ALLOW)
                                    .build());
                        }
                    }
                } else {
                    for (String urlSuffix: urls){
                        if (requestMethods == null){
                            mappingUrlList.add(MappingUrl.builder()
                                    .url(urlFormat(urlPrefix) + urlFormat(urlSuffix))
                                    .method(ScanRunnerAutoConfiguration.METHOD_ALL)
                                    .packageName(packageName)
                                    .className(className)
                                    .methodName(methodName)
                                    .interceptType(jwt?ScanRunnerAutoConfiguration.JWT:ScanRunnerAutoConfiguration.ALLOW)
                                    .build());
                        } else {
                            for (RequestMethod requestMethod: requestMethods){
                                mappingUrlList.add(MappingUrl.builder()
                                        .url(urlFormat(urlPrefix) + urlFormat(urlSuffix))
                                        .method(requestMethod.toString())
                                        .packageName(packageName)
                                        .className(className)
                                        .methodName(methodName)
                                        .interceptType(jwt?ScanRunnerAutoConfiguration.JWT:ScanRunnerAutoConfiguration.ALLOW)
                                        .build());
                            }
                        }
                    }
                }
            }
        } else {
            if (urls.length == 0){
                if (requestMethods == null){
                    mappingUrlList.add(MappingUrl.builder()
                            .url("")
                            .method(ScanRunnerAutoConfiguration.METHOD_ALL)
                            .packageName(packageName)
                            .className(className)
                            .methodName(methodName)
                            .interceptType(jwt?ScanRunnerAutoConfiguration.JWT:ScanRunnerAutoConfiguration.ALLOW)
                            .build());
                } else {
                    for (RequestMethod requestMethod: requestMethods){
                        mappingUrlList.add(MappingUrl.builder()
                                .url("")
                                .method(requestMethod.toString())
                                .packageName(packageName)
                                .className(className)
                                .methodName(methodName)
                                .interceptType(jwt?ScanRunnerAutoConfiguration.JWT:ScanRunnerAutoConfiguration.ALLOW)
                                .build());
                    }
                }
            } else {
                for (String urlSuffix: urls){
                    if (requestMethods == null){
                        mappingUrlList.add(MappingUrl.builder()
                                .url(urlFormat(urlSuffix))
                                .method(ScanRunnerAutoConfiguration.METHOD_ALL)
                                .packageName(packageName)
                                .className(className)
                                .methodName(methodName)
                                .interceptType(jwt?ScanRunnerAutoConfiguration.JWT:ScanRunnerAutoConfiguration.ALLOW)
                                .build());
                    } else {
                        for (RequestMethod requestMethod: requestMethods){
                            mappingUrlList.add(MappingUrl.builder()
                                    .url(urlFormat(urlSuffix))
                                    .method(requestMethod.toString())
                                    .packageName(packageName)
                                    .className(className)
                                    .methodName(methodName)
                                    .interceptType(jwt?ScanRunnerAutoConfiguration.JWT:ScanRunnerAutoConfiguration.ALLOW)
                                    .build());
                        }
                    }
                }
            }
        }
    }

    /**
     * 装载定方式url
     * @param classUrl
     * @param urls
     * @param method
     * @param packageName
     * @param className
     * @param methodName
     * @param jwt
     */
    private void loadAppointMapping(String[] classUrl, String[] urls, RequestMethod method,
                                    String packageName, String className, String methodName, boolean jwt){
        if (classUrl != null){
            for (String urlPrefix: classUrl){
                if (urls.length == 0){
                    mappingUrlList.add(MappingUrl.builder()
                            .url(urlFormat(urlPrefix))
                            .method(method.toString())
                            .packageName(packageName)
                            .className(className)
                            .methodName(methodName)
                            .interceptType(jwt?ScanRunnerAutoConfiguration.JWT:ScanRunnerAutoConfiguration.ALLOW)
                            .build());
                } else {
                    for (String urlSuffix: urls){
                        mappingUrlList.add(MappingUrl.builder()
                                .url(urlFormat(urlPrefix) + urlFormat(urlSuffix))
                                .method(method.toString())
                                .packageName(packageName)
                                .className(className)
                                .methodName(methodName)
                                .interceptType(jwt?ScanRunnerAutoConfiguration.JWT:ScanRunnerAutoConfiguration.ALLOW)
                                .build());
                    }
                }
            }
        } else {
            if (urls.length == 0){
                mappingUrlList.add(MappingUrl.builder()
                        .url("")
                        .method(method.toString())
                        .packageName(packageName)
                        .className(className)
                        .methodName(methodName)
                        .interceptType(jwt?ScanRunnerAutoConfiguration.JWT:ScanRunnerAutoConfiguration.ALLOW)
                        .build());
            } else {
                for (String urlSuffix: urls){
                    mappingUrlList.add(MappingUrl.builder()
                            .url(urlFormat(urlSuffix))
                            .method(method.toString())
                            .packageName(packageName)
                            .className(className)
                            .methodName(methodName)
                            .interceptType(jwt?ScanRunnerAutoConfiguration.JWT:ScanRunnerAutoConfiguration.ALLOW)
                            .build());
                }
            }

        }
    }

    /**
     * 格式化url
     * @param url
     * @return
     */
    public static String urlFormat(String url){
        if (!url.startsWith("/")){
            url = "/" + url;
        }
        if (url.endsWith("/")){
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
