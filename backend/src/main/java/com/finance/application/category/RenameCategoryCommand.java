package com.finance.application.category;

/** Only the name is editable; categoryType stays fixed once set, same reasoning as accountType/currency on accounts. */
public record RenameCategoryCommand(String name) {
}
