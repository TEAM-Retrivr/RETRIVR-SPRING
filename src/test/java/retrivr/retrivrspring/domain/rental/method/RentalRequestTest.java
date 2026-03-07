package retrivr.retrivrspring.domain.rental.method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import retrivr.retrivrspring.domain.entity.item.Item;
import retrivr.retrivrspring.domain.entity.item.ItemUnit;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.entity.rental.Borrower;
import retrivr.retrivrspring.domain.entity.rental.Rental;
import retrivr.retrivrspring.domain.entity.rental.enumerate.RentalStatus;

@ExtendWith(MockitoExtension.class)
class RentalRequestTest {

  @Test
  @DisplayName("ItemUnit 없이 대여요청 생성: 상태=REQUESTED, requestedAt 세팅, item.onRentalRequested 호출, RentalItem 1개 추가")
  void request_withoutItemUnit() {
    // given
    Item item = mock(Item.class);
    Organization org = mock(Organization.class);
    Borrower borrower = mock(Borrower.class);

    when(item.getOrganization()).thenReturn(org);

    // when
    Rental rental = Rental.request(item, null, borrower);

    // then
    assertThat(rental.getStatus()).isEqualTo(RentalStatus.REQUESTED);
    assertThat(rental.getOrganization()).isSameAs(org);
    assertThat(rental.getRequestedAt()).isNotNull();
    assertThat(rental.getOrganization()).isSameAs(org);
    assertThat(rental.getBorrower()).isSameAs(borrower);

    // item side-effect
    verify(item, times(1)).onRentalRequested(null);

    // 관계 엔티티 생성 여부(“장바구니/다건” 확장 전: 1개 들어간다는 가정)
    assertThat(rental.getRentalItems()).hasSize(1);
    assertThat(rental.hasItemUnit()).isFalse();
    assertThat(rental.getRentalItemUnits()).isEmpty();
  }


  @Test
  @DisplayName("ItemUnit 포함 대여요청 생성: RentalItemUnit 1개 추가 + item.onRentalRequested(itemUnit) 호출")
  void request_withItemUnit() {
    // given
    Item item = mock(Item.class);
    ItemUnit unit = mock(ItemUnit.class);
    Organization org = mock(Organization.class);
    Borrower borrower = mock(Borrower.class);

    when(item.getOrganization()).thenReturn(org);

    // when
    Rental rental = Rental.request(item, unit, borrower);

    // then
    verify(item, times(1)).onRentalRequested(unit);

    assertThat(rental.getRentalItems()).hasSize(1);
    assertThat(rental.getRentalItemUnits()).hasSize(1);
    assertThat(rental.hasItemUnit()).isTrue();
    assertThat(rental.getItemUnit()).isNotNull();
  }
}
