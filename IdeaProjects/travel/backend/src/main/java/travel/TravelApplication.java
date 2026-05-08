package travel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
//import travel.utils.PasswordEncoderUtil;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@MapperScan("travel.mapper")
@EnableScheduling
@EnableRetry
public class TravelApplication {
    public static void main(String[] args) {
        //String encoded = PasswordEncoderUtil.encode("123456");
        //System.out.println(encoded);
        //上述为本地加密算法的123456暗文配对
        SpringApplication.run(TravelApplication.class, args);
    }
}