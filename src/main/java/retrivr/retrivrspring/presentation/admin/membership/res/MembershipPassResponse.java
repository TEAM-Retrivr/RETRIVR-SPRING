package retrivr.retrivrspring.presentation.admin.membership.res;

import java.time.LocalDateTime;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassType;

public record MembershipPassResponse(
    String membershipPassId,
    MembershipPassType type,
    MembershipPassStatus status,
    LocalDateTime startAt,
    LocalDateTime expireAt
) {

}