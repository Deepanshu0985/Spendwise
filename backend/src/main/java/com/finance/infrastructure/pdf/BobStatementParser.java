package com.finance.infrastructure.pdf;

import com.finance.domain.statement.ParsedStatement;
import com.finance.domain.statement.ParsedTransactionRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bank of Baroda's real statement export (confirmed against an actual
 * statement, not guessed) - not the single-line six-column shape originally
 * assumed. Each transaction is a date line, one or more wrapped narration
 * lines, then a trailing "amount runningBalance Cr/Dr" line:
 *
 * <pre>
 * 01-07-2026
 * UPI/209737101589/14:14:17/UPI/7302904765@
 * ptyes/NA
 * 15000.00 15078.62 Cr
 * </pre>
 *
 * or, when the narration is short enough to fit, all on one line:
 * {@code 05-07-2026 ACHDR/EMIDUE/3929525584/111397544160 5556.00 442.04 Cr}.
 * There is no separate debit/credit column in the extracted text - only one
 * amount and the running balance - so debit vs. credit is inferred by
 * comparing the running balance to the previous one, seeded from the
 * statement's own "Opening Balance" line. "Closing Balance" is a checkpoint
 * too, not a transaction; both are skipped as rows.
 */
@Component
public class BobStatementParser implements StatementParser {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final Pattern DATE_LINE = Pattern.compile("^(\\d{2}-\\d{2}-\\d{4})$");
    private static final Pattern OPENING_OR_CLOSING_BALANCE_LINE = Pattern.compile(
            "^\\d{2}-\\d{2}-\\d{4}\\s+(?:Opening|Closing) Balance\\s+([\\d,]+\\.\\d{2})\\s+(?:Cr|Dr)\\s*$");
    private static final Pattern SINGLE_LINE_ROW = Pattern.compile(
            "^(\\d{2}-\\d{2}-\\d{4})\\s+(.+?)\\s+([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})\\s+(?:Cr|Dr)\\s*$");
    private static final Pattern BALANCE_LINE = Pattern.compile(
            "^([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})\\s+(?:Cr|Dr)\\s*$");

    // The UPI RRN is globally unique, so it also matches the same payment in another account's statement (e.g. Paytm's "UPI Ref No").
    private static final Pattern UPI_REFERENCE = Pattern.compile("^UPI/(\\d{9,})/");

    private enum State { IDLE, COLLECT_NARRATION }

    @Override
    public String bankName() {
        return "BOB";
    }

    @Override
    public String displayName() {
        return "Bank of Baroda";
    }

    @Override
    public int identityPosition(String extractedText) {
        return extractedText.toUpperCase().indexOf("BANKOFBARODA");
    }

    @Override
    public boolean matches(String extractedText) {
        String upper = extractedText.toUpperCase();
        // The real statement never actually spells out "Bank of Baroda" as a phrase
        // (confirmed against an actual statement) - every page's footer instead prints
        // the bank's URL as one unspaced word, "bankofbaroda.bank.in", so that's the
        // reliable signal, not the human-readable name. Checking the name alone still
        // wouldn't be enough even if it did appear: Paytm's own passbook statement
        // labels a row's linked account "Bank Of Baroda" without being a BOB statement
        // itself, so an actual matching row must be present too (found against a real
        // Paytm file).
        return upper.contains("BANKOFBARODA")
                && extractedText.lines().anyMatch(line -> BALANCE_LINE.matcher(line.trim()).matches());
    }

    @Override
    public ParsedStatement parse(String extractedText) {
        List<ParsedTransactionRow> rows = new ArrayList<>();
        State state = State.IDLE;
        LocalDate pendingDate = null;
        StringBuilder narration = new StringBuilder();
        boolean previousLineEndedWithSpace = false;
        BigDecimal previousBalance = null;
        int rowIndex = 0;

        for (String rawLine : extractedText.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (state == State.IDLE) {
                Matcher openingClosing = OPENING_OR_CLOSING_BALANCE_LINE.matcher(line);
                if (openingClosing.matches()) {
                    previousBalance = StatementParsingSupport.parseAmount(openingClosing.group(1));
                    continue;
                }
                Matcher singleLine = SINGLE_LINE_ROW.matcher(line);
                if (singleLine.matches()) {
                    rowIndex++;
                    LocalDate date = StatementParsingSupport.parseDate(singleLine.group(1), DATE_FORMAT);
                    BigDecimal amount = StatementParsingSupport.parseAmount(singleLine.group(3));
                    BigDecimal balance = StatementParsingSupport.parseAmount(singleLine.group(4));
                    rows.add(buildRow(date, singleLine.group(2).trim(), amount, balance, previousBalance, rowIndex));
                    previousBalance = balance;
                    continue;
                }
                Matcher dateOnly = DATE_LINE.matcher(line);
                if (dateOnly.matches()) {
                    pendingDate = StatementParsingSupport.parseDate(dateOnly.group(1), DATE_FORMAT);
                    narration.setLength(0);
                    previousLineEndedWithSpace = false;
                    state = State.COLLECT_NARRATION;
                }
                // else: page-break/header noise between transactions - skip
            } else {
                Matcher balanceMatcher = BALANCE_LINE.matcher(line);
                if (balanceMatcher.matches()) {
                    rowIndex++;
                    BigDecimal amount = StatementParsingSupport.parseAmount(balanceMatcher.group(1));
                    BigDecimal balance = StatementParsingSupport.parseAmount(balanceMatcher.group(2));
                    rows.add(buildRow(pendingDate, narration.toString().trim(), amount, balance, previousBalance, rowIndex));
                    previousBalance = balance;
                    state = State.IDLE;
                } else {
                    // Narration wraps mid-token (a UPI handle can split anywhere), so lines are
                    // joined directly; PDFBox keeps a trailing space only where the wrap fell
                    // on a real word break ("...PANGLOSS " / "REIMBURSEMENT MGMT").
                    if (narration.length() > 0 && previousLineEndedWithSpace) {
                        narration.append(' ');
                    }
                    narration.append(line);
                    previousLineEndedWithSpace = rawLine.endsWith(" ");
                }
            }
        }

        LocalDate periodStart = rows.stream().map(ParsedTransactionRow::transactionDate).min(Comparator.naturalOrder()).orElse(null);
        LocalDate periodEnd = rows.stream().map(ParsedTransactionRow::transactionDate).max(Comparator.naturalOrder()).orElse(null);
        return new ParsedStatement(bankName(), periodStart, periodEnd, rows);
    }

    private ParsedTransactionRow buildRow(
            LocalDate date, String description, BigDecimal amount, BigDecimal balance, BigDecimal previousBalance, int rowIndex) {
        boolean isCredit = previousBalance != null && balance.compareTo(previousBalance) > 0;
        var debitCredit = isCredit ? ParsedTransactionRow.DebitCredit.CREDIT : ParsedTransactionRow.DebitCredit.DEBIT;
        Matcher reference = UPI_REFERENCE.matcher(description);
        return new ParsedTransactionRow(
                date, amount, debitCredit, description, reference.find() ? reference.group(1) : null, balance, "p1r" + rowIndex);
    }
}
