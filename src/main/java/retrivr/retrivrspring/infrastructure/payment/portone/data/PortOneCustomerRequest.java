package retrivr.retrivrspring.infrastructure.payment.portone.data;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PortOneCustomerRequest(
    String id,
    PortOneCustomerNameRequest name,
    String email,
    String phoneNumber
) {

  public static PortOneCustomerRequest of(String id, String name, String email, String phoneNumber) {
    return new PortOneCustomerRequest(
        id,
        name == null ? null : new PortOneCustomerNameRequest(name),
        email,
        phoneNumber
    );
  }
}
