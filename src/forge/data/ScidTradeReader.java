package forge.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ScidTradeReader {
    private static final String EXPECTED_HEADER_ID = "SCID";
    private static final int EXPECTED_HEADER_SIZE = 56;
    private static final int EXPECTED_RECORD_SIZE = 40;
    private static final int READ_BUFFER_RECORD_COUNT = 4096;
    private static final Instant SCID_EPOCH = LocalDateTime.of(1899, 12, 30, 0, 0).toInstant(ZoneOffset.UTC);

    public List<TradeRow> readTrades(Path scidFilePath) {
        List<TradeRow> trades = new ArrayList<>();
        readTrades(scidFilePath, 1, READ_BUFFER_RECORD_COUNT, trades::addAll);
        return trades;
    }

    public void readTrades(Path scidFilePath, long startRecordIndex, int batchSize, Consumer<List<TradeRow>> tradeBatchConsumer) {
        if (startRecordIndex < 1) {
            throw new IllegalArgumentException("startRecordIndex must be greater than zero");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }
        if (tradeBatchConsumer == null) {
            throw new IllegalArgumentException("tradeBatchConsumer is required");
        }

        try (FileChannel channel = FileChannel.open(scidFilePath, StandardOpenOption.READ)) {
            ScidHeader header = readHeader(channel);
            validateHeader(header);
            channel.position(header.getHeaderSize() + ((startRecordIndex - 1) * header.getRecordSize()));
            long currentRecordIndex = startRecordIndex;

            List<TradeRow> trades = new ArrayList<>(batchSize);
            ByteBuffer recordsBuffer = ByteBuffer
                    .allocate(header.getRecordSize() * READ_BUFFER_RECORD_COUNT)
                    .order(ByteOrder.LITTLE_ENDIAN);

            while (channel.read(recordsBuffer) != -1) {
                recordsBuffer.flip();
                while (recordsBuffer.remaining() >= header.getRecordSize()) {
                    int recordStart = recordsBuffer.position();
                    trades.add(readTrade(recordsBuffer, currentRecordIndex));
                    currentRecordIndex++;
                    if (trades.size() == batchSize) {
                        tradeBatchConsumer.accept(new ArrayList<>(trades));
                        trades.clear();
                    }
                    recordsBuffer.position(recordStart + header.getRecordSize());
                }
                recordsBuffer.compact();
            }

            recordsBuffer.flip();
            if (recordsBuffer.hasRemaining()) {
                throw new IllegalArgumentException("SCID file contains a partial trailing record");
            }

            if (!trades.isEmpty()) {
                tradeBatchConsumer.accept(new ArrayList<>(trades));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read SCID file: " + scidFilePath, exception);
        }
    }

    private ScidHeader readHeader(FileChannel channel) throws IOException {
        ByteBuffer headerBuffer = ByteBuffer.allocate(EXPECTED_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        while (headerBuffer.hasRemaining() && channel.read(headerBuffer) != -1) {
            // Keep reading until the fixed header buffer is full or EOF is reached.
        }
        if (headerBuffer.hasRemaining()) {
            throw new IllegalArgumentException("SCID file is too small to contain a complete header");
        }

        headerBuffer.flip();
        byte[] headerIdBytes = new byte[4];
        headerBuffer.get(headerIdBytes);
        String headerId = new String(headerIdBytes, StandardCharsets.US_ASCII);
        long headerSize = Integer.toUnsignedLong(headerBuffer.getInt());
        long recordSize = Integer.toUnsignedLong(headerBuffer.getInt());
        int version = Short.toUnsignedInt(headerBuffer.getShort());

        return new ScidHeader(headerId, headerSize, recordSize, version);
    }

    private void validateHeader(ScidHeader header) {
        if (!EXPECTED_HEADER_ID.equals(header.getHeaderId())) {
            throw new IllegalArgumentException("SCID file header is invalid");
        }
        if (header.getHeaderSize() != EXPECTED_HEADER_SIZE) {
            throw new IllegalArgumentException("Unsupported SCID header size: " + header.getHeaderSize());
        }
        if (header.getRecordSize() != EXPECTED_RECORD_SIZE) {
            throw new IllegalArgumentException("Unsupported SCID record size: " + header.getRecordSize());
        }
    }

    private TradeRow readTrade(ByteBuffer recordBuffer, long recordIndex) {
        long scidDateTimeMicros = recordBuffer.getLong();
        recordBuffer.getFloat(); // Open is not imported yet.
        float askPrice = recordBuffer.getFloat();
        float bidPrice = recordBuffer.getFloat();
        float price = recordBuffer.getFloat();
        long numTrades = Integer.toUnsignedLong(recordBuffer.getInt());
        long totalVolume = Integer.toUnsignedLong(recordBuffer.getInt());
        long bidVolume = Integer.toUnsignedLong(recordBuffer.getInt());
        long askVolume = Integer.toUnsignedLong(recordBuffer.getInt());

        return new TradeRow(
                convertDateTime(scidDateTimeMicros),
                price,
                nullablePrice(bidPrice),
                nullablePrice(askPrice),
                totalVolume,
                resolveSide(numTrades, bidVolume, askVolume),
                numTrades,
                recordIndex
        );
    }

    private Instant convertDateTime(long scidDateTimeMicros) {
        long seconds = Math.floorDiv(scidDateTimeMicros, 1_000_000L);
        long microAdjustment = Math.floorMod(scidDateTimeMicros, 1_000_000L);
        return SCID_EPOCH.plusSeconds(seconds).plusNanos(microAdjustment * 1_000L);
    }

    private Float nullablePrice(float price) {
        if (price == 0.0f) {
            return null;
        }
        return price;
    }

    private int resolveSide(long numTrades, long bidVolume, long askVolume) {
        if (askVolume > 0 && bidVolume == 0) {
            return TradeRow.BUY_AGGRESSOR;
        }
        if (bidVolume > 0 && askVolume == 0) {
            return TradeRow.SELL_AGGRESSOR;
        }
        throw new IllegalArgumentException(
                "Could not determine aggressor side for SCID record with numTrades=" + numTrades +
                        ", bidVolume=" + bidVolume +
                        ", askVolume=" + askVolume
        );
    }

    private static class ScidHeader {
        private final String headerId;
        private final long headerSize;
        private final long recordSize;
        private final int version;

        private ScidHeader(String headerId, long headerSize, long recordSize, int version) {
            this.headerId = headerId;
            this.headerSize = headerSize;
            this.recordSize = recordSize;
            this.version = version;
        }

        public String getHeaderId() {
            return headerId;
        }

        public long getHeaderSize() {
            return headerSize;
        }

        public int getRecordSize() {
            return Math.toIntExact(recordSize);
        }

        @SuppressWarnings("unused")
        public int getVersion() {
            return version;
        }
    }
}
