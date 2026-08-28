package retrivr.retrivrspring.application.service.membership.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrivr.retrivrspring.application.service.admin.membership.pass.MembershipPassService;
import retrivr.retrivrspring.application.service.admin.membership.pay.PaymentMethodService;
import retrivr.retrivrspring.application.service.admin.membership.pay.PortOnePaymentService;
import retrivr.retrivrspring.application.service.admin.membership.subscription.SubscriptionService;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.SubscriptionPlan;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.pass.MembershipPassRepository;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentMethodRepository;
import retrivr.retrivrspring.domain.repository.membership.payment.PaymentRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.membership.subscription.res.SubscriptionPaymentPreviewResponse;

@ExtendWith(MockitoExtension.class)
class SubscriptionPaymentPreviewServiceTest {

  @Mock private OrganizationRepository organizationRepository;
  @Mock private SubscriptionRepository subscriptionRepository;
  @Mock private MembershipPassRepository membershipPassRepository;
  @Mock private PaymentMethodRepository paymentMethodRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private MembershipPassService membershipPassService;
  @Mock private PortOnePaymentService paymentService;
  @Mock private PaymentMethodService paymentMethodService;

  @InjectMocks private SubscriptionService subscriptionService;

  @Test
  void freePlan_returnsImmediatePaymentAndNextBilling() {
    Organization organization = mock(Organization.class);
    when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
    when(subscriptionRepository.findByOrganization(organization)).thenReturn(Optional.empty());
    when(membershipPassRepository.findFirstByOrganizationAndStatusOrderBySequenceDesc(
        organization, MembershipPassStatus.REGISTERED)).thenReturn(Optional.empty());
    when(membershipPassRepository.findFirstByOrganizationAndStatusOrderBySequenceAsc(
        organization, MembershipPassStatus.ACTIVE)).thenReturn(Optional.empty());

    SubscriptionPaymentPreviewResponse response =
        subscriptionService.getSubscriptionPaymentPreview(1L, SubscriptionPlan.MONTHLY);

    assertThat(response.scenario())
        .isEqualTo(SubscriptionPaymentPreviewResponse.Scenario.IMMEDIATE_PURCHASE);
    assertThat(response.plan()).isEqualTo(SubscriptionPlan.MONTHLY);
    assertThat(response.amount()).isEqualTo(4900);
    assertThat(response.immediatePaymentAmount()).isEqualTo(4900);
    assertThat(response.immediatePaymentAt()).isNotNull();
    assertThat(response.nextBillingAmount()).isEqualTo(4900);
    assertThat(response.nextBillingAt())
        .isEqualTo(response.immediatePaymentAt().plusDays(SubscriptionPlan.MONTHLY.getDuration()));
    assertThat(response.effectiveAt()).isEqualTo(response.immediatePaymentAt());
  }

  @Test
  void couponPass_returnsDeferredPaymentAtPassEnd() {
    Organization organization = mock(Organization.class);
    MembershipPass couponPass = mock(MembershipPass.class);
    LocalDateTime passEndAt = LocalDateTime.now().plusDays(14);
    when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
    when(subscriptionRepository.findByOrganization(organization)).thenReturn(Optional.empty());
    when(membershipPassRepository.findFirstByOrganizationAndStatusOrderBySequenceDesc(
        organization, MembershipPassStatus.REGISTERED)).thenReturn(Optional.of(couponPass));
    when(couponPass.isOverDue(org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(false);
    when(couponPass.getEndAt()).thenReturn(passEndAt);

    SubscriptionPaymentPreviewResponse response =
        subscriptionService.getSubscriptionPaymentPreview(1L, SubscriptionPlan.YEARLY);

    assertThat(response.scenario())
        .isEqualTo(SubscriptionPaymentPreviewResponse.Scenario.DEFERRED_START);
    assertThat(response.plan()).isEqualTo(SubscriptionPlan.YEARLY);
    assertThat(response.immediatePaymentAmount()).isNull();
    assertThat(response.immediatePaymentAt()).isNull();
    assertThat(response.nextBillingAmount()).isEqualTo(52900);
    assertThat(response.nextBillingAt()).isEqualTo(passEndAt);
    assertThat(response.effectiveAt()).isEqualTo(passEndAt);
  }

  @Test
  void activeSubscription_throwsAlreadyStartedError() {
    Organization organization = mock(Organization.class);
    Subscription subscription = mock(Subscription.class);
    when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
    when(subscriptionRepository.findByOrganization(organization)).thenReturn(Optional.of(subscription));
    when(subscription.isActive()).thenReturn(true);

    assertThatThrownBy(
        () -> subscriptionService.getSubscriptionPaymentPreview(1L, SubscriptionPlan.YEARLY)
    )
        .isInstanceOf(ApplicationException.class)
        .extracting(exception -> ((ApplicationException) exception).getErrorCode())
        .isEqualTo(ErrorCode.ALREADY_SUBSCRIPTION_STARTED);
    verify(membershipPassRepository, never())
        .findFirstByOrganizationAndStatusOrderBySequenceDesc(
            organization, MembershipPassStatus.REGISTERED);
  }
}
