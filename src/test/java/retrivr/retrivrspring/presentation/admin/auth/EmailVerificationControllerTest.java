package retrivr.retrivrspring.presentation.admin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import retrivr.retrivrspring.application.service.admin.auth.EmailVerificationService;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.presentation.admin.auth.req.AdminEmailVerificationRequest;
import retrivr.retrivrspring.presentation.admin.auth.res.AdminEmailChangeResponse;

@ExtendWith(MockitoExtension.class)
class EmailVerificationControllerTest {

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private AdminRefreshTokenCookieManager refreshTokenCookieManager;

    @InjectMocks
    private EmailVerificationController emailVerificationController;

    @Test
    void verifyAdminEmail_expiresRefreshTokenCookie() {
        AuthUser authUser = new AuthUser(1L, "old@example.com");
        AdminEmailVerificationRequest request = new AdminEmailVerificationRequest(
                "new@example.com",
                "123456",
                "pvt_email"
        );
        AdminEmailChangeResponse serviceResponse =
                new AdminEmailChangeResponse(1L, "new@example.com");
        ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
                .path("/")
                .maxAge(0)
                .build();

        given(emailVerificationService.verifyChangeEmail(request, 1L))
                .willReturn(serviceResponse);
        given(refreshTokenCookieManager.delete()).willReturn(expiredCookie);

        var response = emailVerificationController.verifyAdminEmail(authUser, request);

        assertEquals(serviceResponse, response.getBody());
        assertEquals(expiredCookie.toString(), response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
    }
}
