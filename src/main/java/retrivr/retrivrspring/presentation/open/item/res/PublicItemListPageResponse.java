package retrivr.retrivrspring.presentation.open.item.res;

import java.util.List;

public record PublicItemListPageResponse(
        Long organizationId,
        String organizationName,
        String profileImageUrl,
        List<PublicItemSummary> items,
        Long nextCursor
) {

}
