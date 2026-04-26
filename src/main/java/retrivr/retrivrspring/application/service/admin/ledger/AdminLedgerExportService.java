package retrivr.retrivrspring.application.service.admin.ledger;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.port.excel.LedgerExportRequest;
import retrivr.retrivrspring.application.port.excel.LedgerExportRequest.InventoryLedgerRow;
import retrivr.retrivrspring.application.port.excel.LedgerExportRequest.RentalLedgerRow;
import retrivr.retrivrspring.application.port.excel.LedgerExportRequest.StatisticsLedgerRow;
import retrivr.retrivrspring.application.port.excel.LedgerExporter;
import retrivr.retrivrspring.application.vo.LedgerByteArrayAndFileName;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.repository.item.ItemRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.domain.repository.rental.RentalRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLedgerExportService {

  private final LedgerExporter ledgerExporter;
  private final OrganizationRepository organizationRepository;
  private final RentalRepository rentalRepository;
  private final ItemRepository itemRepository;

  public LedgerByteArrayAndFileName exportLedger(Long organizationId) {
    // 로그인 검증
    Organization organization = organizationRepository.findById(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    // 로그인한 조직에 대한 Rental, Item 정보 DB 에서 가져옴
    List<Rental> rentals = rentalRepository.findFetchBorrowerAllByOrganization(organization);
    rentalRepository.findFetchRentalItemUnitsByRentalIn(rentals);
    List<Item> items = itemRepository.findFetchItemUnitsByOrganization(organization);

    // item 당 대여 횟수 Map 계산 (itemId, count)
    Map<Long, Long> itemRentalCount = createItemRentalCountMap(rentals);

    // 대여 이력 시트 데이터 계산
    List<RentalLedgerRow> rentalLedgerRows = rentals.stream()
        .map(RentalLedgerRow::of)
        .toList();

    // 물품 현황 시트 데이터 계산
    List<InventoryLedgerRow> inventoryLedgerRows = items.stream()
        .map(item -> InventoryLedgerRow.of(item, itemRentalCount.getOrDefault(item.getId(), 0L)))
        .toList();

    // 요약 통계 시트 데이터 계산
    StatisticsLedgerRow statisticsLedgerRow = createStatisticsLedgerRow(rentals, items, itemRentalCount);

    // Excel 파일 추출
    byte[] excelBytes = ledgerExporter.export(
        new LedgerExportRequest(rentalLedgerRows, inventoryLedgerRows, statisticsLedgerRow)
    );

    return new LedgerByteArrayAndFileName(new ByteArrayResource(excelBytes), organization.getName() + "_장부_"+ LocalDate.now() + ".xlsx");
  }

  private Map<Long, Long> createItemRentalCountMap(List<Rental> rentals) {
    Map<Long, Long> itemRentalCount = new HashMap<>();

    for (Rental rental : rentals) {
      if (!rental.isCountable()) {
        continue;
      }
      rental.getRentalItems().forEach(rentalItem -> {
        Long itemId = rentalItem.getItem().getId();
        itemRentalCount.merge(itemId, 1L, Long::sum);
      });
    }

    return itemRentalCount;
  }

  private StatisticsLedgerRow createStatisticsLedgerRow(
      List<Rental> rentals,
      List<Item> items,
      Map<Long, Long> itemRentalCount
  ) {
    long totalRentalCount = rentals.stream()
        .filter(Rental::isCountable)
        .count();

    long currentRentalCount = rentals.stream()
        .filter(Rental::isRented)
        .count();

    long totalOverdueCount = rentals.stream()
        .filter(Rental::isOverdue)
        .count();

    long totalRentalPeriod = rentals.stream()
        .mapToLong(Rental::getRentalPeriod)
        .sum();

    float averageRentalPeriod = totalRentalCount > 0
        ? roundToSecondDecimal((float) totalRentalPeriod / totalRentalCount)
        : 0f;

    float overdueRate = totalRentalCount > 0
        ? roundToSecondDecimal(((float) totalOverdueCount / totalRentalCount) * 100)
        : 0f;

    String mostRentedItemName = findMostRentedItemName(items, itemRentalCount);

    return StatisticsLedgerRow.of(
        totalRentalCount,
        currentRentalCount,
        totalOverdueCount,
        overdueRate,
        averageRentalPeriod,
        mostRentedItemName
    );
  }

  private String findMostRentedItemName(List<Item> items, Map<Long, Long> itemRentalCount) {
    Entry<Long, Long> maxEntry = itemRentalCount.entrySet().stream()
        .max(Entry.comparingByValue())
        .orElse(null);

    if (maxEntry == null) {
      return null;
    }

    Long mostRentedItemId = maxEntry.getKey();

    return items.stream()
        .filter(item -> item.getId().equals(mostRentedItemId))
        .map(Item::getName)
        .findFirst()
        .orElse("");
  }

  private float roundToSecondDecimal(float value) {
    return Math.round(value * 100) / 100f;
  }
}
