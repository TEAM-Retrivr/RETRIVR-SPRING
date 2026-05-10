package retrivr.retrivrspring.application.service.admin.membership.pass;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.domain.entity.membership.CouponRegistration;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipLevel;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.pass.MembershipPassRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.membership.res.MembershipStatusSummaryResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipPassService {

  private final OrganizationRepository organizationRepository;
  private final MembershipPassRepository membershipPassRepository;
  private final SubscriptionRepository subscriptionRepository;

  @Transactional
  public MembershipPass generateCouponMembershipPass(Long loginOrganizationId,
      CouponRegistration couponRegistration) {
    LocalDateTime now = LocalDateTime.now();

    Organization organization = organizationRepository.findById(loginOrganizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    couponRegistration.validateOwner(organization);

    MembershipPass lastPass = membershipPassRepository.findFirstByOrganizationOrderBySequenceDesc(
            organization)
        .orElse(null);

    LocalDateTime startAt;
    long sequence;
    if (lastPass == null) {
      startAt = now;
      sequence = 1L;
    } else {
      startAt = lastPass.getEndAt().isBefore(now)
          ? now
          : lastPass.getEndAt();

      sequence = lastPass.getSequence() + 1;
    }

    MembershipPass pass = MembershipPass.createCouponPass(
        organization,
        MembershipLevel.PREMIUM,
        couponRegistration,
        startAt,
        couponRegistration.getCoupon().getDurationDays(),
        sequence
    );

    if (pass.isActivable(now)) {
      pass.activate(now);
    }

    try {
      return membershipPassRepository.saveAndFlush(pass);
    } catch (DataIntegrityViolationException e) {
      throw new ApplicationException(ErrorCode.MEMBERSHIP_PASS_CONFLICT);
    }
  }

  @Transactional
  public MembershipPass generateSubscriptionMembershipPass(Long loginOrganizationId,
      Subscription subscription) {
    LocalDateTime now = LocalDateTime.now();

    Organization organization = organizationRepository.findById(loginOrganizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    subscription.validateOwner(organization);

    MembershipPass lastPass = membershipPassRepository.findFirstByOrganizationOrderBySequenceDesc(
            organization)
        .orElse(null);

    LocalDateTime startAt;
    long sequence;
    if (lastPass == null) {
      startAt = now;
      sequence = 1L;
    } else {
      startAt = lastPass.getEndAt().isBefore(now)
          ? now
          : lastPass.getEndAt();

      sequence = lastPass.getSequence() + 1;
    }

    MembershipPass pass = MembershipPass.createSubscriptionPass(
        organization,
        MembershipLevel.PREMIUM,
        subscription,
        startAt,
        subscription.getDurationDays(),
        sequence
    );

    if (pass.isActivable(now)) {
      pass.activate(now);
    }

    try {
      return membershipPassRepository.saveAndFlush(pass);
    } catch (DataIntegrityViolationException e) {
      throw new ApplicationException(ErrorCode.MEMBERSHIP_PASS_CONFLICT);
    }
  }

  public MembershipLevel validateMembershipPass(Long organizationId, LocalDateTime now) {
    Organization organization = organizationRepository.findById(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    MembershipPass membershipPass = membershipPassRepository.findFirstByOrganizationAndStatusOrderBySequenceDesc(
            organization, MembershipPassStatus.ACTIVE)
        .orElse(null);

    if (membershipPass == null) {
      return MembershipLevel.FREE;
    }

    if (membershipPass.isExpired(now)) {
      return MembershipLevel.FREE;
    }

    return membershipPass.getLevel();
  }

  public MembershipStatusSummaryResponse getMembershipStatusSummary(Long organizationId) {
    Organization organization = organizationRepository.findById(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    MembershipPass membershipPass = membershipPassRepository.findFirstByOrganizationAndStatusOrderBySequenceDesc(
            organization, MembershipPassStatus.ACTIVE)
        .orElse(null);

    if (membershipPass == null) {
      return MembershipStatusSummaryResponse.freePlan();
    }

    if (membershipPass.isSubscriptionPass()) {
      return MembershipStatusSummaryResponse.subscribedPlan(membershipPass);
    }

    Subscription subscription = subscriptionRepository.findByOrganization(organization)
        .orElse(null);
    if (subscription == null) {
      return MembershipStatusSummaryResponse.couponPlanWithoutSubscription(membershipPass);
    }

    return MembershipStatusSummaryResponse.couponPlanWithSubscription(membershipPass, subscription);
  }
}
