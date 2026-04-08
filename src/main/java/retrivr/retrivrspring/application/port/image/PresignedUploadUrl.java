package retrivr.retrivrspring.application.port.image;

public record PresignedUploadUrl(
    String objectKey,
    String uploadUrl
) {

}
