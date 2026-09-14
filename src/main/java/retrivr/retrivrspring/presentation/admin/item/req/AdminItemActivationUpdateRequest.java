package retrivr.retrivrspring.presentation.admin.item.req;

import jakarta.validation.constraints.NotNull;

public record AdminItemActivationUpdateRequest(@NotNull Boolean isActive) {
}
