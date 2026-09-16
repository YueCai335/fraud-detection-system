package com.yuecai.fraud.prediction;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import jakarta.validation.Validation;
import java.util.Locale;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CsvTransactionParserTest {

    // Bean Validation messages are localized from the JVM default locale outside a web request;
    // pin English so the expected strings match on any developer machine and in CI.
    private static Locale originalLocale;

    @BeforeAll
    static void pinLocale() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
    }

    @AfterAll
    static void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    private final CsvTransactionParser parser =
            new CsvTransactionParser(Validation.buildDefaultValidatorFactory().getValidator());

    private CsvTransactionParser.ParseResult parse(String csv) {
        return parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void skipsHeaderAndBlankLinesAndKeepsLineNumbers() {
        var result = parse("""
                step,type_code,amount,oldbalanceOrg,newbalanceOrig,oldbalanceDest,newbalanceDest
                1,3,9839.64,170136.0,160296.36,0.0,0.0

                100,1,10000.0,10000.0,0.0,0.0,10000.0
                """);

        assertThat(result.skipped()).isEmpty();
        assertThat(result.rows()).hasSize(2);
        assertThat(result.rows().get(0).lineNumber()).isEqualTo(2);
        assertThat(result.rows().get(1).lineNumber()).isEqualTo(4);
        assertThat(result.rows().get(1).transaction().typeCode()).isEqualTo(1);
    }

    @Test
    void worksWithoutHeader() {
        var result = parse("1,3,9839.64,170136.0,160296.36,0.0,0.0\n");
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).lineNumber()).isEqualTo(1);
    }

    @Test
    void reportsBadRowsInsteadOfZeroFilling() {
        var result = parse("""
                1,3,9839.64,170136.0,160296.36,0.0,0.0
                1,3,abc,170136.0,160296.36,0.0,0.0
                1,3,9839.64
                1,9,9839.64,170136.0,160296.36,0.0,0.0
                """);

        assertThat(result.rows()).hasSize(1);
        assertThat(result.skipped()).containsExactly(
                "line 2: non-numeric value",
                "line 3: expected 7 columns, found 3",
                "line 4: typeCode must be less than or equal to 4");
    }

    @Test
    void appliesTheSameConstraintsAsTheSingleEndpoint() {
        // Regression: negative amounts used to pass through CSV while POST /predictions rejected them.
        var result = parse("""
                100,1,-5,10000.0,0.0,0.0,10000.0
                -1,1,10000.0,10000.0,0.0,0.0,10000.0
                100,1,10000.0,-1.0,0.0,0.0,10000.0
                100,1,10000.0,10000.0,0.0,0.0,10000.0
                """);

        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).lineNumber()).isEqualTo(4);
        assertThat(result.skipped()).containsExactly(
                "line 1: amount must be greater than or equal to 0",
                "line 2: step must be greater than or equal to 0",
                "line 3: oldbalanceOrg must be greater than or equal to 0");
    }
}
