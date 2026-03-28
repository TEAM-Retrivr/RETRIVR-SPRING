package retrivr.retrivrspring.controller.admin.rental;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import retrivr.retrivrspring.application.service.admin.rental.AdminActiveRentalService;
import retrivr.retrivrspring.application.service.message.SendMessageService;
import retrivr.retrivrspring.global.error.GlobalExceptionHandler;
import retrivr.retrivrspring.presentation.admin.rental.AdminActiveRentalController;

@ExtendWith(MockitoExtension.class)
class AdminActiveRentalControllerValidationTest {

  private MockMvc mockMvc;

  @Mock
  private AdminActiveRentalService adminRentalService;

  @Mock
  private SendMessageService sendMessageService;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc = MockMvcBuilders
        .standaloneSetup(new AdminActiveRentalController(adminRentalService, sendMessageService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setValidator(validator)
        .build();
  }

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
