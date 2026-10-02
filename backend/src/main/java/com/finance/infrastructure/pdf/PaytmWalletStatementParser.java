package com.finance.infrastructure.pdf;

import com.finance.domain.statement.ParsedStatement;
import com.finance.domain.statement.ParsedTransactionRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Paytm's real UPI "Passbook Payments History" export - confirmed against an
 * actual statement, not guessed. Unlike every bank statement (one row per
 * line), each transaction here is a multi-line block once PDFBox extracts the
 * table's columns in reading order:
 *
 * <pre>
 * 29 Sep
 * 10:05 PM
 * Paid to Rana Confectionery And
 * Grocery Store
 * UPI ID: gpay-11259061333@okbizaxis
 * UPI Ref No: 615023242614
 * Tag:
 * # Groceries
 * Ujjivan Small
 * Finance
 * Bank - 82
 * - Rs.110
 * </pre>
 *
 * so this parser is a small line-by-line state machine, not a single regex.
 * Dates carry no year - it's read from the statement's own "DD Mon'YY - DD
 * Mon'YY" header line instead. A block with no leading +/- sign on its amount
 * (e.g. "Automatic Add Money for UPI Lite", "Transferred to Self, ...") is a
 * self-transfer between the user's own linked accounts - Paytm's own header
 * says these aren't counted in the statement's totals, and this parser skips
 * them the same way rather than mis-recording them as income or an expense.
 */
@Component
public class PaytmWalletStatementParser implements StatementParser {

    private static final Pattern PERIOD_HEADER = Pattern.compile(
            "(\\d{1,2})\\s+([A-Za-z]{3})'(\\d{2})\\s*-\\s*(\\d{1,2})\\s+([A-Za-z]{3})'(\\d{2})");
    private static final Pattern DATE_LINE = Pattern.compile("^(\\d{1,2})\\s+([A-Za-z]{3})$");
    private static final Pattern TIME_LINE = Pattern.compile("^\\d{1,2}:\\d{2}\\s*(AM|PM)$");
    private static final Pattern UPI_ID_LINE = Pattern.compile("^UPI ID:");
    private static final Pattern UPI_REF_LINE = Pattern.compile("^UPI Ref No:\\s*(\\S+)");
    private static final Pattern NOTE_LINE = Pattern.compile("^Note:");
    private static final Pattern TAG_LINE = Pattern.compile("^Tag:$");
    private static final Pattern CATEGORY_LINE = Pattern.compile("^#\\s*(.+)$");
    private static final Pattern AMOUNT_LINE = Pattern.compile("([+-])?\\s*Rs\\.([\\d,]+(?:\\.\\d+)?)\\s*$");

    private enum State { IDLE, EXPECT_TIME, COLLECT_DESCRIPTION, AFTER_UPI_ID, AFTER_REF, IN_NOTE, EXPECT_CATEGORY, COLLECT_ACCOUNT }

    @Override
    public String bankName() {
        return "PAYTM_WALLET";
    }

    @Override
    public boolean matches(String extractedText) {
        // A bare "PAYTM" substring isn't specific enough: a real Bank of Baroda
        // statement's own UPI narrations routinely mention Paytm-linked merchant
        // handles ("UPI/.../paytm.d11487.../...", "paytmqr...") without the statement
        // being a Paytm export at all - found against a real BOB file, where this
        // false-matched and won the format-detection race before BobStatementParser
        // was ever tried. "Passbook Payments History" is Paytm's own literal section
        // title and reliably specific to its actual export.
        return extractedText.toUpperCase().contains("PASSBOOK PAYMENTS HISTORY");
    }

