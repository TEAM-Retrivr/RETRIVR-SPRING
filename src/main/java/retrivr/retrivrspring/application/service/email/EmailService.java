package retrivr.retrivrspring.application.service.email;

public interface EmailService {

    void sendTextEmail(String toEmail, String subject, String body);

    void sendHtmlEmail(String toEmail, String subject, String htmlBody);
}
