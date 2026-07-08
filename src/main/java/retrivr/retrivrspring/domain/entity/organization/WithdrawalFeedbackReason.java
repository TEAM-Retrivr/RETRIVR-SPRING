package retrivr.retrivrspring.domain.entity.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.organization.enumerate.WithdrawalReasonCode;

@Getter
@Entity
@Table(name = "withdrawal_feedback_reason")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawalFeedbackReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawal_feedback_reason_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "withdrawal_feedback_id", nullable = false)
    private WithdrawalFeedback feedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 50)
    private WithdrawalReasonCode reasonCode;

    @Builder
    public WithdrawalFeedbackReason(WithdrawalFeedback feedback, WithdrawalReasonCode reasonCode) {
        this.feedback = feedback;
        this.reasonCode = reasonCode;
    }
}
