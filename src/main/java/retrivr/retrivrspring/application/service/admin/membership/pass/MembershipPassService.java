package retrivr.retrivrspring.application.service.admin.membership.pass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.vo.DefaultNormalizedCursorPageSearchSize;
import retrivr.retrivrspring.domain.entity.membership.CouponRegistration;
import retrivr.retrivrspring.domain.entity.membership.MembershipPass;
import retrivr.retrivrspring.domain.entity.membership.Payment;
import retrivr.retrivrspring.domain.entity.membership.Subscription;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipLevel;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassStatus;
import retrivr.retrivrspring.domain.entity.membership.enumerate.MembershipPassType;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.membership.pass.MembershipPassRepository;
import retrivr.retrivrspring.domain.repository.membership.subscription.SubscriptionRepository;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.admin.membership.pass.res.CouponMembershipPassListResponse;
import retrivr.retrivrspring.presentation.admin.membership.pass.res.CurrentSubscriptionMembershipPassResponse;
import retrivr.retrivrspring.presentation.admin.membership.pass.res.MembershipPassHistoryResponse;
import retrivr.retrivrspring.presentation.admin.membership.pass.res.MembershipStatusSummaryResponse;

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

  //generateSubscriptionMembershipPassWithPayment 로 통합됨
  @Deprecated
  @Transactional
  public MembershipPass generateSubscriptionMembershipPass(Long loginOrganizationId,
      Subscription subscription, Payment payment) {
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
        sequence,
        payment
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
  public MembershipPass generateSubscriptionMembershipPassWithPayment(Long loginOrganizationId,
      Payment payment, Subscription subscription) {
    LocalDateTime now = LocalDateTime.now();

    Organization organization = organizationRepository.findById(loginOrganizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    subscription.validateOwner(organization);
    payment.validateOwner(organization);

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
        payment.getPlan().getDuration(),
        sequence,
        payment
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

  public CurrentSubscriptionMembershipPassResponse getCurrentSubscriptionMembershipPass(Long organizationId) {
    Organization organization = organizationRepository.findById(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    MembershipPass membershipPass = membershipPassRepository.findFirstByOrganizationAndStatusOrderBySequenceAsc(
        organization, MembershipPassStatus.ACTIVE
    ).orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ACTIVE_PASS));

    return CurrentSubscriptionMembershipPassResponse.of(
        membershipPass.getSubscriptionOrThrow(),
        membershipPass
    );
  }

  public CouponMembershipPassListResponse getCouponMembershipPasses(Long organizationId) {
    Organization organization = organizationRepository.findById(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    List<MembershipPass> membershipPassList = membershipPassRepository.findAllByOrganizationAndSourceTypeAndStatusIsNotOrderBySequenceAsc(
        organization,
        MembershipPassType.COUPON,
        MembershipPassStatus.EXPIRED
    );

    return CouponMembershipPassListResponse.from(
        membershipPassList
    );
  }

  public MembershipPassHistoryResponse getMembershipPassHistory(Long organizationId, Long cursor, Integer limit, LocalDate start, LocalDate end) {
    Organization organization = organizationRepository.findById(organizationId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_ORGANIZATION));

    DefaultNormalizedCursorPageSearchSize normalizedSize = DefaultNormalizedCursorPageSearchSize.of(
        limit);

    long sequenceCursor = cursor == null ? Long.MAX_VALUE : cursor;

    List<MembershipPass> passes =
        membershipPassRepository
            .findAllByOrganizationAndSequenceLessThanAndCreatedAtBetweenOrderBySequenceDesc(
                organization,
                sequenceCursor,
                start.atStartOfDay(),
                end.atStartOfDay().plusDays(1),
                PageRequest.of(0, normalizedSize.sizePlusOne())
            );

    boolean hasNext = passes.size() > normalizedSize.size();

    List<MembershipPass> page = hasNext
        ? passes.subList(0, normalizedSize.size())
        : passes;

    Long nextCursor = hasNext && !page.isEmpty()
        ? page.getLast().getSequence()
        : null;

    return MembershipPassHistoryResponse.from(
        page,
        nextCursor
    );
  }
}
