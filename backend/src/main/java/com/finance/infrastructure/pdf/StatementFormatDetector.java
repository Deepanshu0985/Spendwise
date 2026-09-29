package com.finance.infrastructure.pdf;

import com.finance.application.exception.UnsupportedFileException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Picks the StatementParser whose bank the extracted text looks like it belongs
 * to. Spring injects every StatementParser @Component here as a List - adding a
 * fourth bank later (post-MVP, per ADR-014) means adding one new parser class,
 * not touching this detector.
 */
@Component
public class StatementFormatDetector {

    private final List<StatementParser> parsers;

    public StatementFormatDetector(List<StatementParser> parsers) {
        this.parsers = parsers;
    }

    public StatementParser detect(String extractedText) {
        return parsers.stream()
                .filter(parser -> parser.matches(extractedText))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileException(
                        "This statement's format isn't recognized. Supported banks: HDFC Bank, SBI, Axis Bank."));
    }
}
