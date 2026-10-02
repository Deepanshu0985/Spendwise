package com.finance.infrastructure.pdf;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bank of Baroda UPI narrations look like {@code UPI/<12-digit ref>/<hh:mm:ss>/UPI/<handle>@<bank>/<truncated note>}.
 * The ref, time, bank suffix and note are noise to the user; the handle is the only part
 * that says who was paid; MerchantNameResolver then names it where it's a known brand.
 */
@Component
public class BobDescriptionCleaner implements DescriptionCleaner {

    private final MerchantNameResolver merchantNameResolver;

    public BobDescriptionCleaner(MerchantNameResolver merchantNameResolver) {
        this.merchantNameResolver = merchantNameResolver;
    }

    private static final Pattern UPI_NARRATION = Pattern.compile("^UPI/\\d+/\\d{2}:\\d{2}:\\d{2}/UPI/(.+)$");

    @Override
    public String bankName() {
        return "BOB";
    }

    @Override
    public Optional<String> clean(String rawDescription) {
        Matcher matcher = UPI_NARRATION.matcher(rawDescription.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String payee = matcher.group(1);
        int at = payee.indexOf('@');
        String handle = at >= 0 ? payee.substring(0, at) : payee.replace('/', ' ');
        if (handle.isBlank()) {
            return Optional.empty();
        }
        return Optional.of("UPI: " + merchantNameResolver.resolve(handle).orElse(handle.trim()));
    }
}
