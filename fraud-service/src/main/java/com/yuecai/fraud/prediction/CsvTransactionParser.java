package com.yuecai.fraud.prediction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Parses the batch-upload CSV: {@code step,type_code,amount,oldbalanceOrg,newbalanceOrig,oldbalanceDest,newbalanceDest}.
 * A header row is optional and detected the same way the legacy servlet did. Rows that cannot be
 * parsed are reported back with their line number instead of being silently zero-filled.
 */
@Component
public class CsvTransactionParser {

    public static final String EXPECTED_HEADER =
            "step,type_code,amount,oldbalanceOrg,newbalanceOrig,oldbalanceDest,newbalanceDest";
    private static final int COLUMNS = 7;

    public record Row(int lineNumber, TransactionRequest transaction) {}

    public record ParseResult(List<Row> rows, List<String> skipped) {}

    public ParseResult parse(InputStream in) {
        List<Row> rows = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.strip();
                if (line.isEmpty()) continue;
                if (first) {
                    first = false;
                    if (looksLikeHeader(line)) continue;
                }
                String[] cols = line.split(",", -1);
                if (cols.length < COLUMNS) {
                    skipped.add("line " + lineNumber + ": expected " + COLUMNS + " columns, found " + cols.length);
                    continue;
                }
                try {
                    TransactionRequest tx = new TransactionRequest(
                            Integer.parseInt(cols[0].strip()),
                            Integer.parseInt(cols[1].strip()),
                            Double.parseDouble(cols[2].strip()),
                            Double.parseDouble(cols[3].strip()),
                            Double.parseDouble(cols[4].strip()),
                            Double.parseDouble(cols[5].strip()),
                            Double.parseDouble(cols[6].strip()));
                    if (!TransactionType.isValidCode(tx.typeCode())) {
                        skipped.add("line " + lineNumber + ": type_code must be 0–4");
                        continue;
                    }
                    rows.add(new Row(lineNumber, tx));
                } catch (NumberFormatException e) {
                    skipped.add("line " + lineNumber + ": non-numeric value");
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new ParseResult(rows, skipped);
    }

    static boolean looksLikeHeader(String line) {
        String low = line.toLowerCase(Locale.ROOT);
        return low.contains("step") && low.contains("type") && low.contains("amount");
    }
}
