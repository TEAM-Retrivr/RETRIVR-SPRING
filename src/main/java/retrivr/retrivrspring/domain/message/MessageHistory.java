package retrivr.retrivrspring.domain.message;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;
import retrivr.retrivrspring.domain.entity.rental.Rental;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageHistory extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Rental rental;

    private String recipient;

  @Enumerated(EnumType.STRING)
  private MessageType messageType;

  @Enumerated(EnumType.STRING)
  private MessageSendStatus status;

  @Column(columnDefinition = "TEXT")
  private String content;

  private LocalDate sentDate;

  public static MessageHistory create(
      Rental rental,
      String recipient,
      MessageType messageType,
      MessageSendStatus status,
      String content,
      LocalDate sentDate
  ) {
    return new MessageHistory(rental, recipient, messageType, status, content, sentDate);
  }

  private MessageHistory(
      Rental rental,
      String recipient,
      MessageType messageType,
      MessageSendStatus status,
      String content,
      LocalDate sentDate
  ) {
    this.rental = rental;
    this.recipient = recipient;
    this.messageType = messageType;
    this.status = status;
    this.content = content;
    this.sentDate = sentDate;
  }
}
