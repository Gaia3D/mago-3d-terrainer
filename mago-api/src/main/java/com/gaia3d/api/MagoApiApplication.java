package com.gaia3d.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MagoApiApplication {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        // 显式指定 JAXB 上下文工厂，解决 GeoTools GDALMetadataParser NPE 问题
        System.setProperty("javax.xml.bind.JAXBContextFactory", "com.sun.xml.bind.v2.ContextFactory");
        SpringApplication.run(MagoApiApplication.class, args);
    }
}
