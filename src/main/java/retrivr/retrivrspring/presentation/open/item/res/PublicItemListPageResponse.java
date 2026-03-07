package retrivr.retrivrspring.presentation.open.item.res;

import java.util.List;

public record PublicItemListPageResponse(
    List<PublicItemSummary> items,
    Long nextCursor
) {

}
