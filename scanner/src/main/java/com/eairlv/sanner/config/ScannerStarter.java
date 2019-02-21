package com.eairlv.sanner.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * @author lv
 */
@Slf4j
public class ScannerStarter implements ApplicationRunner {

    private ScanRunnerAutoConfiguration scanRunnerAutoConfiguration;

    public ScannerStarter(ScanRunnerAutoConfiguration scanRunnerAutoConfiguration) {
        this.scanRunnerAutoConfiguration = scanRunnerAutoConfiguration;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (scanRunnerAutoConfiguration.scanRunnerProperties.getEnable()){
            scanRunnerAutoConfiguration.permissionFilter(scanRunnerAutoConfiguration.endpointMapping.getEndpointMapping());
            scanRunnerAutoConfiguration.permissionSender();
        } else {
            log.warn("scanner.connect.enable set false, micro service resource can't refresh");
        }
    }
}
