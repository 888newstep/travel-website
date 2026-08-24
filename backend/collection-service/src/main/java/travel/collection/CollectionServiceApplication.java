package travel.collection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@ComponentScan(basePackages = {"travel.collection", "travel.common"})
@MapperScan(basePackages = {"travel.collection.mapper", "travel.common.mapper"})
public class CollectionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollectionServiceApplication.class, args);
    }
}