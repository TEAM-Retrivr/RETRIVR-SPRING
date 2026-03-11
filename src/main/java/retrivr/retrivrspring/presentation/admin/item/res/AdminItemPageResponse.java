package retrivr.retrivrspring.presentation.admin.item.res;

import java.util.List;

public record AdminItemPageResponse(
    List<AdminItemListResponse> items,
    Long nextCursor
) {
}
