package coffee.api.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.cors")
@Data
public class CorsProperties {
  private List<String> allowedOrigins;
  private List<String> allowedOriginPatterns;
  private List<String> allowedMethods;
  private List<String> allowedHeaders;
  private boolean allowCredentials;
}
