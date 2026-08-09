package coffee.api.config;

import java.sql.*;
import java.util.UUID;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(UUID.class)
public class UUIDTypeHandler extends BaseTypeHandler<UUID> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setObject(i, parameter);
  }

  @Override
  public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
    String value = rs.getString(columnName);
    return value != null ? UUID.fromString(value) : null;
  }

  @Override
  public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    String value = rs.getString(columnIndex);
    return value != null ? UUID.fromString(value) : null;
  }

  @Override
  public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    String value = cs.getString(columnIndex);
    return value != null ? UUID.fromString(value) : null;
  }
}