    @Override
    public ParsedStatement parse(String extractedText) {
        String[] lines = extractedText.split("\\R");
        int[] periodYears = periodYears(extractedText);
        LocalDate fallbackStart = LocalDate.of(2000 + periodYears[0], 1, 1);
        LocalDate fallbackEnd = LocalDate.of(2000 + periodYears[1], 12, 31);

        List<ParsedTransactionRow> rows = new ArrayList<>();
        State state = State.IDLE;
        int day = 0;
        Month month = null;
        StringBuilder description = new StringBuilder();
        String reference = null;
        int rowIndex = 0;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher dateMatcher = DATE_LINE.matcher(line);
            if (state == State.IDLE && dateMatcher.matches()) {
                day = Integer.parseInt(dateMatcher.group(1));
                month = parseMonth(dateMatcher.group(2));
                state = month == null ? State.IDLE : State.EXPECT_TIME;
                continue;
            }
            switch (state) {
                case EXPECT_TIME -> {
                    if (TIME_LINE.matcher(line).matches()) {
                        description.setLength(0);
                        reference = null;
                        state = State.COLLECT_DESCRIPTION;
                    } else {
                        state = State.IDLE;
                    }
                }
                case COLLECT_DESCRIPTION -> {
                    // "UPI Ref No:" is checked first and independently of "UPI ID:" - a handful of
                    // entries (e.g. "Automatic Add Money for UPI Lite") have no UPI ID line at all
                    // and go straight from description to the ref number.
                    Matcher directRefMatcher = UPI_REF_LINE.matcher(line);
                    if (directRefMatcher.find()) {
                        reference = directRefMatcher.group(1);
                        state = State.AFTER_REF;
                    } else if (UPI_ID_LINE.matcher(line).find()) {
                        state = State.AFTER_UPI_ID;
                    } else {
                        if (description.length() > 0) {
                            description.append(' ');
                        }
                        description.append(line);
                    }
                }
                case AFTER_UPI_ID -> {
                    Matcher refMatcher = UPI_REF_LINE.matcher(line);
                    if (refMatcher.find()) {
                        reference = refMatcher.group(1);
                        state = State.AFTER_REF;
                    }
                    // otherwise still inside a wrapped UPI ID (e.g. split across two lines) - skip
                }
                case AFTER_REF -> {
                    if (NOTE_LINE.matcher(line).find()) {
                        state = State.IN_NOTE;
                    } else if (TAG_LINE.matcher(line).matches()) {
                        state = State.EXPECT_CATEGORY;
                    }
                }
                case IN_NOTE -> {
                    if (TAG_LINE.matcher(line).matches()) {
                        state = State.EXPECT_CATEGORY;
                    }
                    // otherwise still inside a (possibly multi-line) Note: block - skip
                }
                case EXPECT_CATEGORY -> {
                    if (CATEGORY_LINE.matcher(line).matches()) {
                        state = State.COLLECT_ACCOUNT;
                    }
                }
                case COLLECT_ACCOUNT -> {
                    // A short account name (e.g. "UPI Lite") shares its line with the amount
                    // instead of wrapping onto its own line the way "Ujjivan Small Finance Bank
                    // - 82" does, so this looks for the amount at the END of the line rather
                    // than requiring the whole line to be just the amount.
                    Matcher amountMatcher = AMOUNT_LINE.matcher(line);
                    if (amountMatcher.find()) {
                        rowIndex++;
                        String sign = amountMatcher.group(1);
                        if (sign != null) {
                            LocalDate date = resolveYear(day, month, periodYears, fallbackStart, fallbackEnd);
                            BigDecimal amount = StatementParsingSupport.parseAmount(amountMatcher.group(2));
                            ParsedTransactionRow.DebitCredit debitCredit =
                                    "-".equals(sign) ? ParsedTransactionRow.DebitCredit.DEBIT : ParsedTransactionRow.DebitCredit.CREDIT;
                            if (amount.signum() > 0) {
                                rows.add(new ParsedTransactionRow(
                                        date, amount, debitCredit, description.toString().trim(), reference, null, "p1r" + rowIndex));
                            }
                        }
                        // sign == null: a self-transfer between the user's own accounts (Paytm's own
                        // statement note says these are excluded from totals) - not staged at all.
                        state = State.IDLE;
                    }
                    // otherwise still inside the (possibly multi-line, wrapped) account name - skip
                }
                default -> {
                    // IDLE with no date match: header/footer noise between transaction blocks - skip
                }
            }
        }

        LocalDate periodStart = rows.stream().map(ParsedTransactionRow::transactionDate).min(Comparator.naturalOrder()).orElse(null);
        LocalDate periodEnd = rows.stream().map(ParsedTransactionRow::transactionDate).max(Comparator.naturalOrder()).orElse(null);
        return new ParsedStatement(bankName(), periodStart, periodEnd, rows);
    }

    /** Reads the "DD Mon'YY - DD Mon'YY" header; falls back to the current year for both ends if it's missing or unparseable. */
    private int[] periodYears(String extractedText) {
        Matcher matcher = PERIOD_HEADER.matcher(extractedText);
        if (matcher.find()) {
            return new int[] {Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(6))};
        }
        int currentYear = LocalDate.now().getYear() % 100;
        return new int[] {currentYear, currentYear};
    }

    /** Most statements don't cross a year boundary; when the header's two years differ, a date in the later months uses the start year. */
    private LocalDate resolveYear(int day, Month month, int[] periodYears, LocalDate fallbackStart, LocalDate fallbackEnd) {
        int startYear = 2000 + periodYears[0];
        int endYear = 2000 + periodYears[1];
        if (startYear == endYear) {
            return LocalDate.of(startYear, month, day);
        }
        LocalDate withStartYear = LocalDate.of(startYear, month, day);
        return !withStartYear.isBefore(fallbackStart) && !withStartYear.isAfter(fallbackEnd)
                ? withStartYear
                : LocalDate.of(endYear, month, day);
    }

    private Month parseMonth(String abbreviation) {
        String name = switch (abbreviation.toUpperCase(Locale.ROOT)) {
            case "JAN" -> "JANUARY";
            case "FEB" -> "FEBRUARY";
            case "MAR" -> "MARCH";
            case "APR" -> "APRIL";
            case "MAY" -> "MAY";
            case "JUN" -> "JUNE";
            case "JUL" -> "JULY";
            case "AUG" -> "AUGUST";
            case "SEP" -> "SEPTEMBER";
            case "OCT" -> "OCTOBER";
            case "NOV" -> "NOVEMBER";
            case "DEC" -> "DECEMBER";
            default -> null;
        };
        try {
            return name == null ? null : Month.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
