package retrivr.retrivrspring.application.vo;

import org.springframework.core.io.ByteArrayResource;

public record LedgerByteArrayAndFileName(
    ByteArrayResource resource,
    String fileName
) {

}
