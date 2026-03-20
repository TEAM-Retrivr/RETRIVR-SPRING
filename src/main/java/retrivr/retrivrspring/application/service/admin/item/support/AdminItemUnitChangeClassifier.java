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

        Map<String, ItemUnit> currentUnitMap = new LinkedHashMap<>();
        for (ItemUnit currentItemUnit : currentItemUnits) {
            currentUnitMap.put(currentItemUnit.getCode(), currentItemUnit);
        }

        Set<String> seenCodes = new LinkedHashSet<>();
        List<String> deleteUnitCodes = new ArrayList<>();
        List<String> createLabels = new ArrayList<>();
        List<UnitRenameCommand> renameCommands = new ArrayList<>();

        for (AdminItemUnitChangeRequest unitChange : unitChanges) {
            String code = normalize(unitChange.code());
            String label = normalize(unitChange.label());

            if (code == null && label == null) {
                throw new ApplicationException(ErrorCode.BAD_REQUEST_EXCEPTION,
                        "Item unit change must contain code or label.");
            }

            if (code == null) {
                createLabels.add(requireLabel(label));
                continue;
            }

            if (!seenCodes.add(code)) {
                throw new ApplicationException(ErrorCode.BAD_REQUEST_EXCEPTION,
                        "Duplicated item unit code in update request.");
            }

            ItemUnit targetItemUnit = currentUnitMap.get(code);
            if (targetItemUnit == null) {
                throw new ApplicationException(ErrorCode.BAD_REQUEST_EXCEPTION,
                        "Item unit code does not exist.");
            }

            if (label == null) {
                deleteUnitCodes.add(code);
                continue;
            }

            renameCommands.add(new UnitRenameCommand(targetItemUnit, label));
        }

        return new AdminItemUnitChangeSet(deleteUnitCodes, createLabels, renameCommands);
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
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record UnitRenameCommand(ItemUnit itemUnit, String label) {
    }

    public record AdminItemUnitChangeSet(
            List<String> deleteUnitCodes,
            List<String> createLabels,
            List<UnitRenameCommand> renameCommands
    ) {
        public static AdminItemUnitChangeSet empty() {
            return new AdminItemUnitChangeSet(List.of(), List.of(), List.of());
        }
    }
}
