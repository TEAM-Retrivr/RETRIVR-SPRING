package retrivr.retrivrspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@EnableJpaAuditing
@ConfigurationPropertiesScan
@SpringBootApplication
public class RetrivrSpringApplication {

  public static void main(String[] args) {
    SpringApplication.run(RetrivrSpringApplication.class, args);
  }

}
