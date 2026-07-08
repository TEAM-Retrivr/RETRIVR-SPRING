package retrivr.retrivrspring.global.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  private final CorsPolicyProperties corsPolicyProperties;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins(corsPolicyProperties.getAllowedOrigins().toArray(String[]::new))
        .allowedMethods(corsPolicyProperties.getAllowedMethods().toArray(String[]::new))
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
  }
}
