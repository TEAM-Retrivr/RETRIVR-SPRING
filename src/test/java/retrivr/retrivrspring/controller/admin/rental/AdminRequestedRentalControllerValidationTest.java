package retrivr.retrivrspring.controller.admin.rental;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import retrivr.retrivrspring.application.service.admin.rental.AdminRequestedRentalService;
import retrivr.retrivrspring.presentation.admin.rental.AdminRequestedRentalController;

@WebMvcTest(AdminRequestedRentalController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class AdminRequestedRentalControllerValidationTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  JpaMetamodelMappingContext jpaMetamodelMappingContext;

  @MockitoBean
  private AdminRequestedRentalService adminRentalService;

  @Nested
  @DisplayName("대여 승인 요청 검증")
  class ApproveValidationTest {

    @Test
    @DisplayName("관리자 이름이 null이면 400")
    void approve_nullAdminName_returns400() throws Exception {
      String requestBody = """
          {
            "adminNameToApprove": null
          }
          """;

      mockMvc.perform(post("/api/admin/v1/rentals/{rentalId}/approve", 1L)
              .contentType(APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());

      then(adminRentalService).should(never()).approveRentalRequest(any(), any(), any());
    }

    @Test
    @DisplayName("관리자 이름이 공백이면 400")
    void approve_blankAdminName_returns400() throws Exception {
      String requestBody = """
          {
            "adminNameToApprove": "   "
          }
          """;

      mockMvc.perform(post("/api/admin/v1/rentals/{rentalId}/approve", 1L)
              .contentType(APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());

      then(adminRentalService).should(never()).approveRentalRequest(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("대여 거절 요청 검증")
  class RejectValidationTest {

    @Test
    @DisplayName("관리자 이름이 null이면 400")
    void reject_nullAdminName_returns400() throws Exception {
      String requestBody = """
          {
            "adminNameToReject": null
          }
          """;

      mockMvc.perform(post("/api/admin/v1/rentals/{rentalId}/reject", 1L)
              .contentType(APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());

      then(adminRentalService).should(never()).rejectRentalRequest(any(), any(), any());
    }

    @Test
    @DisplayName("관리자 이름이 공백이면 400")
    void reject_blankAdminName_returns400() throws Exception {
      String requestBody = """
          {
            "adminNameToReject": "   "
          }
          """;

      mockMvc.perform(post("/api/admin/v1/rentals/{rentalId}/reject", 1L)
              .contentType(APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());

      then(adminRentalService).should(never()).rejectRentalRequest(any(), any(), any());
    }
  }

}
