package retrivr.retrivrspring.application.service.open;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrivr.retrivrspring.application.port.message.MessageSender;
import retrivr.retrivrspring.application.port.message.NotificationRecipient;
import retrivr.retrivrspring.application.port.message.NotificationRequest;
import retrivr.retrivrspring.domain.entity.organization.PhoneVerification;
import retrivr.retrivrspring.domain.entity.organization.PhoneVerificationToken;
import retrivr.retrivrspring.domain.entity.organization.enumerate.PhoneVerificationPurpose;
import retrivr.retrivrspring.domain.entity.rental.PhoneNumber;
import retrivr.retrivrspring.domain.message.MessageType;
import retrivr.retrivrspring.domain.message.PhoneVerificationCodeContent;
import retrivr.retrivrspring.domain.repository.auth.PhoneVerificationRepository;
import retrivr.retrivrspring.domain.repository.auth.PhoneVerificationTokenRepository;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.presentation.open.auth.req.PhoneVerificationSendRequest;
import retrivr.retrivrspring.presentation.open.auth.req.PhoneVerificationVerifyRequest;
import retrivr.retrivrspring.presentation.open.auth.res.PhoneVerificationSendResponse;
import retrivr.retrivrspring.presentation.open.auth.res.PhoneVerificationVerifyResponse;

@Service
@Transactional(readOnly = true)
public class PublicPhoneVerificationService {

  private final PhoneVerificationRepository phoneVerificationRepository;
  private final PhoneVerificationTokenRepository phoneVerificationTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final MessageSender messageSender;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  public PublicPhoneVerificationService(
      PhoneVerificationRepository phoneVerificationRepository,
      PhoneVerificationTokenRepository phoneVerificationTokenRepository,
      PasswordEncoder passwordEncoder,
      @Qualifier("alimTalkMessageSender") MessageSender messageSender
  ) {
    this.phoneVerificationRepository = phoneVerificationRepository;
    this.phoneVerificationTokenRepository = phoneVerificationTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.messageSender = messageSender;
  }

  @Transactional
  public PhoneVerificationSendResponse sendVerificationCode(PhoneVerificationSendRequest request) {
    PhoneNumber normalizedPhone = new PhoneNumber(request.phoneNumber());
    String rawCode = generateCode();
    String encodedCode = passwordEncoder.encode(rawCode);
    LocalDateTime now = LocalDateTime.now();

    PhoneVerification verification = phoneVerificationRepository
        .findByPhoneAndPurpose(normalizedPhone, request.purpose())
        .map(existing -> {
          existing.refresh(encodedCode, now);
          return existing;
        })
        .orElseGet(() -> PhoneVerification.create(
            normalizedPhone,
            request.purpose(),
            encodedCode,
            now
        ));

    verification.clearToken();
    phoneVerificationRepository.save(verification);

    messageSender.send(new NotificationRequest(
        MessageType.PHONE_VERIFICATION,
        new NotificationRecipient(null, normalizedPhone.getPhone()),
        new PhoneVerificationCodeContent(rawCode, PhoneVerification.EXPIRATION_TIME_MINUTES)
    ));

    return new PhoneVerificationSendResponse(verification.getId());
  }

  @Transactional
  public PhoneVerificationVerifyResponse verifyCode(PhoneVerificationVerifyRequest request) {
    LocalDateTime now = LocalDateTime.now();

    PhoneVerification phoneVerification = phoneVerificationRepository.findById(
            request.verificationId())
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_PHONE_VERIFICATION));

    if (phoneVerification.isVerificationLocked(now)) {
      throw new ApplicationException(ErrorCode.TOO_MANY_PHONE_VERIFICATION_ATTEMPTS);
    }

    if (!phoneVerification.isMatchesPurpose(request.purpose())) {
      throw new ApplicationException(ErrorCode.PHONE_VERIFICATION_PURPOSE_MISMATCH);
    }

    if (phoneVerification.isExpired(now)) {
      throw new ApplicationException(ErrorCode.EXPIRED_PHONE_VERIFICATION);
    }

    if (!passwordEncoder.matches(request.rawCode(), phoneVerification.getCodeHash())) {
      int failedAttempts = phoneVerification.incrementFailedAttempts();
      if (failedAttempts >= PhoneVerification.MAX_FAILED_VERIFICATION_ATTEMPTS) {
        phoneVerification.lockVerification(now);
      }
      phoneVerificationRepository.save(phoneVerification);

      if (phoneVerification.isVerificationLocked(now)) {
        throw new ApplicationException(ErrorCode.TOO_MANY_PHONE_VERIFICATION_ATTEMPTS);
      }
      throw new ApplicationException(ErrorCode.PHONE_VERIFICATION_CODE_MISMATCH);
    }

    phoneVerification.markVerified(now);
    return issueVerificationToken(phoneVerification, now);
  }

  private PhoneVerificationVerifyResponse issueVerificationToken(PhoneVerification verification,
      LocalDateTime now) {
    String rawToken = "pvt_" + UUID.randomUUID();
    String encodedToken = passwordEncoder.encode(rawToken);

    PhoneVerificationToken token = phoneVerificationTokenRepository
        .findByPhoneVerification(verification)
        .map(existing -> {
          existing.refresh(encodedToken, now);
          return existing;
        })
        .orElseGet(() -> PhoneVerificationToken.create(
            verification,
            encodedToken,
            now
        ));

    phoneVerificationTokenRepository.save(token);
    return new PhoneVerificationVerifyResponse(
        rawToken,
        token.getId()
    );
  }

  @Transactional
  public void validateAndConsumePhoneVerificationToken(
      String tokenId,
      String rawToken,
      PhoneVerificationPurpose purpose
  ) {
    LocalDateTime now = LocalDateTime.now();

    PhoneVerificationToken token = phoneVerificationTokenRepository.findById(tokenId)
        .orElseThrow(() -> new ApplicationException(ErrorCode.NOT_FOUND_PHONE_VERIFICATION_TOKEN));

    if (!passwordEncoder.matches(rawToken, token.getTokenHash())) {
      throw new ApplicationException(ErrorCode.PHONE_VERIFICATION_TOKEN_MISMATCH);
    }

    if (!token.getPhoneVerification().isMatchesPurpose(purpose)) {
      throw new ApplicationException(ErrorCode.PHONE_VERIFICATION_PURPOSE_MISMATCH);
    }

    if (token.isExpired(now)) {
      throw new ApplicationException(ErrorCode.EXPIRED_PHONE_VERIFICATION_TOKEN);
    }

    phoneVerificationRepository.delete(token.getPhoneVerification());
  }

  private String generateCode() {
    int code = SECURE_RANDOM.nextInt(900000) + 100000;
    return String.valueOf(code);
  }

}
