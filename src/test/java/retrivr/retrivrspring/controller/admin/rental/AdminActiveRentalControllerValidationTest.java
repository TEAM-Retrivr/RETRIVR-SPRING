package retrivr.retrivrspring.controller.admin.rental;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import retrivr.retrivrspring.application.service.admin.rental.AdminActiveRentalService;
import retrivr.retrivrspring.presentation.admin.rental.AdminActiveRentalController;

@WebMvcTest(AdminActiveRentalController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class AdminActiveRentalControllerValidationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  JpaMetamodelMappingContext jpaMetamodelMappingContext;

  @MockitoBean
  private AdminActiveRentalService adminRentalService;

  @Nested
  @DisplayName("반납 확인 요청 검증")
  class ReturnValidationTest {

    @Test
    @DisplayName("관리자 이름이 null이면 400")
    void confirmReturn_nullAdminName_returns400() throws Exception {
      String requestBody = """
          {
            "adminNameToConfirm": null
          }
          """;

      mockMvc.perform(post("/api/admin/v1/rentals/{rentalId}/return", 1L)
              .contentType(APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());

      then(adminRentalService).should(never()).confirmReturn(any(), any(), any());
    }

    @Test
    @DisplayName("관리자 이름이 공백이면 400")
    void confirmReturn_blankAdminName_returns400() throws Exception {
      String requestBody = """
          {
            "adminNameToConfirm": "   "
          }
          """;

      mockMvc.perform(post("/api/admin/v1/rentals/{rentalId}/return", 1L)
              .contentType(APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());

      then(adminRentalService).should(never()).confirmReturn(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("반납 예정일 변경 요청 검증")
  class DueDateValidationTest {

    @Test
    @DisplayName("반납 예정일이 null이면 400")
    void updateDueDate_nullDueDate_returns400() throws Exception {
      String requestBody = """
          {
            "newReturnDueDate": null
          }
          """;

      mockMvc.perform(patch("/api/admin/v1/rentals/{rentalId}/due-date", 1L)
              .contentType(APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());

      then(adminRentalService).should(never()).updateDueDate(any(), any(), any());
    }
  }
}
