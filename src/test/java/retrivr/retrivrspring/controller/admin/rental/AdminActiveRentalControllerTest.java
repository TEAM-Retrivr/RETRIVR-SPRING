package retrivr.retrivrspring.controller.admin.rental;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import retrivr.retrivrspring.application.service.admin.rental.AdminActiveRentalService;
import retrivr.retrivrspring.application.service.message.SendMessageService;
import retrivr.retrivrspring.global.auth.AuthOrg;
import retrivr.retrivrspring.global.auth.AuthUser;
import retrivr.retrivrspring.global.config.JacksonConfig;
import retrivr.retrivrspring.presentation.admin.rental.AdminActiveRentalController;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminRentalSearchPageResponse;
import retrivr.retrivrspring.presentation.admin.rental.res.AdminRentalSearchPageResponse.RentalSearchSummary;

@ExtendWith(MockitoExtension.class)
class AdminActiveRentalControllerTest {

  private MockMvc mockMvc;

  @Mock
  private AdminActiveRentalService adminActiveRentalService;

  @Mock
  private SendMessageService sendMessageService;

  @BeforeEach
  void setUp() {
    Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
    new JacksonConfig().jsonCustomizer().customize(builder);

    mockMvc = MockMvcBuilders
        .standaloneSetup(new AdminActiveRentalController(adminActiveRentalService, sendMessageService))
        .setMessageConverters(new MappingJackson2HttpMessageConverter(builder.build()))
        .setCustomArgumentResolvers(new AuthOrgArgumentResolver())
        .build();
  }

  @Test
  @DisplayName("rental search API serializes expanded fields, nullable values, and cursors")
  void searchRentals_returnsExpandedResponse() throws Exception {
    Map<String, String> borrowerFields = new LinkedHashMap<>();
    borrowerFields.put("additionalProp1", "컴퓨터공학과");
    borrowerFields.put("additionalProp2", "20260430");
    borrowerFields.put("additionalProp3", "");
    RentalSearchSummary overdue = new RentalSearchSummary(
        1L, "이리트", "010-1234-5678", "c타입 충전기", true, "c타입 충전기(1)",
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10), borrowerFields, "오후 반납", "관리자"
    );
    RentalSearchSummary nonOverdue = new RentalSearchSummary(
        2L, "빈 값", null, "우산", false, null,
        LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 30), Map.of(), null, null
    );
    when(adminActiveRentalService.searchRankedPageByKeyword("충전기", null, null, 2, 10L))
        .thenReturn(new AdminRentalSearchPageResponse(List.of(overdue, nonOverdue), 0.5, 2L));

    mockMvc.perform(get("/api/admin/v1/rentals/search")
            .param("keyword", "충전기")
            .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rentals[0].rentalId").value(1))
        .andExpect(jsonPath("$.rentals[0].isOverdue").value(true))
        .andExpect(jsonPath("$.rentals[0].itemUnitLabel").value("c타입 충전기(1)"))
        .andExpect(jsonPath("$.rentals[0].rentalDate").value("2026-09-01"))
        .andExpect(jsonPath("$.rentals[0].expectedReturnDueDate").value("2026-09-10"))
        .andExpect(jsonPath("$.rentals[0].borrowerFields.additionalProp1").value("컴퓨터공학과"))
        .andExpect(jsonPath("$.rentals[0].borrowerFields.additionalProp3").value(""))
        .andExpect(jsonPath("$.rentals[0].requestNote").value("오후 반납"))
        .andExpect(jsonPath("$.rentals[0].approvalAdminName").value("관리자"))
        .andExpect(jsonPath("$.rentals[1].isOverdue").value(false))
        .andExpect(jsonPath("$.rentals[1].itemUnitLabel").isEmpty())
        .andExpect(jsonPath("$.rentals[1].borrowerFields").isMap())
        .andExpect(jsonPath("$.rentals[1].borrowerFields").isEmpty())
        .andExpect(jsonPath("$.rentals[1].requestNote").isEmpty())
        .andExpect(jsonPath("$.rentals[1].approvalAdminName").isEmpty())
        .andExpect(jsonPath("$.nextScoreCursor").value(0.5))
        .andExpect(jsonPath("$.nextRentalIdCursor").value(2));
  }

  private static class AuthOrgArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.hasParameterAnnotation(AuthOrg.class);
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
      return new AuthUser(10L, "admin@example.com");
    }
  }
}
