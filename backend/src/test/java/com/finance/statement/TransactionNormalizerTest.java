package com.finance.statement;

import com.finance.domain.statement.ParsedTransactionRow;
import com.finance.domain.transaction.TransactionType;
import com.finance.infrastructure.pdf.NormalizedTransactionRow;
import com.finance.infrastructure.pdf.TransactionNormalizer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Framework-free, no Spring context needed - transaction-normalization.md's classification table, one case per row. */
class TransactionNormalizerTest {

    private final TransactionNormalizer normalizer = new TransactionNormalizer();

    @Test
    void atmWithdrawalIsCashWithdrawal() {
        NormalizedTransactionRow result = normalizer.normalize(row("ATM WDL CASH", ParsedTransactionRow.DebitCredit.DEBIT));
        assertThat(result.transactionType()).isEqualTo(TransactionType.CASH_WITHDRAWAL);
    }

    @Test
    void salaryCreditIsIncome() {
        NormalizedTransactionRow result = normalizer.normalize(row("SALARY CREDIT ACME CORP", ParsedTransactionRow.DebitCredit.CREDIT));
        assertThat(result.transactionType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void reversalCreditIsRefund() {
        NormalizedTransactionRow result = normalizer.normalize(row("REVERSAL OF FAILED TXN", ParsedTransactionRow.DebitCredit.CREDIT));
        assertThat(result.transactionType()).isEqualTo(TransactionType.REFUND);
    }

    @Test
    void interestDebitIsInterestCharged() {
        NormalizedTransactionRow result = normalizer.normalize(row("INTEREST CHARGED ON OUTSTANDING", ParsedTransactionRow.DebitCredit.DEBIT));
        assertThat(result.transactionType()).isEqualTo(TransactionType.INTEREST_CHARGED);
    }

    @Test
    void interestCreditIsInterestEarned() {
        NormalizedTransactionRow result = normalizer.normalize(row("INTEREST EARNED ON DEPOSIT", ParsedTransactionRow.DebitCredit.CREDIT));
        assertThat(result.transactionType()).isEqualTo(TransactionType.INTEREST_EARNED);
    }

    @Test
    void feeDebitIsFeeCharged() {
        NormalizedTransactionRow result = normalizer.normalize(row("ANNUAL FEE CHARGE", ParsedTransactionRow.DebitCredit.DEBIT));
        assertThat(result.transactionType()).isEqualTo(TransactionType.FEE_CHARGED);
    }

    @Test
    void realBankFeeAndChargeNarrationsAreStillFeeCharged() {
        for (String narration : new String[] {
                "SMS CHARGES", "SERVICE CHARGE GST", "NEFT CHARGES", "LATE FEE", "DEBIT CARD ANNUAL FEES", "AMC CHARGES FOR SAVINGS ACCOUNT",
                "MIN BAL CHARGES"}) {
            assertThat(normalizer.normalize(row(narration, ParsedTransactionRow.DebitCredit.DEBIT)).transactionType())
                    .as(narration).isEqualTo(TransactionType.FEE_CHARGED);
        }
    }

    @Test
    void aFeePaidToAnInstitutionIsAnExpenseNotABankFee() {
        // The word "fee" here is what the user paid a school or university, not something the bank charged.
        for (String narration : new String[] {
                "UNIVERSITY FEE PAYMENT", "TUITION FEE Q2", "SCHOOL FEES TERM 1", "COLLEGE EXAM FEE", "COACHING INSTITUTE FEE", "FASTAG TOLL FEE",
                "HOSPITAL REGISTRATION CHARGES"}) {
            assertThat(normalizer.normalize(row(narration, ParsedTransactionRow.DebitCredit.DEBIT)).transactionType())
                    .as(narration).isEqualTo(TransactionType.EXPENSE);
        }
    }

    @Test
    void wordsThatMerelyContainFeeOrChargeOrAtmAreNotClassifiedByThem() {
        // COFFEE contains FEE, TREATMENT contains ATM, SUPERCHARGED and DISCHARGE contain CHARGE - none of them mean what the rule means.
        for (String narration : new String[] {"COFFEE DAY OUTLET", "UPI/CAFFEE SHOP", "HOSPITAL TREATMENT", "ATMOSPHERE RESTAURANT", "SUPERCHARGED GYM", "DISCHARGE SUMMARY PHARMACY"}) {
            assertThat(normalizer.normalize(row(narration, ParsedTransactionRow.DebitCredit.DEBIT)).transactionType())
                    .as(narration).isEqualTo(TransactionType.EXPENSE);
        }
    }

    @Test
    void atmCashWithdrawalsAreStillRecognisedInTheirCommonForms() {
        for (String narration : new String[] {"ATM WDL CASH", "ATM/CASH WDL/123", "NFS ATM WITHDRAWAL", "CASH WITHDRAWAL ATM-HDFC"}) {
            assertThat(normalizer.normalize(row(narration, ParsedTransactionRow.DebitCredit.DEBIT)).transactionType())
                    .as(narration).isEqualTo(TransactionType.CASH_WITHDRAWAL);
        }
    }

    @Test
    void aMerchantNamedLikeSelfOrInterestIsNotATransferOrInterest() {
        assertThat(normalizer.normalize(row("SELFRIDGES ONLINE", ParsedTransactionRow.DebitCredit.DEBIT)).transactionType())
                .isEqualTo(TransactionType.EXPENSE);
        assertThat(normalizer.normalize(row("SELF TRANSFER TO SAVINGS", ParsedTransactionRow.DebitCredit.DEBIT)).transactionType())
                .isEqualTo(TransactionType.TRANSFER_OUT);
        assertThat(normalizer.normalize(row("INTERESTING BOOKS STORE", ParsedTransactionRow.DebitCredit.DEBIT)).transactionType())
                .isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void creditCardBillPaidFromABankAccountIsAPlainExpense() {
        NormalizedTransactionRow result = normalizer.normalize(row("CARD PAYMENT RECEIVED", ParsedTransactionRow.DebitCredit.DEBIT));
        assertThat(result.transactionType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void unmatchedDebitFallsBackToExpense() {
        NormalizedTransactionRow result = normalizer.normalize(row("SWIGGY BANGALORE", ParsedTransactionRow.DebitCredit.DEBIT));
        assertThat(result.transactionType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void unmatchedCreditFallsBackToIncome() {
        NormalizedTransactionRow result = normalizer.normalize(row("NEFT CR FROM CLIENT", ParsedTransactionRow.DebitCredit.CREDIT));
        assertThat(result.transactionType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void paymentPrefixIsStrippedFromNormalizedDescriptionButRawDescriptionIsUntouched() {
        ParsedTransactionRow row = new ParsedTransactionRow(
                LocalDate.of(2026, 9, 15), new BigDecimal("500.00"), ParsedTransactionRow.DebitCredit.DEBIT,
                "UPI/SWIGGY/12345", "12345", new BigDecimal("1000.00"), "p1r1");
        NormalizedTransactionRow result = normalizer.normalize(row);
        assertThat(result.normalizedDescription()).isEqualTo("SWIGGY/12345");
        assertThat(row.rawDescription()).isEqualTo("UPI/SWIGGY/12345");
    }

    @Test
    void wellFormedRowGetsHigherConfidenceThanAFallbackClassification() {
        NormalizedTransactionRow specific = normalizer.normalize(row("ATM WDL", ParsedTransactionRow.DebitCredit.DEBIT));
        NormalizedTransactionRow fallback = normalizer.normalize(row("RANDOM MERCHANT XYZ", ParsedTransactionRow.DebitCredit.DEBIT));
        assertThat(specific.confidenceScore()).isGreaterThan(fallback.confidenceScore());
    }

    private ParsedTransactionRow row(String description, ParsedTransactionRow.DebitCredit debitCredit) {
        return new ParsedTransactionRow(
                LocalDate.of(2026, 9, 15), new BigDecimal("500.00"), debitCredit, description, "REF1",
                new BigDecimal("1000.00"), "p1r1");
    }
}
