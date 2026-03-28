package retrivr.retrivrspring.domain.message;

import java.util.Map;

public abstract class MessageContent {

  public final String getSubject() {
    return buildSubject();
  }

  public final String getMessage() {
    return prefix() + "\n" + buildBody();
  }

  public final String getTemplateCode() {
    return buildTemplateCode();
  }

  public final Map<String, String> getTemplateVariables() {
    return buildTemplateVariables();
  }

  protected abstract String buildSubject();

  protected String prefix() {
    return "[Retrivr]";
  }

  protected String buildTemplateCode() {
    return null;
  }

  protected Map<String, String> buildTemplateVariables() {
    return Map.of();
  }

  protected abstract String buildBody();
}
