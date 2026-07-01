package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneCancelScheduledPaymentResponse(
    List<String> revokedScheduleIds,
    OffsetDateTime revokedAt
) {
}
