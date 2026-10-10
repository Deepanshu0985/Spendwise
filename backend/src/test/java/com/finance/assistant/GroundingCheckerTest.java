package com.finance.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.application.assistant.GroundingChecker;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GroundingCheckerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private Set<BigDecimal> allowedFrom(String... toolJson) throws Exception {
        List<JsonNode> nodes = new java.util.ArrayList<>();
        for (String json : toolJson) {
            nodes.add(mapper.readTree(json));
        }
        return GroundingChecker.numbersIn(nodes);
    }

    @Test
    void figuresThatAToolReturnedAreGroundedHoweverTheyAreFormatted() throws Exception {
        Set<BigDecimal> allowed = allowedFrom("{\"expenses\":33093.00,\"income\":83333.00,\"savingsRate\":60.3}");

        assertThat(GroundingChecker.ungrounded("You spent ₹33,093.00 and earned Rs. 83,333 in October.", allowed)).isEmpty();
        assertThat(GroundingChecker.ungrounded("Your savings rate was 60.3%.", allowed)).isEmpty();
        assertThat(GroundingChecker.ungrounded("Expenses: INR 33093", allowed)).isEmpty();
    }

    @Test
    void aFigureNoToolReturnedIsReported() throws Exception {
        Set<BigDecimal> allowed = allowedFrom("{\"expenses\":33093.00}");

        assertThat(GroundingChecker.ungrounded("You spent ₹34,500 this month.", allowed)).containsExactly("₹34,500");
        assertThat(GroundingChecker.ungrounded("That is up 15% on last month.", allowed)).containsExactly("15%");
        assertThat(GroundingChecker.ungrounded("About 12,345.67 went on food.", allowed)).hasSize(1);
    }

    @Test
    void aCurrencyWrittenAfterTheNumberStillMarksItAsAFigureToCheck() throws Exception {
        // Found with the real model: "You spent 400 INR less" slipped through because only a leading marker counted.
        Set<BigDecimal> allowed = allowedFrom("{\"august\":19649.00,\"september\":19249.00}");

        assertThat(GroundingChecker.ungrounded("You spent 400 INR less in September.", allowed)).hasSize(1);
        assertThat(GroundingChecker.ungrounded("That is 400 rupees less.", allowed)).hasSize(1);
        assertThat(GroundingChecker.ungrounded("That is 400 Rs less.", allowed)).hasSize(1);
        assertThat(GroundingChecker.ungrounded("In August you spent 19,649 INR and in September 19,249 INR.", allowed)).isEmpty();
    }

    @Test
    void anAmountRightAfterAMonthNameIsNotMistakenForADate() throws Exception {
        Set<BigDecimal> allowed = allowedFrom("{\"september\":19249.00}");

        assertThat(GroundingChecker.ungrounded("September 19,249 INR", allowed)).isEmpty();
        assertThat(GroundingChecker.ungrounded("September 19,999 INR", allowed)).hasSize(1);
        assertThat(GroundingChecker.ungrounded("Oct 5, 2026 and Oct 5 and October 2026 are dates", allowed)).isEmpty();
    }

    @Test
    void aDerivedFigureIsNotGroundedEvenIfTheArithmeticIsRight() throws Exception {
        // The model may not calculate: 83,333 - 33,093 = 50,240, but 50,240 was not returned by a tool.
        Set<BigDecimal> allowed = allowedFrom("{\"expenses\":33093.00,\"income\":83333.00}");

        assertThat(GroundingChecker.ungrounded("You saved ₹50,240.", allowed)).containsExactly("₹50,240");
    }

    @Test
    void aWholeNumberMayBeAToolValueRoundedToTheNearestUnit() throws Exception {
        Set<BigDecimal> allowed = allowedFrom("{\"expenses\":12345.67}");

        assertThat(GroundingChecker.ungrounded("About ₹12,346 went out.", allowed)).isEmpty();
        assertThat(GroundingChecker.ungrounded("About ₹12,400 went out.", allowed)).hasSize(1);
        // but a figure written with decimals must match exactly
        assertThat(GroundingChecker.ungrounded("Exactly ₹12,345.70 went out.", allowed)).hasSize(1);
    }

    @Test
    void datesYearsListNumbersAndSmallCountsAreNotTreatedAsFigures() throws Exception {
        Set<BigDecimal> allowed = allowedFrom("{\"expenses\":500.00}");
        String answer = "Between 2026-10-01 and 31 Oct 2026 (October 2026):\n1. You spent ₹500.00\n2. across 3 categories, in 2026, on 5th Oct.";

        assertThat(GroundingChecker.ungrounded(answer, allowed)).isEmpty();
    }

    @Test
    void digitsInsideDatesOrNamesInAToolResultDoNotMakeAFigureLegitimate() throws Exception {
        // 2026 and 10 appear only inside strings; they must not authorise "₹2,026" or "₹10.00".
        Set<BigDecimal> allowed = allowedFrom("{\"period\":{\"from\":\"2026-10-01\"},\"merchant\":\"Shop 4242\",\"amount\":500.00}");

        assertThat(GroundingChecker.ungrounded("You paid ₹10.00 and ₹4,242.", allowed)).hasSize(2);
        assertThat(GroundingChecker.ungrounded("You paid ₹500.00.", allowed)).isEmpty();
    }

    @Test
    void aReferenceNumberQuotedFromADescriptionIsFineButItNeverAuthorisesAnAmount() throws Exception {
        List<JsonNode> results = List.of(mapper.readTree("{\"description\":\"UPI 615023242614 Order 48213\",\"amount\":500.00}"));
        Set<BigDecimal> allowed = GroundingChecker.numbersIn(results);
        Set<String> quoted = GroundingChecker.textNumbersIn(results);

        // quoting the reference as text passes...
        assertThat(GroundingChecker.ungrounded("It was payment UPI 615023242614 (order 48213) for ₹500.00.", allowed, quoted)).isEmpty();
        // ...but the same digits as money do not, and neither does a number that is in no tool result at all
        assertThat(GroundingChecker.ungrounded("You paid ₹48,213.", allowed, quoted)).hasSize(1);
        assertThat(GroundingChecker.ungrounded("You paid 48213 INR.", allowed, quoted)).hasSize(1);
        assertThat(GroundingChecker.ungrounded("Reference 999888777666 was used.", allowed, quoted)).hasSize(1);
    }

    @Test
    void aFigureTheUserTypedMayBeRepeatedBackButNothingElseGetsThrough() throws Exception {
        Set<BigDecimal> allowed = new java.util.HashSet<>(allowedFrom("{\"amount\":2000.00}"));
        allowed.addAll(GroundingChecker.numbersInText("List all payments over 1,500 INR, and anything above 75000"));

        assertThat(GroundingChecker.ungrounded("Payments over ₹1,500: one for ₹2,000.00.", allowed)).isEmpty();
        assertThat(GroundingChecker.ungrounded("Above 75000 there was nothing.", allowed)).isEmpty();
        assertThat(GroundingChecker.ungrounded("Payments over ₹1,500: one for ₹2,100.00.", allowed)).hasSize(1);
        assertThat(GroundingChecker.numbersInText("no numbers here")).isEmpty();
        assertThat(GroundingChecker.numbersInText(null)).isEmpty();
    }

    @Test
    void negativeToolValuesGroundTheirAbsoluteFigure() throws Exception {
        Set<BigDecimal> allowed = allowedFrom("{\"remaining\":-1500.00}");

        assertThat(GroundingChecker.ungrounded("You are ₹1,500.00 over budget.", allowed)).isEmpty();
    }

    @Test
    void withNoToolResultsAnyFigureIsUngrounded() {
        assertThat(GroundingChecker.ungrounded("You probably spend ₹20,000 a month.", Set.of())).hasSize(1);
        assertThat(GroundingChecker.ungrounded("I can only answer questions about your own transactions.", Set.of())).isEmpty();
    }

    @Test
    void anAnswerWithNoFiguresIsAlwaysGrounded() throws Exception {
        assertThat(GroundingChecker.ungrounded("Your top merchant was Zomato, followed by Swiggy.", allowedFrom("{\"x\":1}"))).isEmpty();
    }
}
