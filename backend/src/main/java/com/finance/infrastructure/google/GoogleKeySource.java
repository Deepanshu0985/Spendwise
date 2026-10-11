package com.finance.infrastructure.google;

import java.security.PublicKey;
import java.util.Optional;

/** Where Google's token-signing public keys come from; a port so tests can supply their own keys without the network. */
public interface GoogleKeySource {

    Optional<PublicKey> keyFor(String keyId);
}
