package retrivr.retrivrspring.global.aws;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloud.aws.s3")
@Getter
@NoArgsConstructor
public class S3Properties {
  private String bucket;
  private String region;
  private Long putExpiration;
  private Long getExpiration;
}
