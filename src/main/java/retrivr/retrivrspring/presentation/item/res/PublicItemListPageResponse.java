package retrivr.retrivrspring.presentation.item.res;

import java.util.List;

public record PublicItemListPageResponse(
    List<PublicItemSummary> items,
    Long nextCursor
) {

}
