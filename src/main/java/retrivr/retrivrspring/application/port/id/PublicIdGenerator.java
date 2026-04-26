package retrivr.retrivrspring.application.port.id;

public interface PublicIdGenerator {

  String generateRentalId(Long organizationId);

  String generateItemId(Long organizationId);
}