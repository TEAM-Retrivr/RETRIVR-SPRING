package retrivr.retrivrspring.presentation.admin.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import retrivr.retrivrspring.application.service.admin.profile.AdminProfileService;
import retrivr.retrivrspring.application.service.admin.profile.PasswordVerificationService;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.presentation.admin.auth.AdminRefreshTokenCookieManager;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminCodeUpdateRequest;
import retrivr.retrivrspring.presentation.admin.profile.req.AdminPasswordUpdateRequest;

@ExtendWith(MockitoExtension.class)
class AdminProfileControllerTest {

    @Mock
    private AdminProfileService adminProfileService;

    @Mock
    private PasswordVerificationService passwordVerificationService;

    @Mock
    private AdminRefreshTokenCookieManager refreshTokenCookieManager;

    @InjectMocks
    private AdminProfileController adminProfileController;

    private final AuthUser authUser = new AuthUser(1L, "admin@retrivr.com");

    @Test
    void updatePassword_returnsNoContentAndExpiresRefreshTokenCookie() {
        AdminPasswordUpdateRequest request = new AdminPasswordUpdateRequest(
                "NewPassword123!",
                "NewPassword123!",
                "pvt_password"
        );
        ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
                .path("/")
                .maxAge(0)
                .build();
        given(refreshTokenCookieManager.delete()).willReturn(expiredCookie);

        var response = adminProfileController.updatePassword(authUser, request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        assertEquals(
                expiredCookie.toString(),
                response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)
        );
        verify(adminProfileService).updatePassword(1L, request);
    }

    @Test
    void updateAdminCode_returnsNoContentWithoutExpiringSession() {
        AdminCodeUpdateRequest request =
                new AdminCodeUpdateRequest("123456", "123456", "pvt_admin_code");

        var response = adminProfileController.updateAdminCode(authUser, request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(adminProfileService).updateAdminCode(1L, request);
        verifyNoInteractions(refreshTokenCookieManager);
    }
}
