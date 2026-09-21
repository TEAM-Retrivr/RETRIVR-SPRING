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
        Map<String, ItemUnit> currentUnitByLabel = new LinkedHashMap<>();
        for (ItemUnit currentItemUnit : currentItemUnits) {
            currentUnitById.put(currentItemUnit.getId(), currentItemUnit);
            currentUnitByLabel.put(currentItemUnit.getLabel(), currentItemUnit);
        }

        Set<Long> seenItemUnitIds = new LinkedHashSet<>();
        List<String> deleteUnitLabels = new ArrayList<>();
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
                validateCreateLabel(currentUnitByLabel, createLabels, createLabel);
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
                deleteUnitLabels.add(targetItemUnit.getLabel());
                continue;
            }

            validateRenameLabel(currentUnitByLabel, createLabels, renameCommands, targetItemUnit, label);
            renameCommands.add(new UnitRenameCommand(targetItemUnit, label));
        }

        return new AdminItemUnitChangeSet(deleteUnitLabels, createLabels, renameCommands);
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

    private void validateCreateLabel(
            Map<String, ItemUnit> currentUnitMap,
            List<String> createLabels,
            String label
    ) {
        if (currentUnitMap.containsKey(label) || createLabels.contains(label)) {
            throw new ApplicationException(ErrorCode.DUPLICATE_ITEM_UNIT_LABEL);
        }
    }

    private void validateRenameLabel(
            Map<String, ItemUnit> currentUnitMap,
            List<String> createLabels,
            List<UnitRenameCommand> renameCommands,
            ItemUnit targetItemUnit,
            String newLabel
    ) {
        ItemUnit currentOwner = currentUnitMap.get(newLabel);
        boolean ownedBySameUnit = currentOwner != null && currentOwner == targetItemUnit;
        boolean duplicatedCreateLabel = createLabels.contains(newLabel);
        boolean duplicatedRenameLabel = renameCommands.stream()
                .anyMatch(command -> newLabel.equals(command.label()));

        if (!ownedBySameUnit && (currentOwner != null || duplicatedCreateLabel || duplicatedRenameLabel)) {
            throw new ApplicationException(ErrorCode.DUPLICATE_ITEM_UNIT_LABEL);
        }
    }

    public record UnitRenameCommand(ItemUnit itemUnit, String label) {
    }

    public record AdminItemUnitChangeSet(
            List<String> deleteUnitLabels,
            List<String> createLabels,
            List<UnitRenameCommand> renameCommands
    ) {
        public static AdminItemUnitChangeSet empty() {
            return new AdminItemUnitChangeSet(List.of(), List.of(), List.of());
        }
    }
}
