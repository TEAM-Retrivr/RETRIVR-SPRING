package retrivr.retrivrspring.domain.repository.message;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.message.MessageHistory;
import retrivr.retrivrspring.domain.message.MessageSendStatus;
import retrivr.retrivrspring.domain.message.MessageType;

public interface MessageHistoryRepository extends JpaRepository<MessageHistory, Long> {

  boolean existsByRentalAndSentDateAndMessageType(Rental rental, LocalDate sentDate, MessageType messageType);

  List<MessageHistory> findByRentalInAndMessageTypeAndStatusOrderBySentDateDesc(
      List<Rental> rentals,
      MessageType messageType,
      MessageSendStatus status
  );
}
