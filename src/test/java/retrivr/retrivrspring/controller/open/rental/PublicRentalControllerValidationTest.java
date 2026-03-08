package retrivr.retrivrspring.controller.open.rental;

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
import retrivr.retrivrspring.application.service.open.PublicRentalService;
import retrivr.retrivrspring.presentation.open.rental.PublicRentalController;

@WebMvcTest(PublicRentalController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class PublicRentalControllerValidationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  JpaMetamodelMappingContext jpaMetamodelMappingContext;

  @MockitoBean
  private PublicRentalService publicRentalService;

  @Nested
  @DisplayName("공개 대여 생성 요청 검증")
  class CreateRentalValidationTest {

    @Test
    @DisplayName("이름이 공백이면 400")
    void createRental_blankName_returns400() throws Exception {
      String requestBody = """
          {
            "itemUnitId": 1,
            "name": "   ",
            "phone": "010-1234-5678",
            "renterFields": {
              "학과": "컴퓨터공학부",
              "학번": "202012345"
            }
          }
          """;

      mockMvc.perform(post("/api/public/v1/items/{itemId}/rentals", 1L)
              .contentType(APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());

      then(publicRentalService).should(never()).requestRental(any(), any());
    }

    @Test
    @DisplayName("전화번호 형식이 잘못되면 400")
    void createRental_invalidPhone_returns400() throws Exception {
      String requestBody = """
          {
            "itemUnitId": 1,
            "name": "홍길동",
            "phone": "abc123!!!",
            "renterFields": {
              "학과": "컴퓨터공학부",
              "학번": "202012345"
            }
          }
          """;

      mockMvc.perform(post("/api/public/v1/items/{itemId}/rentals", 1L)
              .contentType(APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());

      then(publicRentalService).should(never()).requestRental(any(), any());
    }

    @Test
    @DisplayName("추가 대여자 정보 값이 공백이면 400")
    void createRental_blankRenterFieldValue_returns400() throws Exception {
      String requestBody = """
          {
            "itemUnitId": 1,
            "name": "홍길동",
            "phone": "010-1234-5678",
            "renterFields": {
              "학과": "   ",
              "학번": "202012345"
            }
          }
          """;

      mockMvc.perform(post("/api/public/v1/items/{itemId}/rentals", 1L)
              .contentType(APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());

      then(publicRentalService).should(never()).requestRental(any(), any());
    }

    @Test
    @DisplayName("추가 대여자 정보 키 값이 공백이면 400")
    void createRental_blankRenterFieldKey_returns400() throws Exception {
      String requestBody = """
          {
            "itemUnitId": 1,
            "name": "홍길동",
            "phone": "010-1234-5678",
            "renterFields": {
              " ": "컴퓨터",
              "학번": "202012345"
            }
          }
          """;

      mockMvc.perform(post("/api/public/v1/items/{itemId}/rentals", 1L)
              .contentType(APPLICATION_JSON)
              .content(requestBody))
          .andExpect(status().isBadRequest());

      then(publicRentalService).should(never()).requestRental(any(), any());
    }
  }
}
