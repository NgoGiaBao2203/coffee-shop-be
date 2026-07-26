package coffee.api;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ApiApplication {
  @PostConstruct
  public void init() {
    // Set JVM default time zone to Vietnam (ICT / UTC+7)
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
  }

  public static void main(String[] args) {
    SpringApplication.run(ApiApplication.class, args);
  }

}
