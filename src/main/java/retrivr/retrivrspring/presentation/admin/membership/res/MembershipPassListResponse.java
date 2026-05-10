package retrivr.retrivrspring.presentation.admin.membership.res;

import java.util.List;

public record MembershipPassListResponse(
    List<MembershipPassResponse> passes
) {
}
