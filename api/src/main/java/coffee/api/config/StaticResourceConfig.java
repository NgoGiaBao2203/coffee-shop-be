package coffee.api.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
    Path uploadDir = Paths.get("uploads/drinks").toAbsolutePath().normalize();
    String uploadUri = uploadDir.toUri().toString();

    if (!uploadUri.endsWith("/")) {
      uploadUri += "/";
    }

    registry.addResourceHandler("/uploads/drinks/**").addResourceLocations(uploadUri);
  }
}
