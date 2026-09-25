package com.finance.application.merchant;

import com.finance.domain.merchant.Merchant;

import java.util.List;
import java.util.UUID;

public interface MerchantService {

    Merchant create(UUID userId, MerchantNameCommand command);

    List<Merchant> listForUser(UUID userId);

    Merchant rename(UUID userId, UUID merchantId, MerchantNameCommand command);
}
