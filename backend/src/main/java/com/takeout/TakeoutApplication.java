package com.takeout;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 外卖系统启动类
 *
 * <p>三端一体：顾客端(/customer) / 商家端(/merchant) / 管理后台(/admin)，
 * 模块单体化架构，业务模块之间只通过各自 api 包契约通信。</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.takeout.**.mapper")
@EnableScheduling
@EnableAsync
public class TakeoutApplication {

    public static void main(String[] args) {
        SpringApplication.run(TakeoutApplication.class, args);
    }
}