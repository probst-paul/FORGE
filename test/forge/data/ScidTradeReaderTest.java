package forge.data;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
class ScidTradeReaderTest {
    private final ScidTradeReader reader = new ScidTradeReader();

    @TempDir
    Path tempDirectory;

    @Nested
    class ReadTrades {
        @Test
        void readsSingleTradeRecords() throws IOException {
            Path scidFile = tempDirectory.resolve("ESU25_FUT_CME.scid");
            Files.write(scidFile, scidFile(
                    record(1_123_456, 0.0f, 101.25f, 101.0f, 101.25f, 1, 2, 0, 2)
            ));

            List<TradeRow> trades = reader.readTrades(scidFile, 0.25);

            assertEquals(1, trades.size());
            TradeRow trade = trades.get(0);
            assertEquals(
                    LocalDateTime.of(1899, 12, 30, 0, 0, 1, 123_456_000).toInstant(ZoneOffset.UTC),
                    trade.getTradeDateTime()
            );
            assertEquals(405L, trade.getPriceTicks());
            assertEquals(404L, trade.getBidPriceTicks());
            assertEquals(405L, trade.getAskPriceTicks());
            assertEquals(2, trade.getQuantity());
            assertEquals(1, trade.getNumTrades());
            assertEquals(TradeRow.BUY_AGGRESSOR, trade.getSide());
        }

        @Test
        void batchesTrades() throws IOException {
            Path scidFile = tempDirectory.resolve("NQM26_FUT_CME.scid");
            Files.write(scidFile, scidFile(
                    record(1_000_000, 0.0f, 20_001.0f, 20_000.75f, 20_001.0f, 1, 1, 0, 1),
                    record(2_000_000, 0.0f, 20_001.25f, 20_001.0f, 20_001.0f, 1, 3, 3, 0),
                    record(3_000_000, 0.0f, 20_001.5f, 20_001.25f, 20_001.5f, 2, 5, 0, 5)
            ));
            List<List<TradeRow>> batches = new ArrayList<>();

            reader.readTrades(scidFile, 1, 2, 0.25, batches::add);

            assertEquals(2, batches.size());
            assertEquals(2, batches.get(0).size());
            assertEquals(1, batches.get(1).size());
            assertEquals(TradeRow.SELL_AGGRESSOR, batches.get(0).get(1).getSide());
            assertEquals(TradeRow.BUY_AGGRESSOR, batches.get(1).get(0).getSide());
        }

        @Test
        void resumesFromStartRecordIndex() throws IOException {
            Path scidFile = tempDirectory.resolve("RTYH26_FUT_CME.scid");
            Files.write(scidFile, scidFile(
                    record(1_000_000, 0.0f, 2200.1f, 2200.0f, 2200.1f, 1, 1, 0, 1),
                    record(2_000_000, 0.0f, 2200.2f, 2200.1f, 2200.1f, 1, 1, 1, 0),
                    record(3_000_000, 0.0f, 2200.3f, 2200.2f, 2200.3f, 1, 1, 0, 1)
            ));
            List<TradeRow> trades = new ArrayList<>();

            reader.readTrades(scidFile, 3, 10, 0.10, trades::addAll);

            assertEquals(1, trades.size());
            assertEquals(3, trades.get(0).getScidRecordIndex());
            assertEquals(Instant.ofEpochSecond(-2209161597L), trades.get(0).getTradeDateTime());
        }

        @Test
        void importsRecordsWithAmbiguousAggressorSideAsNullSide() throws IOException {
            Path scidFile = tempDirectory.resolve("CLU25_FUT_CME.scid");
            Files.write(scidFile, scidFile(
                    record(1_000_000, 0.0f, 70.10f, 70.09f, 70.10f, 2, 5, 2, 3)
            ));

            List<TradeRow> trades = reader.readTrades(scidFile, 0.01);

            assertEquals(1, trades.size());
            assertEquals(null, trades.get(0).getSide());
        }

        @Test
        void rejectsInvalidHeaders() throws IOException {
            Path scidFile = tempDirectory.resolve("BAD.scid");
            ByteBuffer file = ByteBuffer.allocate(56).order(ByteOrder.LITTLE_ENDIAN);
            file.put("NOPE".getBytes());
            file.putInt(56);
            file.putInt(40);
            file.putShort((short) 1);
            file.putShort((short) 0);
            file.putInt(0);

            Files.write(scidFile, file.array());

            assertThrows(IllegalArgumentException.class, () -> reader.readTrades(scidFile, 0.25));
        }

        @Test
        void rejectsPricesThatAreNotAlignedToTickSize() throws IOException {
            Path scidFile = tempDirectory.resolve("ESU25_FUT_CME.scid");
            Files.write(scidFile, scidFile(
                    record(1_000_000, 0.0f, 101.13f, 101.0f, 101.13f, 1, 2, 0, 2)
            ));

            assertThrows(IllegalArgumentException.class, () -> reader.readTrades(scidFile, 0.25));
        }
    }

    private byte[] scidFile(byte[]... records) {
        ByteBuffer file = ByteBuffer.allocate(56 + records.length * 40).order(ByteOrder.LITTLE_ENDIAN);
        file.put("SCID".getBytes());
        file.putInt(56);
        file.putInt(40);
        file.putShort((short) 1);
        file.putShort((short) 0);
        file.putInt(0);
        file.put(new byte[36]);
        for (byte[] record : records) {
            file.put(record);
        }
        return file.array();
    }

    private byte[] record(
            long dateTimeMicros,
            float open,
            float high,
            float low,
            float close,
            int numTrades,
            int totalVolume,
            int bidVolume,
            int askVolume
    ) {
        ByteBuffer record = ByteBuffer.allocate(40).order(ByteOrder.LITTLE_ENDIAN);
        record.putLong(dateTimeMicros);
        record.putFloat(open);
        record.putFloat(high);
        record.putFloat(low);
        record.putFloat(close);
        record.putInt(numTrades);
        record.putInt(totalVolume);
        record.putInt(bidVolume);
        record.putInt(askVolume);
        return record.array();
    }
}
