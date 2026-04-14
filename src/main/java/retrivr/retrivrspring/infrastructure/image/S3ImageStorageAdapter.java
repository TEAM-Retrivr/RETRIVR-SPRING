package retrivr.retrivrspring.infrastructure.image;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import retrivr.retrivrspring.application.port.image.ImageStoragePort;
import retrivr.retrivrspring.application.port.image.PresignedUploadUrl;
import retrivr.retrivrspring.global.aws.S3Properties;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@RequiredArgsConstructor
public class S3ImageStorageAdapter implements ImageStoragePort {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final S3Properties s3Properties;

  @Override
  public PresignedUploadUrl createPresignedUploadUrl(String objectKey, String contentType) {
    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(s3Properties.bucket())
        .key(objectKey)
        .contentType(contentType)
        .build();

    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofSeconds(s3Properties.putExpiration()))
        .putObjectRequest(putObjectRequest)
        .build();

    PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

    return new PresignedUploadUrl(
        objectKey,
        presignedRequest.url().toString()
    );
  }

  @Override
  public String createPresignedDownloadUrl(String objectKey) {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
        .bucket(s3Properties.bucket())
        .key(objectKey)
        .build();

    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofSeconds(s3Properties.getExpiration()))
        .getObjectRequest(getObjectRequest)
        .build();

    PresignedGetObjectRequest presignedRequest =
        s3Presigner.presignGetObject(presignRequest);

    return presignedRequest.url().toString();
  }

  @Override
  public void delete(String objectKey) {
    DeleteObjectRequest request = DeleteObjectRequest.builder()
        .bucket(s3Properties.bucket())
        .key(objectKey)
        .build();

    s3Client.deleteObject(request);
  }

  @Override
  public boolean exists(String objectKey) {
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(s3Properties.bucket())
        .key(objectKey)
        .build();

    try {
      HeadObjectResponse ignored = s3Client.headObject(request);
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    } catch (S3Exception e) {
      if (e.statusCode() == 404) {
        return false;
      }
      throw e;
    }
  }
}
