package com.agent.demo16;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackageClasses = Demo16StreamingSseApplication.class)
public class Demo16StreamingSseApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Demo16StreamingSseApplication.class);
        application.setWebApplicationType(WebApplicationType.REACTIVE);// 启动 Netty，提供响应式 HTTP 服务
        application.run(args);
    }
}
