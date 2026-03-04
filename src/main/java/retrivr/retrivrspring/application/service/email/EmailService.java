package retrivr.retrivrspring.application.service.email;

public interface EmailService {

    void sendHtmlEmail(String toEmail, String subject, String htmlBody);
}
