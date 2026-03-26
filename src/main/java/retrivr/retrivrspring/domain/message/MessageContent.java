package retrivr.retrivrspring.domain.message;

public abstract class MessageContent {

  public final String getMessage() {
    return prefix() + "\n" + buildBody();
  }

  protected String prefix() {
    return "[Retrivr]";
  }

  protected abstract String buildBody();
}
