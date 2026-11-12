package com.finance.infrastructure.pdf;

import com.finance.domain.statement.ParsedStatement;
import com.finance.domain.statement.ParsedTransactionRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The statement the Bank of Baroda mobile app ("bob World") produces, which is a different document from the older net-banking
 * export BobStatementParser reads (found with a real statement the older reader could not recognise). Columns are Sr.No,
 * transaction date, value date, description, cheque number, debit, credit, balance, with every heading also printed in Hindi.
 * In the extracted text each transaction is a block, and the text order is not the column order:
 *
 * <pre>
 * 4,900.001 02-10-2026 02-10-2026          running balance, immediately followed by the Sr.No (no space), then the two dates
 * UPI/200000000001/10:00:00/UPI/shop@ptybl/Se        the narration, wrapped over one or more lines
 * nt                                          (a continuation)
 * 100.00 -                                    debit, credit: a dash for the empty one ("- 250.00" is a credit)
 * </pre>
 *
 * A short narration can share the first line with the dates and the amounts can share the last narration line:
 * {@code 11,594.004 06-10-2026 06-10-2026 ACHDR/EMIDUE/1234567890/111397544160 5,556.00 -}. Unlike the older BOB layout the
 * statement says outright whether an amount is a debit or a credit, so the direction is read, not inferred from balances.
 * The balance and Sr.No run together, but the balance always has exactly two decimals, so the split is unambiguous.
 * Page footers, the repeated headings on each page and the "Opening Balance" line are skipped.
 */
@Component
public class BobWorldStatementParser implements StatementParser {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String AMOUNT_OR_DASH = "(-|[\\d,]+\\.\\d{2})";

    private static final Pattern PERIOD = Pattern.compile("Account Statement from (\\d{2}-\\d{2}-\\d{4}) to (\\d{2}-\\d{2}-\\d{4})");
    private static final Pattern BALANCE_ROW_LABEL = Pattern.compile(
            "^[\\d,]+\\.\\d{2}\\d+\\s+\\d{2}-\\d{2}-\\d{4}\\s+(?:Opening|Closing) Balance\\s+-\\s+-$");
    private static final Pattern BLOCK_START = Pattern.compile(
            "^([\\d,]+\\.\\d{2})(\\d+)\\s+(\\d{2}-\\d{2}-\\d{4})\\s+(\\d{2}-\\d{2}-\\d{4})(?:\\s+(.+))?$");
    private static final Pattern AMOUNTS_ONLY = Pattern.compile("^" + AMOUNT_OR_DASH + "\\s+" + AMOUNT_OR_DASH + "$");
    private static final Pattern TEXT_THEN_AMOUNTS = Pattern.compile("^(.*\\S)\\s+" + AMOUNT_OR_DASH + "\\s+" + AMOUNT_OR_DASH + "$");

    // The UPI reference number is globally unique, so it also matches the same payment in another account's statement.
    private static final Pattern UPI_REFERENCE = Pattern.compile("^UPI/(\\d{9,})/");

    @Override
    public String bankName() {
        return "BOB_WORLD";
    }

    @Override
    public String displayName() {
        return "Bank of Baroda (bob World app)";
    }

    @Override
    public int identityPosition(String extractedText) {
        int position = extractedText.toUpperCase().indexOf("BOB WORLD");
        return position >= 0 ? position : Math.max(0, extractedText.indexOf("Account Statement from"));
    }

    @Override
    public boolean matches(String extractedText) {
        // This layout's own structure, not a bank name: the run-together balance and Sr.No line, and the debit/credit amount
        // line that closes each block. Another bank's statement that merely mentions Bank of Baroda in a narration has neither.
        return extractedText.contains("Account Statement from")
                && StatementParsingSupport.hasMatchingRow(extractedText, BLOCK_START)
                && StatementParsingSupport.hasMatchingRow(extractedText, AMOUNTS_ONLY);
    }

    @Override
    public ParsedStatement parse(String extractedText) {
        List<ParsedTransactionRow> rows = new ArrayList<>();
        Block block = null;

        for (String rawLine : extractedText.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (BALANCE_ROW_LABEL.matcher(line).matches()) {
                block = null; // a checkpoint, not a transaction
                continue;
            }
            Matcher start = BLOCK_START.matcher(line);
            if (start.matches()) {
                // A new block while one is still open means the open one never got its amounts: it is dropped, not guessed.
                block = new Block(
                        StatementParsingSupport.parseAmount(start.group(1)), start.group(2),
                        StatementParsingSupport.parseDate(start.group(3), DATE_FORMAT));
                String tail = start.group(5);
                if (tail != null) {
                    Matcher inline = TEXT_THEN_AMOUNTS.matcher(tail.trim());
                    if (inline.matches()) {
                        rows.add(block.finish(inline.group(1), inline.group(2), inline.group(3)));
                        block = null;
                    } else {
                        block.addNarration(tail.trim(), rawLine.endsWith(" "));
                    }
                }
                continue;
            }
            if (block == null) {
                continue; // headings, footers, page numbers, Hindi labels
            }
            Matcher amounts = AMOUNTS_ONLY.matcher(line);
            if (amounts.matches()) {
                rows.add(block.finish(null, amounts.group(1), amounts.group(2)));
                block = null;
                continue;
            }
            Matcher trailing = TEXT_THEN_AMOUNTS.matcher(line);
            if (trailing.matches()) {
                rows.add(block.finish(trailing.group(1), trailing.group(2), trailing.group(3)));
                block = null;
                continue;
            }
            block.addNarration(line, rawLine.endsWith(" "));
        }

        LocalDate start = null;
        LocalDate end = null;
        Matcher period = PERIOD.matcher(extractedText);
        if (period.find()) {
            start = StatementParsingSupport.parseDate(period.group(1), DATE_FORMAT);
            end = StatementParsingSupport.parseDate(period.group(2), DATE_FORMAT);
        }
        return new ParsedStatement(bankName(), start, end, rows);
    }

    /** One transaction being collected: its balance, Sr.No and date, then the narration lines until the amounts arrive. */
    private static final class Block {
        private final BigDecimal balance;
        private final String serialNumber;
        private final LocalDate date;
        private final StringBuilder narration = new StringBuilder();
        private boolean previousLineEndedWithSpace;

        Block(BigDecimal balance, String serialNumber, LocalDate date) {
            this.balance = balance;
            this.serialNumber = serialNumber;
            this.date = date;
        }

        /** Narration wraps mid-token (a UPI handle can split anywhere), so lines join directly; a trailing space marks a real word break. */
        void addNarration(String text, boolean endedWithSpace) {
            if (narration.length() > 0 && previousLineEndedWithSpace) {
                narration.append(' ');
            }
            narration.append(text);
            previousLineEndedWithSpace = endedWithSpace;
        }

        ParsedTransactionRow finish(String lastNarrationLine, String debit, String credit) {
            if (lastNarrationLine != null) {
                addNarration(lastNarrationLine.trim(), false);
            }
            boolean isDebit = !"-".equals(debit);
            BigDecimal amount = StatementParsingSupport.parseAmount(isDebit ? debit : credit);
            String description = narration.toString().trim();
            Matcher reference = UPI_REFERENCE.matcher(description);
            return new ParsedTransactionRow(
                    date, amount,
                    isDebit ? ParsedTransactionRow.DebitCredit.DEBIT : ParsedTransactionRow.DebitCredit.CREDIT,
                    description, reference.find() ? reference.group(1) : null, balance, "sr" + serialNumber);
        }
    }
}
