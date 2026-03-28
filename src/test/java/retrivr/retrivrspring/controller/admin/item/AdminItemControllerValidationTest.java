package retrivr.retrivrspring.controller.admin.item;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import retrivr.retrivrspring.application.service.admin.item.AdminItemService;
import retrivr.retrivrspring.global.error.GlobalExceptionHandler;
import retrivr.retrivrspring.presentation.admin.item.AdminItemController;

@ExtendWith(MockitoExtension.class)
class AdminItemControllerValidationTest {

  @Mock
  private AdminItemService adminItemService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc = MockMvcBuilders.standaloneSetup(new AdminItemController(adminItemService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setValidator(validator)
        .build();
  }

  @Nested
  @DisplayName("물품 생성 요청 검증")
  class CreateValidationTest {

    @Test
    @DisplayName("name이 blank면 400")
    void create_blankName_returns400() throws Exception {
      String requestBody = """
          {
            "name": "   ",
            "description": "desc",
            "rentalDuration": 3,
            "isActive": true,
            "itemManagementType": "NON_UNIT",
            "borrowerRequirements": []
          }
          """;

      mockMvc.perform(post("/api/admin/v1/items")
              .contentType(APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());

      then(adminItemService).should(never()).createItem(anyLong(), any());
    }
  }

  @Nested
  @DisplayName("물품 수정 요청 검증")
  class UpdateValidationTest {

    @Test
    @DisplayName("full overwrite에서 rentalDuration이 null이면 400")
    void update_nullRentalDuration_returns400() throws Exception {
      String requestBody = """
          {
            "name": "충전기",
            "description": null,
            "rentalDuration": null,
            "isActive": true,
            "itemManagementType": "NON_UNIT",
            "borrowerRequirements": []
          }
          """;

      mockMvc.perform(patch("/api/admin/v1/items/{itemId}", 1L)
              .contentType(APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());

      then(adminItemService).should(never()).updateItem(anyLong(), anyLong(), any());
    }
  }

  @Test
  @DisplayName("unit availability 요청에서 isAvailable이 null이면 400")
  void updateUnitAvailability_nullIsAvailable_returns400() throws Exception {
    String requestBody = """
        {
          "isAvailable": null
        }
        """;

    mockMvc.perform(patch("/api/admin/v1/items/{itemId}/units/{itemUnitId}/availability", 1L, 2L)
            .contentType(APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest());
  }
}
