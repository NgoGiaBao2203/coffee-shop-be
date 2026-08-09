package coffee.api;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
