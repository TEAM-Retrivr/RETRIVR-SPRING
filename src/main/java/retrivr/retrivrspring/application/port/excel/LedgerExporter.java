package retrivr.retrivrspring.application.port.excel;

public interface LedgerExporter {
  byte[] export(LedgerExportRequest request);
}
