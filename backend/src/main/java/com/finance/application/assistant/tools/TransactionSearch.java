package com.finance.application.assistant.tools;

import com.finance.application.assistant.FuzzyMatcher;
import com.finance.application.assistant.ToolArgumentException;
import com.finance.application.assistant.ToolArguments;
import com.finance.application.assistant.ToolResult;
import com.finance.domain.category.Category;
import com.finance.domain.category.CategoryRepository;
import com.finance.domain.merchant.Merchant;
import com.finance.domain.merchant.MerchantRepository;
import com.finance.domain.transaction.Transaction;
import com.finance.domain.transaction.TransactionLookup;
import com.finance.domain.transaction.TransactionRepository;
import com.finance.domain.transaction.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The shared engine behind the three search tools: turns a model's (already schema-checked) arguments into a repository
 * lookup, runs it as the session user, and names the results. Nothing here accepts a user id from the model.
 */
@Component
public class TransactionSearch {

    /** More than this many matches cannot be totalled reliably in one call; the request is rejected, never silently cut. */
    static final int MAX_ROWS_TO_AGGREGATE = 20_000;
    private static final int FUZZY_CANDIDATES = 2_000;
    private static final int MAX_WORDS = 6;

    private final TransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;
    private final CategoryRepository categoryRepository;
    private final Clock clock;

    public TransactionSearch(
            TransactionRepository transactionRepository, MerchantRepository merchantRepository, CategoryRepository categoryRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.merchantRepository = merchantRepository;
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    /** The user's merchants and categories by id, for naming results and resolving names in filters. */
    public record Directory(Map<UUID, String> merchantNames, List<Category> categories) {

        String merchantName(UUID id) {
            return id == null ? null : merchantNames.get(id);
        }

        String categoryName(UUID id) {
            return id == null ? null : categories.stream().filter(c -> c.getId().equals(id)).map(Category::getName).findFirst().orElse(null);
        }
    }

    public record Filters(
            LocalDate from, LocalDate to, BigDecimal minAmount, BigDecimal maxAmount, TransactionType type, UUID categoryId, List<String> words) {
    }

    public Directory directory(UUID userId) {
        Map<UUID, String> merchants = merchantRepository.findByUserIdOrderByCanonicalName(userId).stream()
                .collect(Collectors.toMap(Merchant::getId, Merchant::getCanonicalName));
        return new Directory(merchants, categoryRepository.findVisibleForUser(userId).stream().filter(Category::isActive).toList());
    }

    /**
     * Reads the filter arguments every search tool shares. The category must be one of the user's own (an unknown name is
     * an error that lists the real ones).
     */
    public Filters parseFilters(ToolArguments args, Directory directory, LocalDate[] period, TransactionType defaultType) {
        BigDecimal min = args.optionalAmount("minAmount");
        BigDecimal max = args.optionalAmount("maxAmount");
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new ToolArgumentException("'minAmount' must not be more than 'maxAmount'.");
        }
        TransactionType type = args.optionalEnum("type", TransactionType.class);
        if (type == null) {
            type = defaultType;
        }
        UUID categoryId = null;
        String category = args.optionalText("category", 100);
        if (category != null) {
            Category match = directory.categories().stream().filter(c -> c.getName().equalsIgnoreCase(category)).findFirst().orElse(null);
            if (match == null) {
                String available = directory.categories().stream().map(c -> UntrustedText.of(c.getName())).sorted().collect(Collectors.joining(", "));
                throw new ToolArgumentException("No category named '" + UntrustedText.of(category) + "'. Available categories: " + available + ".");
            }
            categoryId = match.getId();
        }
        List<String> words = new ArrayList<>(requiredWords(args.optionalText("text", 200), "text"));
        words.addAll(requiredWords(args.optionalText("merchant", 100), "merchant"));
        if (words.size() > MAX_WORDS) {
            throw new ToolArgumentException("Too many search words; use at most " + MAX_WORDS + ".");
        }
        return new Filters(period[0], period[1], min, max, type, categoryId, words);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * The words of a text filter. A filter that was given but contains no usable word (only punctuation such as "%", or single
     * letters) is an error, not "no filter" - otherwise asking for a nonsense word would quietly return every transaction.
     */
    private static List<String> requiredWords(String text, String field) {
        List<String> found = words(text);
        if (text != null && found.isEmpty()) {
            throw new ToolArgumentException("'" + field + "' needs at least one word of two or more letters or digits.");
        }
        return found;
    }

    private static List<String> words(String text) {
        if (text == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{Nd}]+")) {
            if (word.length() >= 2 && word.length() <= 40) {
                out.add(word);
            }
        }
        return out;
    }

