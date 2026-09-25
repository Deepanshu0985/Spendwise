package com.finance.application.merchant;

import com.finance.application.exception.ConflictException;
import com.finance.application.exception.NotFoundException;
import com.finance.domain.merchant.Merchant;
import com.finance.domain.merchant.MerchantNormalizer;
import com.finance.domain.merchant.MerchantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantNormalizer normalizer;

    public MerchantServiceImpl(MerchantRepository merchantRepository, MerchantNormalizer normalizer) {
        this.merchantRepository = merchantRepository;
        this.normalizer = normalizer;
    }

    @Override
    @Transactional
    public Merchant create(UUID userId, MerchantNameCommand command) {
        String normalizedKey = normalizer.normalize(command.canonicalName());
        if (merchantRepository.existsByUserIdAndNormalizedKey(userId, normalizedKey)) {
            throw new ConflictException("A merchant with an equivalent name already exists.");
        }
        Merchant merchant = new Merchant(userId, command.canonicalName(), normalizedKey);
        return merchantRepository.save(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Merchant> listForUser(UUID userId) {
        return merchantRepository.findByUserIdOrderByCanonicalName(userId);
    }

    @Override
    @Transactional
    public Merchant rename(UUID userId, UUID merchantId, MerchantNameCommand command) {
        Merchant merchant = merchantRepository.findByIdAndUserId(merchantId, userId)
                .orElseThrow(() -> new NotFoundException("Merchant not found."));

        String normalizedKey = normalizer.normalize(command.canonicalName());
        if (!normalizedKey.equals(merchant.getNormalizedKey())
                && merchantRepository.existsByUserIdAndNormalizedKey(userId, normalizedKey)) {
            throw new ConflictException("A merchant with an equivalent name already exists.");
        }

        merchant.rename(command.canonicalName(), normalizedKey);
        return merchantRepository.save(merchant);
    }
}
