package travel.attraction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
@ComponentScan(basePackages = {"travel.attraction", "travel.common"})
@MapperScan(basePackages = {"travel.attraction.mapper", "travel.common.mapper"})
public class AttractionServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(AttractionServiceApplication.class, args);
    }
}
