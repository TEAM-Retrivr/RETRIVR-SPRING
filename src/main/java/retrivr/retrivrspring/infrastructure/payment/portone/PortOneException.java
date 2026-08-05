package retrivr.retrivrspring.infrastructure.payment.portone;

import retrivr.retrivrspring.global.error.InfraException;
import retrivr.retrivrspring.global.error.ErrorCode;

public class PortOneException extends InfraException {

  public PortOneException(String detail) {
    super(ErrorCode.PAYMENT_FAILED, detail);
  }

  public PortOneException(String detail, Throwable cause) {
    super(ErrorCode.PAYMENT_FAILED, detail, cause);
  }
}
