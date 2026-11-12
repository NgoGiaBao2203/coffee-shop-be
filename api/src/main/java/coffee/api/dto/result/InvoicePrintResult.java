package coffee.api.dto.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Generated invoice receipt: download name and PDF bytes. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePrintResult {
  private String fileName;
  private byte[] content;
}
