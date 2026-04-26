package retrivr.retrivrspring.application.port.image;

public interface ImageStoragePort {

  PresignedUploadUrl createPresignedUploadUrl(
      String objectKey,
      String contentType
  );

  String createPresignedDownloadUrl(
      String objectKey
  );

  void delete(String objectKey);

  boolean exists(String objectKey);
}