    private TransactionLookup toLookup(Filters f, Directory directory, boolean includeWords) {
        List<TransactionLookup.TextMatcher> matchers = new ArrayList<>();
        if (includeWords) {
            for (String word : f.words()) {
                Set<UUID> merchantIds = directory.merchantNames().entrySet().stream()
                        .filter(entry -> entry.getValue().toLowerCase(Locale.ROOT).contains(word)).map(Map.Entry::getKey).collect(Collectors.toSet());
                matchers.add(new TransactionLookup.TextMatcher(word, merchantIds));
            }
        }
        return new TransactionLookup(f.from(), f.to(), f.minAmount(), f.maxAmount(), f.type(), f.categoryId(), null, matchers);
    }

    public record Found(List<Transaction> rows, long totalMatches, boolean closeMatchesOnly) {
    }

    /** At most limit rows in the order asked for, the true number of matches, and whether they are typo-tolerant guesses. */
    public Found search(UUID userId, Filters f, Directory directory, TransactionLookup.Sort sort, int limit) {
        TransactionLookup exact = toLookup(f, directory, true);
        long total = transactionRepository.countLookup(userId, exact);
        if (total > 0 || f.words().isEmpty()) {
            return new Found(transactionRepository.lookup(userId, exact, sort, limit), total, false);
        }
        // Nothing matched as typed: look for close spellings among the most recent rows that satisfy the other filters.
        List<Transaction> candidates = transactionRepository.lookup(userId, toLookup(f, directory, false), TransactionLookup.Sort.DATE_DESC, FUZZY_CANDIDATES);
        List<Transaction> close = candidates.stream().filter(t -> matchesAllWords(t, f.words(), directory)).sorted(comparator(sort)).toList();
        return new Found(close.stream().limit(limit).toList(), close.size(), true);
    }

    /** Every matching transaction, or a refusal if there are too many to total reliably. */
    public List<Transaction> all(UUID userId, Filters f, Directory directory) {
        List<Transaction> rows = transactionRepository.lookup(userId, toLookup(f, directory, true), TransactionLookup.Sort.DATE_DESC, MAX_ROWS_TO_AGGREGATE + 1);
        if (rows.size() > MAX_ROWS_TO_AGGREGATE) {
            throw new ToolArgumentException("Too many transactions match to total reliably; narrow the period or add a filter.");
        }
        return rows;
    }

    private boolean matchesAllWords(Transaction t, List<String> words, Directory directory) {
        String haystack = String.join(" ",
                t.getDescription() == null ? "" : t.getDescription(), t.getRawDescription() == null ? "" : t.getRawDescription(),
                directory.merchantName(t.getMerchantId()) == null ? "" : directory.merchantName(t.getMerchantId()));
        String lower = haystack.toLowerCase(Locale.ROOT);
        return words.stream().allMatch(word -> lower.contains(word) || FuzzyMatcher.isClose(word, haystack));
    }

    private static Comparator<Transaction> comparator(TransactionLookup.Sort sort) {
        return switch (sort) {
            case DATE_DESC -> Comparator.comparing(Transaction::getTransactionDate).reversed();
            case DATE_ASC -> Comparator.comparing(Transaction::getTransactionDate);
            case AMOUNT_DESC -> Comparator.comparing(Transaction::getAmount).reversed();
            case AMOUNT_ASC -> Comparator.comparing(Transaction::getAmount);
        };
    }

    /**
     * One row as shown to the user: their own text, with control characters stripped and a length cap. (What the model sees is
     * passed through UntrustedText separately.) The record id stays server-side.
     */
    public ToolResult.SourceRow source(Transaction t, Directory directory) {
        String merchant = directory.merchantName(t.getMerchantId());
        return new ToolResult.SourceRow(
                t.getId(), t.getTransactionDate(), tidy(display(t)), merchant == null ? null : tidy(merchant),
                ToolOutput.money(t.getAmount()), t.getCurrency(), t.getTransactionType().name());
    }

    private static String tidy(String text) {
        String cleaned = text.replaceAll("\\p{Cntrl}", " ").trim();
        return cleaned.length() > 200 ? cleaned.substring(0, 200) + "…" : cleaned;
    }

    static String display(Transaction t) {
        return t.getDescription() != null && !t.getDescription().isBlank() ? t.getDescription() : (t.getRawDescription() == null ? "" : t.getRawDescription());
    }

    static Map<String, List<Transaction>> byCurrency(List<Transaction> rows) {
        Map<String, List<Transaction>> map = new java.util.TreeMap<>();
        for (Transaction row : rows) {
            map.computeIfAbsent(row.getCurrency(), c -> new ArrayList<>()).add(row);
        }
        return map;
    }
}
