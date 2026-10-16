package retrivr.retrivrspring.application.service.admin.item.support;

import org.springframework.stereotype.Component;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.item.req.AdminItemUnitChangeRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class AdminItemUnitChangeClassifier {

    public AdminItemUnitChangeSet classify(
            List<ItemUnit> currentItemUnits,
            List<AdminItemUnitChangeRequest> unitChanges
    ) {
        if (unitChanges == null || unitChanges.isEmpty()) {
            return AdminItemUnitChangeSet.empty();
        }

        Map<Long, ItemUnit> currentUnitById = new LinkedHashMap<>();
        for (ItemUnit currentItemUnit : currentItemUnits) {
            currentUnitById.put(currentItemUnit.getId(), currentItemUnit);
        }

        Set<Long> seenItemUnitIds = new LinkedHashSet<>();
        List<ItemUnit> deleteItemUnits = new ArrayList<>();
        List<String> createLabels = new ArrayList<>();
        List<UnitRenameCommand> renameCommands = new ArrayList<>();

        for (AdminItemUnitChangeRequest unitChange : unitChanges) {
            Long itemUnitId = unitChange.itemUnitId();
            String label = normalize(unitChange.label());

            if (itemUnitId == null && label == null) {
                throw new ApplicationException(ErrorCode.BAD_REQUEST_EXCEPTION,
                        "Item unit change must contain itemUnitId or label.");
            }

            if (itemUnitId == null) {
                String createLabel = requireLabel(label);
                createLabels.add(createLabel);
                continue;
            }

            if (!seenItemUnitIds.add(itemUnitId)) {
                throw new ApplicationException(ErrorCode.DUPLICATE_ITEM_UNIT_ID_IN_REQUEST);
            }

            ItemUnit targetItemUnit = currentUnitById.get(itemUnitId);
            if (targetItemUnit == null) {
                throw new ApplicationException(ErrorCode.NOT_FOUND_ITEM_UNIT);
            }

            if (label == null) {
                deleteItemUnits.add(targetItemUnit);
                continue;
            }

            renameCommands.add(new UnitRenameCommand(targetItemUnit, label));
        }

        return new AdminItemUnitChangeSet(deleteItemUnits, createLabels, renameCommands);
    }

    private String requireLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new ApplicationException(ErrorCode.BAD_REQUEST_EXCEPTION,
                    "Item unit label must not be blank.");
        }
        return label;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new ApplicationException(
                    ErrorCode.BAD_REQUEST_EXCEPTION,
                    "Item unit label must not be blank."
            );
        }
        return trimmed;
    }

    public record UnitRenameCommand(ItemUnit itemUnit, String label) {
    }

    public record AdminItemUnitChangeSet(
            List<ItemUnit> deleteItemUnits,
            List<String> createLabels,
            List<UnitRenameCommand> renameCommands
    ) {
        public static AdminItemUnitChangeSet empty() {
            return new AdminItemUnitChangeSet(List.of(), List.of(), List.of());
        }
    }
}
