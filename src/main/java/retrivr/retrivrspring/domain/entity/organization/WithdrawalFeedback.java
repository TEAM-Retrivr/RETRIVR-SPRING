package retrivr.retrivrspring.domain.entity.organization;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import retrivr.retrivrspring.domain.entity.BaseTimeEntity;

@Getter
@Entity
@Table(name = "withdrawal_feedback")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawalFeedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawal_feedback_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "other_reason", length = 200)
    private String otherReason;

    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<WithdrawalFeedbackReason> reasons = new ArrayList<>();

    @Builder
    public WithdrawalFeedback(Organization organization, String otherReason) {
        this.organization = organization;
        this.otherReason = otherReason;
    }

    public void addReason(WithdrawalFeedbackReason reason) {
        reasons.add(reason);
    }
}
