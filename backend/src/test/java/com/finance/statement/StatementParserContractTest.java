package com.finance.statement;

import com.finance.domain.statement.ParsedStatement;
import com.finance.infrastructure.pdf.StatementFormatDetector;
import com.finance.infrastructure.pdf.StatementParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The checks every bank must pass, applied automatically to every StatementParser found on the classpath. Adding a
 * bank means writing one parser class and one synthetic sample (src/test/resources/statements/samples/&lt;bankName&gt;.txt,
 * see docs/05-statement-processing/adding-a-bank.md) - this test then verifies, with no edits, that the new bank
 * recognises itself, parses its sample, and neither steals nor loses a statement to any other bank, however Spring
 * happens to order the parsers. The bugs that real statements exposed (a bank claimed by another bank's name appearing
 * in narration) are exactly what this keeps from coming back for the 200th bank.
 */
class StatementParserContractTest {

    private static List<StatementParser> discoverAllParsers() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(StatementParser.class));
        List<StatementParser> parsers = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("com.finance")) {
            try {
                parsers.add((StatementParser) Class.forName(definition.getBeanClassName()).getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("A StatementParser must have a public no-argument constructor: " + definition.getBeanClassName(), e);
            }
        }
        return parsers;
    }

    private static String sampleFor(StatementParser parser) {
        String path = "/statements/samples/" + parser.bankName() + ".txt";
        try (InputStream in = StatementParserContractTest.class.getResourceAsStream(path)) {
            assertThat(in)
                    .as("%s needs a synthetic sample at src/test/resources%s (see docs/05-statement-processing/adding-a-bank.md)",
                            parser.bankName(), path)
                    .isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private final List<StatementParser> parsers = discoverAllParsers();

    @Test
    void thereAreParsersToCheck() {
        assertThat(parsers).hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    void everyParserHasAUniqueBankNameAndAReadableDisplayName() {
        Set<String> names = new HashSet<>();
        for (StatementParser parser : parsers) {
            assertThat(parser.bankName()).isNotBlank();
            assertThat(parser.displayName()).isNotBlank();
            assertThat(names.add(parser.bankName())).as("duplicate bankName %s", parser.bankName()).isTrue();
        }
    }

    @Test
    void everyParserRecognisesAndParsesItsOwnSample() {
        for (StatementParser parser : parsers) {
            String sample = sampleFor(parser);

            assertThat(parser.matches(sample)).as("%s should recognise its own sample", parser.bankName()).isTrue();
            ParsedStatement parsed = parser.parse(sample);
            assertThat(parsed.rows()).as("%s should parse rows from its sample", parser.bankName()).isNotEmpty();
            parsed.rows().forEach(row -> {
                assertThat(row.transactionDate()).as("%s row date", parser.bankName()).isNotNull();
                assertThat(row.amount().signum()).as("%s row amount is positive", parser.bankName()).isPositive();
                assertThat(row.rawDescription()).as("%s row description", parser.bankName()).isNotBlank();
            });
        }
    }

    @Test
    void everySampleIsDetectedAsItsOwnBankWhateverOrderTheParsersAreRegisteredIn() {
        List<List<StatementParser>> orders = new ArrayList<>();
        orders.add(new ArrayList<>(parsers));
        List<StatementParser> reversed = new ArrayList<>(parsers);
        Collections.reverse(reversed);
        orders.add(reversed);
        for (long seed = 1; seed <= 5; seed++) {
            List<StatementParser> shuffled = new ArrayList<>(parsers);
            Collections.shuffle(shuffled, new Random(seed));
            orders.add(shuffled);
        }

        for (List<StatementParser> order : orders) {
            StatementFormatDetector detector = new StatementFormatDetector(order);
            for (StatementParser parser : parsers) {
                assertThat(detector.detect(sampleFor(parser)).bankName())
                        .as("sample of %s, parsers registered as %s", parser.bankName(), order.stream().map(StatementParser::bankName).toList())
                        .isEqualTo(parser.bankName());
            }
        }
    }
}
