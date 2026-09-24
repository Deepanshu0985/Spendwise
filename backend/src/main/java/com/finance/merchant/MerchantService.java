package com.finance.merchant;

import java.util.List;
import java.util.UUID;

public interface MerchantService {

    Merchant create(UUID userId, MerchantNameRequest request);

    List<Merchant> listForUser(UUID userId);

    Merchant rename(UUID userId, UUID merchantId, MerchantNameRequest request);
}
