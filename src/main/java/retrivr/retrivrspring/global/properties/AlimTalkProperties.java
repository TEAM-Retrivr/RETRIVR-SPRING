package retrivr.retrivrspring.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.alim-talk")
public record AlimTalkProperties(
    String host,
    String userId,
    String profileKey
) {
}
