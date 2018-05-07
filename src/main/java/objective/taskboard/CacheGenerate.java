package objective.taskboard;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
public class CacheGenerate implements CommandLineRunner {

    public static void main(String[] args ) {
        System.out.println("Running the cache generate!!");
        SpringApplication.run(CacheGenerate.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
    }
}


