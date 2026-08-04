package retrivr.retrivrspring.presentation.admin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import retrivr.retrivrspring.application.service.admin.auth.EmailVerificationService;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminEmailVerificationSendRequest;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminEmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.EmailVerificationSendResponse;

@ExtendWith(MockitoExtension.class)
class EmailVerificationControllerTest {

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private AdminRefreshTokenCookieManager refreshTokenCookieManager;

    @InjectMocks
    private EmailVerificationController emailVerificationController;

    @Test
    void sendAdminEmailVerificationCode_returnsOnlyTimingPolicy() {
        AuthUser authUser = new AuthUser(1L, "old@example.com");
        AdminEmailVerificationSendRequest request =
                new AdminEmailVerificationSendRequest("new@example.com", "pvt_email");
        EmailVerificationSendResponse serviceResponse = new EmailVerificationSendResponse(
                "new@example.com",
                "EMAIL_CHANGE",
                600,
                60
        );
        given(emailVerificationService.sendChangeEmailCode(request, 1L))
                .willReturn(serviceResponse);

        var response =
                emailVerificationController.sendAdminEmailVerificationCode(authUser, request);

        assertEquals(600, response.getBody().expiresInSeconds());
        assertEquals(60, response.getBody().resendAvailableInSeconds());
    }

    @Test
    void verifyAdminEmail_expiresRefreshTokenCookie() {
        AuthUser authUser = new AuthUser(1L, "old@example.com");
        AdminEmailVerificationRequest request = new AdminEmailVerificationRequest(
                "new@example.com",
                "123456",
                "pvt_email"
        );
        ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
                .path("/")
                .maxAge(0)
                .build();

        given(refreshTokenCookieManager.delete()).willReturn(expiredCookie);

        var response = emailVerificationController.verifyAdminEmail(authUser, request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        assertEquals(expiredCookie.toString(), response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
        verify(emailVerificationService).verifyChangeEmail(request, 1L);
    }
}
