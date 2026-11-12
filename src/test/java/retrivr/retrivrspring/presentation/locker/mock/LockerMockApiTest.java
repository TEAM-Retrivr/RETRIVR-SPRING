package retrivr.retrivrspring.presentation.locker.mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import retrivr.retrivrspring.domain.entity.organization.Organization;
import retrivr.retrivrspring.domain.repository.organization.OrganizationRepository;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.config.JwtTokenProvider;
import retrivr.retrivrspring.global.config.SecurityConfig;
import retrivr.retrivrspring.global.web.CorsPolicyProperties;
import retrivr.retrivrspring.presentation.admin.locker.mock.AdminLockerAvailabilityMockController;
import retrivr.retrivrspring.presentation.admin.locker.mock.AdminLockerExtensionMockController;
import retrivr.retrivrspring.presentation.admin.locker.mock.AdminLockerRentalMockController;
import retrivr.retrivrspring.presentation.open.locker.PublicLockerMockController;

@WebMvcTest({PublicLockerMockController.class, AdminLockerRentalMockController.class,
    AdminLockerExtensionMockController.class, AdminLockerAvailabilityMockController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class LockerMockApiTest {
  @Autowired MockMvc mvc;
  @MockitoBean JpaMetamodelMappingContext jpaMetamodelMappingContext;
  @MockitoBean JwtTokenProvider jwtTokenProvider;
  @MockitoBean OrganizationRepository organizationRepository;
  @MockitoBean CorsPolicyProperties corsPolicyProperties;

  private static final String RENTALS = "/api/admin/v1/locker-rentals";
  private static final String EXTENSIONS = "/api/admin/v1/locker-rental-extensions";
  private static final String ID = "30000000-0000-0000-0000-000000000001";
  private static final String BORROWER = """
      {"name":"홍길동","phone":"01000000000","additionalInfo":{"department":"컴퓨터공학과","studentId":"20260000"},
       "phoneVerificationToken":"mock-phone-token","requestNote":"연장 요청"}
      """;

  @BeforeEach
  void authentication() {
    given(corsPolicyProperties.getAllowedOrigins()).willReturn(List.of("http://localhost"));
    given(corsPolicyProperties.getAllowedMethods()).willReturn(List.of("GET", "POST", "PATCH", "OPTIONS"));
    given(jwtTokenProvider.resolveToken(any())).willAnswer(invocation ->
        "Bearer valid".equals(invocation.getArgument(0)) ? "valid" : null);
    given(jwtTokenProvider.validateToken("valid")).willReturn(true);
    given(jwtTokenProvider.getAuthUser("valid")).willReturn(new AuthUser(1L, "admin@example.com"));
    given(organizationRepository.findById(1L)).willReturn(java.util.Optional.of(mock(Organization.class)));
  }

  @Test
  void publicLayoutNeedsNoJwtAndContainsNoBorrower() throws Exception {
    mvc.perform(get("/api/public/v1/locker-zones/{id}/lockers", ID))
        .andExpect(status().isOk()).andExpect(jsonPath("$.lockers.length()").value(6))
        .andExpect(jsonPath("$.lockers[0].borrower").doesNotExist())
        .andExpect(jsonPath("$.lockers[0].status").value("PENDING"));
  }

  @ParameterizedTest
  @ValueSource(strings = {RENTALS, RENTALS + "/overview", RENTALS + "/" + ID, EXTENSIONS, EXTENSIONS + "/" + ID})
  void privateReadsRequireJwt(String path) throws Exception {
    mvc.perform(get(path)).andExpect(status().isUnauthorized());
    mvc.perform(get(path).header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
    mvc.perform(get(path).header("Authorization", "Bearer valid")).andExpect(status().isOk());
  }

  @ParameterizedTest
  @ValueSource(strings = {RENTALS + "/" + ID + "/approve", RENTALS + "/" + ID + "/reject",
      RENTALS + "/" + ID + "/return", RENTALS + "/approve", RENTALS + "/return",
      EXTENSIONS + "/" + ID + "/approve", EXTENSIONS + "/" + ID + "/reject"})
  void privateMutationsRequireJwt(String path) throws Exception {
    mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void availabilityRequiresJwt() throws Exception {
    mvc.perform(patch("/api/admin/v1/lockers/availability").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void publicCreateValidatesAndDoesNotPersist() throws Exception {
    String url = "/api/public/v1/lockers/" + ID + "/rentals";
    mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
            .content("{\"borrower\":" + BORROWER + ",\"termsAgreed\":false,\"termsVersion\":1}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
            .content("{\"borrower\":" + BORROWER + ",\"termsAgreed\":true,\"termsVersion\":1}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.publicId").value(LockerMockData.PUBLIC_ID))
        .andExpect(jsonPath("$.status").value("REQUESTED"));
    mvc.perform(post(RENTALS + "/" + ID + "/approve").header("Authorization", "Bearer valid")
            .contentType(MediaType.APPLICATION_JSON).content("{\"managerName\":\"김관리\",\"collateralConfirmed\":true}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
    mvc.perform(get("/api/public/v1/locker-rentals/" + LockerMockData.PUBLIC_ID))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REQUESTED"));
  }

  @Test
  void privateOverviewIncludesCountsAndBorrower() throws Exception {
    mvc.perform(get(RENTALS + "/overview").header("Authorization", "Bearer valid"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.summary.totalCount").value(6))
        .andExpect(jsonPath("$.lockers[0].borrower.additionalInfo.department").value("컴퓨터공학과"));
  }

  @Test
  void extensionFlowProvidesDatesAndApprovalWithoutChangingFixture() throws Exception {
    mvc.perform(get("/api/public/v1/lockers/" + ID + "/extension-form"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.previousDueDate").value("2026-11-15"))
        .andExpect(jsonPath("$.newDueDate").value("2026-11-22"));
    mvc.perform(post("/api/public/v1/lockers/" + ID + "/extensions").contentType(MediaType.APPLICATION_JSON)
            .content("{\"borrower\":" + BORROWER + "}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REQUESTED"))
        .andExpect(jsonPath("$.borrower").doesNotExist());
    mvc.perform(post(EXTENSIONS + "/" + ID + "/approve").header("Authorization", "Bearer valid")
            .contentType(MediaType.APPLICATION_JSON).content("{\"managerName\":\"김관리\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
    mvc.perform(get(EXTENSIONS + "/" + ID).header("Authorization", "Bearer valid"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REQUESTED"));
  }
}
