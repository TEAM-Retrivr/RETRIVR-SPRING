package retrivr.retrivrspring.presentation.open.item.res;

import java.util.List;

public record PublicItemListPageResponse(
        Long organizationId,
        String organizationName,
        List<PublicItemSummary> items,
        Long nextCursor
) {

}
