package retrivr.retrivrspring.global.swagger;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExample;
import retrivr.retrivrspring.global.swagger.annotation.ApiErrorCodeExamples;
import retrivr.retrivrspring.global.error.ErrorCode;
import retrivr.retrivrspring.global.error.ErrorResponse;

@Configuration
public class SwaggerConfig {

  @Value("${app.base-url}")
  private String baseUrl;

  @Value("${server.port}")
  private String port;

  @Bean
  public OpenAPI retrivrOpenAPI() {
    String securitySchemeName = "bearerAuth";

    return new OpenAPI()
        .info(new Info()
            .title("Retrivr API")
            .description("Retrivr 대여 서비스 API 문서")
            .version("v1.0.0")
        )
        .servers(List.of(
            new Server().url(baseUrl + ":" + port).description("Server")
        ))
        .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
            .addList(securitySchemeName))
        .components(new io.swagger.v3.oas.models.Components()
            .addSecuritySchemes(securitySchemeName,
                new io.swagger.v3.oas.models.security.SecurityScheme()
                    .name("Authorization")
                    .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            )
        );
  }

  @Bean
  public OperationCustomizer customize() {
    return (Operation operation, HandlerMethod handlerMethod) -> {
      ApiErrorCodeExamples multi = handlerMethod.getMethodAnnotation(ApiErrorCodeExamples.class);
      if (multi != null) {
        generateErrorCodeResponseExample(operation, multi.value());
        return operation;
      }

      ApiErrorCodeExample single = handlerMethod.getMethodAnnotation(ApiErrorCodeExample.class);
      if (single != null) {
        generateErrorCodeResponseExample(operation, single.value());
      }

      return operation;
    };
  }

  private void generateErrorCodeResponseExample(Operation operation, ErrorCode[] errorCodes) {
    ApiResponses responses = operation.getResponses();
    if (responses == null) {
      responses = new ApiResponses();
      operation.setResponses(responses);
    }

    Map<Integer, List<ExampleHolder>> grouped = Arrays.stream(errorCodes)
        .map(errorCode -> ExampleHolder.builder()
            .holder(getSwaggerExample(errorCode))
            .code(errorCode.getHttpStatus().value())
            .name(errorCode.name())
            .build()
        )
        .collect(Collectors.groupingBy(ExampleHolder::getCode));

    addExamplesToResponses(responses, grouped);
  }

  private void generateErrorCodeResponseExample(Operation operation, ErrorCode errorCode) {
    ApiResponses responses = operation.getResponses();
    if (responses == null) {
      responses = new ApiResponses();
      operation.setResponses(responses);
    }

    ExampleHolder exampleHolder = ExampleHolder.builder()
        .holder(getSwaggerExample(errorCode))
        .name(errorCode.name())
        .code(errorCode.getHttpStatus().value())
        .build();

    addExamplesToResponses(responses, exampleHolder);
  }

  private Example getSwaggerExample(ErrorCode errorCode) {
    ErrorResponse errorResponse = ErrorResponse.of(errorCode);
    Example example = new Example();
    example.setValue(errorResponse);
    return example;
  }

  private void addExamplesToResponses(ApiResponses responses,
      Map<Integer, List<ExampleHolder>> statusWithExampleHolders) {

    statusWithExampleHolders.forEach((status, holders) -> {
      Content content = new Content();
      MediaType mediaType = new MediaType();
      ApiResponse apiResponse = new ApiResponse();

      holders.forEach(h -> mediaType.addExamples(h.getName(), h.getHolder()));

      content.addMediaType("application/json", mediaType);
      apiResponse.setContent(content);
      responses.addApiResponse(String.valueOf(status), apiResponse);
    });
  }

  private void addExamplesToResponses(ApiResponses responses, ExampleHolder exampleHolder) {
    Content content = new Content();
    MediaType mediaType = new MediaType();
    ApiResponse apiResponse = new ApiResponse();

    mediaType.addExamples(exampleHolder.getName(), exampleHolder.getHolder());
    content.addMediaType("application/json", mediaType);

    apiResponse.setContent(content);
    responses.addApiResponse(String.valueOf(exampleHolder.getCode()), apiResponse);
  }
}
