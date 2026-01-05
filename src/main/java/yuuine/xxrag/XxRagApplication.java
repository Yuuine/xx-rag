package yuuine.xxrag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class XxRagApplication {

    public static void main(String[] args) {

        SpringApplication.run(XxRagApplication.class, args);

    }
}