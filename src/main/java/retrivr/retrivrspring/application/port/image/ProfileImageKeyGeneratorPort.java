package retrivr.retrivrspring.application.port.image;

public interface ProfileImageKeyGeneratorPort {
  String generate(Long organizationId, String extension);
  boolean isProfileImageKeyOwner(Long organizationId, String objectKey);
}
