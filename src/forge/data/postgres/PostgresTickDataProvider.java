package forge.data.postgres;

import forge.data.market.ContractTradeWindow;
import forge.data.market.TickDataProvider;
import forge.data.market.TradeBatchReader;
import forge.data.market.TradeTick;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostgresTickDataProvider implements TickDataProvider {
    private final PostgresDatabaseSettings settings;

    public PostgresTickDataProvider(PostgresDatabaseSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("settings is required");
        }
        this.settings = settings;
    }

    @Override
    public TradeBatchReader openReader(List<ContractTradeWindow> windows, int batchSize) {
        if (windows == null || windows.isEmpty()) {
            throw new IllegalArgumentException("at least one contract window is required");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        return new PostgresTradeBatchReader(windows, batchSize);
    }

    private class PostgresTradeBatchReader implements TradeBatchReader {
        private final List<ContractTradeWindow> windows;
        private final int batchSize;
        private int windowIndex;
        private Instant lastTradeDateTime;
        private long lastScidRecordIndex;

        private PostgresTradeBatchReader(List<ContractTradeWindow> windows, int batchSize) {
            this.windows = Collections.unmodifiableList(new ArrayList<>(windows));
            this.batchSize = batchSize;
        }

        @Override
        public List<TradeTick> readNextBatch() {
            while (windowIndex < windows.size()) {
                ContractTradeWindow window = windows.get(windowIndex);
                List<TradeTick> batch = readBatch(window);
                if (!batch.isEmpty()) {
                    TradeTick lastTick = batch.get(batch.size() - 1);
                    lastTradeDateTime = lastTick.getTradeDateTime();
                    lastScidRecordIndex = lastTick.getScidRecordIndex();
                    return batch;
                }

                windowIndex++;
                lastTradeDateTime = null;
                lastScidRecordIndex = 0;
            }
            return Collections.emptyList();
        }

        private List<TradeTick> readBatch(ContractTradeWindow window) {
            String tableName = validateContractTableName(window.getContractSymbol());
            boolean firstBatchForWindow = lastTradeDateTime == null;
            String sql = buildBatchSql(tableName, firstBatchForWindow);

            try (Connection connection = DriverManager.getConnection(
                    settings.primaryJdbcUrl(),
                    settings.getUsername(),
                    settings.getPassword()
            );
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                int parameterIndex = 1;
                statement.setTimestamp(parameterIndex++, Timestamp.from(window.getStartInclusiveInstant()));
                statement.setTimestamp(parameterIndex++, Timestamp.from(window.getEndExclusiveInstant()));
                if (!firstBatchForWindow) {
                    statement.setTimestamp(parameterIndex++, Timestamp.from(lastTradeDateTime));
                    statement.setTimestamp(parameterIndex++, Timestamp.from(lastTradeDateTime));
                    statement.setLong(parameterIndex++, lastScidRecordIndex);
                }
                statement.setInt(parameterIndex, batchSize);

                try (ResultSet resultSet = statement.executeQuery()) {
                    List<TradeTick> batch = new ArrayList<>();
                    while (resultSet.next()) {
                        batch.add(toTradeTick(window.getContractSymbol(), resultSet));
                    }
                    return Collections.unmodifiableList(batch);
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not read trade ticks from PostgreSQL table '" + tableName + "'", exception);
            }
        }

        private String buildBatchSql(String tableName, boolean firstBatchForWindow) {
            StringBuilder sql = new StringBuilder()
                    .append("SELECT ")
                    .append(quoteIdentifier("tradeDateTime")).append(", ")
                    .append(quoteIdentifier("priceTicks")).append(", ")
                    .append(quoteIdentifier("bidPriceTicks")).append(", ")
                    .append(quoteIdentifier("askPriceTicks")).append(", ")
                    .append("quantity, ")
                    .append("side, ")
                    .append(quoteIdentifier("scidRecordIndex"))
                    .append(" FROM ").append(quoteIdentifier(tableName))
                    .append(" WHERE side IS NOT NULL")
                    .append(" AND ").append(quoteIdentifier("tradeDateTime")).append(" >= ?")
                    .append(" AND ").append(quoteIdentifier("tradeDateTime")).append(" < ?");
            if (!firstBatchForWindow) {
                sql.append(" AND (")
                        .append(quoteIdentifier("tradeDateTime")).append(" > ?")
                        .append(" OR (")
                        .append(quoteIdentifier("tradeDateTime")).append(" = ?")
                        .append(" AND ").append(quoteIdentifier("scidRecordIndex")).append(" > ?")
                        .append("))");
            }
            sql.append(" ORDER BY ")
                    .append(quoteIdentifier("tradeDateTime"))
                    .append(", ")
                    .append(quoteIdentifier("scidRecordIndex"))
                    .append(" LIMIT ?");
            return sql.toString();
        }
    }

    private TradeTick toTradeTick(String contractSymbol, ResultSet resultSet) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(1);
        Long bidPriceTicks = nullableLong(resultSet, 3);
        Long askPriceTicks = nullableLong(resultSet, 4);
        return new TradeTick(
                contractSymbol,
                timestamp.toInstant(),
                resultSet.getLong(2),
                bidPriceTicks,
                askPriceTicks,
                resultSet.getLong(5),
                resultSet.getInt(6),
                resultSet.getLong(7)
        );
    }

    private Long nullableLong(ResultSet resultSet, int columnIndex) throws SQLException {
        long value = resultSet.getLong(columnIndex);
        if (resultSet.wasNull()) {
            return null;
        }
        return value;
    }

    private String validateContractTableName(String tableName) {
        if (tableName == null || !tableName.toUpperCase().matches("[A-Z]{1,3}[FGHJKMNQUVXZ][0-9]{1,2}")) {
            throw new IllegalArgumentException("contract table name is invalid: " + tableName);
        }
        return tableName.toUpperCase();
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
