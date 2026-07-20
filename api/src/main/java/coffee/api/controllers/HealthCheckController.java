package coffee.api.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
  @GetMapping("heath-check")
  @PreAuthorize("hasRole('OWNER')")
  public String healthCheck() {
    return "OK";
  }
}
