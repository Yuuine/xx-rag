package yuuine.xxrag.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupBanner implements CommandLineRunner {

    @Value("${server.port:8083}")
    private int port;

    @Override
    public void run(String... args) {

        String baseUrl = "http://localhost:" + port;

        System.out.println("\n" +
                "==============================================\n" +
                "               应用启动成功\n" +
                "----------------------------------------------\n" +
                "        访问地址: " + baseUrl + "\n" +
                "==============================================\n");
    }
}