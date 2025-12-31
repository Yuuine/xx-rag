package yuuine.xxrag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.modulith.ApplicationModule;

@SpringBootApplication
@ApplicationModule
@EnableFeignClients(basePackages = "yuuine.xxrag.app.client")
@ComponentScan(basePackages = {"yuuine.xxrag.app", "yuuine.xxrag.inference"})
public class XxRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(XxRagApplication.class, args);
    }

}
