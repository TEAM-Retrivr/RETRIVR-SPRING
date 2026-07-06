package retrivr.retrivrspring.controller.admin.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import retrivr.retrivrspring.application.service.admin.auth.AdminAuthService;
import retrivr.retrivrspring.application.service.admin.auth.AdminLogoutResult;
import retrivr.retrivrspring.application.service.admin.auth.AdminRefreshResult;
import retrivr.retrivrspring.global.error.ApplicationException;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.error.GlobalExceptionHandler;
import retrivr.retrivrspring.presentation.admin.auth.AdminAuthController;
import retrivr.retrivrspring.presentation.admin.auth.AdminAuthOriginValidator;
import retrivr.retrivrspring.presentation.admin.auth.AdminRefreshTokenCookieManager;

import static org.mockito.Mockito.mock;

class AdminAuthControllerOriginValidationTest {

    private AdminAuthService adminAuthService;
    private AdminRefreshTokenCookieManager refreshTokenCookieManager;
    private AdminAuthOriginValidator adminAuthOriginValidator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminAuthService = mock(AdminAuthService.class);
        refreshTokenCookieManager = mock(AdminRefreshTokenCookieManager.class);
        adminAuthOriginValidator = mock(AdminAuthOriginValidator.class);

        AdminAuthController controller = new AdminAuthController(
                adminAuthService,
                refreshTokenCookieManager,
                adminAuthOriginValidator
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void refresh_rejectsDisallowedOrigin() throws Exception {
        doThrow(new ApplicationException(ErrorCode.FORBIDDEN_EXCEPTION, "Origin is not allowed."))
                .when(adminAuthOriginValidator)
                .validate(any());

        mockMvc.perform(post("/api/admin/v1/auth/refresh")
                        .header(HttpHeaders.ORIGIN, "https://malicious.example")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN_EXCEPTION.getCode()));

        verify(adminAuthService, never()).refresh(any());
    }

    @Test
    void refresh_allowsConfiguredOrigin() throws Exception {
        given(refreshTokenCookieManager.extract(any())).willReturn("refresh-token");
        given(adminAuthService.refresh("refresh-token"))
                .willReturn(new AdminRefreshResult(1L, "admin@retrivr.com", "new-access-token", "new-refresh-token"));
        given(refreshTokenCookieManager.create("new-refresh-token"))
                .willReturn(ResponseCookie.from("refreshToken", "new-refresh-token").path("/").build());

        mockMvc.perform(post("/api/admin/v1/auth/refresh")
                        .header(HttpHeaders.ORIGIN, "https://retrivr-web.vercel.app")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(1L))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refreshToken=new-refresh-token")));
    }

    @Test
    void logout_rejectsDisallowedOrigin() throws Exception {
        doThrow(new ApplicationException(ErrorCode.FORBIDDEN_EXCEPTION, "Origin is not allowed."))
                .when(adminAuthOriginValidator)
                .validate(any());

        mockMvc.perform(post("/api/admin/v1/auth/logout")
                        .header(HttpHeaders.ORIGIN, "https://malicious.example")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN_EXCEPTION.getCode()));

        verify(adminAuthService, never()).logout(any());
    }

    @Test
    void logout_allowsConfiguredOrigin() throws Exception {
        given(refreshTokenCookieManager.extract(any())).willReturn("refresh-token");
        given(refreshTokenCookieManager.delete())
                .willReturn(ResponseCookie.from("refreshToken", "").path("/").build());
        given(adminAuthService.logout("refresh-token")).willReturn(new AdminLogoutResult(true));

        mockMvc.perform(post("/api/admin/v1/auth/logout")
                        .header(HttpHeaders.ORIGIN, "https://www.retrivr.kr")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
