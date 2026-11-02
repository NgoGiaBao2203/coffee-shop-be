package coffee.api.controllers;

import coffee.api.constants.Authority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
  @GetMapping("actuator/health")
  @PreAuthorize(Authority.OWNER)
  public String healthCheck() {
    return "OK";
  }
}
