package com.finance.ai;

import com.finance.domain.ai.KeywordCategoryRules;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordCategoryRulesTest {

    @Test
    void wellKnownBrandsMapToTheirCategoryWhateverTheHandleLooksLike() {
        assertThat(KeywordCategoryRules.categoryNameFor("zomatoltd32.rzp")).contains("Food & Dining");
        assertThat(KeywordCategoryRules.categoryNameFor("Paid to Swiggy Instamart")).contains("Groceries");
        assertThat(KeywordCategoryRules.categoryNameFor("Paid to Swiggy")).contains("Food & Dining");
        assertThat(KeywordCategoryRules.categoryNameFor("Netflix.com")).contains("Subscriptions");
        assertThat(KeywordCategoryRules.categoryNameFor("IRCTC e-ticket")).contains("Travel");
    }

    @Test
    void unknownPayeesAndPeopleGetNoRuleSoTheyStayForTheModelOrTheUser() {
        assertThat(KeywordCategoryRules.categoryNameFor("Paid to Yash Kanojiya")).isEmpty();
        assertThat(KeywordCategoryRules.categoryNameFor("Naeem Hair cut")).isEmpty();
        assertThat(KeywordCategoryRules.categoryNameFor((String) null)).isEmpty();
    }

    @Test
    void theFirstTextWithAMatchWins() {
        assertThat(KeywordCategoryRules.categoryNameFor("nothing here", "paid to Zepto")).contains("Groceries");
    }
}
