package retrivr.retrivrspring.application.port.message;

public record NotificationRecipient(
    String email,
    String phoneNumber
) {

  public String resolve(NotificationChannel channel) {
    return switch (channel) {
      case EMAIL -> email;
      case ALIM_TALK -> phoneNumber;
    };
  }

  public boolean hasRecipientFor(NotificationChannel channel) {
    return resolve(channel) != null;
  }
}
