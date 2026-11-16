package coffee.api.dto.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Generated revenue report file: download name and .xlsx bytes. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueExportResult {
  private String fileName;
  private byte[] content;
}
