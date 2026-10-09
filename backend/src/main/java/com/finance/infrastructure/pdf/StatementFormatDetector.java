package com.finance.infrastructure.pdf;

import com.finance.application.exception.UnsupportedFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Picks the StatementParser whose bank the extracted text belongs to. Spring injects every StatementParser
 * @Component here as a List - adding another bank means adding one new parser class, not touching this detector.
 *
 * The outcome never depends on the order Spring registers the parsers in (that comes from classpath scanning, so it
 * differs between a laptop and a jar): exactly one match wins; if several parsers claim the text, the one whose own
 * name appears earliest wins (the letterhead precedes narration that merely mentions another bank); only a genuine
 * tie is refused, loudly, rather than guessed - a wrong parser silently returning wrong rows is worse than an error.
 */
@Component
public class StatementFormatDetector {

    private static final Logger log = LoggerFactory.getLogger(StatementFormatDetector.class);

    private final List<StatementParser> parsers;

    public StatementFormatDetector(List<StatementParser> parsers) {
        this.parsers = parsers;
    }

    public StatementParser detect(String extractedText) {
        List<StatementParser> matching = parsers.stream().filter(parser -> parser.matches(extractedText)).toList();
        if (matching.isEmpty()) {
            throw new UnsupportedFileException(
                    "This statement's format isn't recognized yet. Supported: " + supportedBanks() + ".");
        }
        if (matching.size() == 1) {
            return matching.get(0);
        }

        List<StatementParser> byPosition = matching.stream()
                .sorted(Comparator.comparingInt(parser -> parser.identityPosition(extractedText)))
                .toList();
        int best = byPosition.get(0).identityPosition(extractedText);
        int second = byPosition.get(1).identityPosition(extractedText);
        if (best == second) {
            String names = matching.stream().map(StatementParser::bankName).collect(Collectors.joining(", "));
            log.warn("Statement format is ambiguous - several parsers claim it equally: {}", names);
            throw new UnsupportedFileException(
                    "This statement matches more than one supported format, so it was not imported to avoid reading it wrongly.");
        }
        return byPosition.get(0);
    }

    private String supportedBanks() {
        return parsers.stream().map(StatementParser::displayName).sorted().collect(Collectors.joining(", "));
    }
}
