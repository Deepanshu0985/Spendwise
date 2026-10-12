package com.finance.application.user;

import java.util.UUID;

/** A person's right to take their data with them and to have it erased (data-privacy-retention.md, DPDP). */
public interface UserDataService {

    /** Everything held about the person as one JSON document (UTF-8). Never includes the password hash or other people's data. */
    byte[] exportAsJson(UUID userId);

    /** Erases the account and all its data for good. Throws a validation error, deleting nothing, when the proof is wrong. */
    void deleteAccount(UUID userId, DeleteAccountCommand command);
}
