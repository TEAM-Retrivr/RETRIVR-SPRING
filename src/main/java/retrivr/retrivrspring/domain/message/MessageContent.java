package retrivr.retrivrspring.domain.message;

public abstract class MessageContent {

  public final String getSubject() {
    return buildSubject();
  }

  public final String getMessage() {
    return prefix() + "\n" + buildBody();
  }

  protected String buildSubject() {
    return "[Retrivr] Notification";
  }

  protected String prefix() {
    return "[Retrivr]";
  }

  protected abstract String buildBody();
}
