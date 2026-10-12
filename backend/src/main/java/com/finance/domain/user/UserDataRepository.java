package com.finance.domain.user;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Port: everything the product holds about one person, for handing it back (export) and for erasing it (deletion). */
public interface UserDataRepository {

    /** Each of the person's tables as a JSON array of its rows (internal columns removed), in a fixed order. */
    Map<String, String> exportTables(UUID userId);

    /**
     * Deletes every row the person owns, the account itself last, in one transaction. Returns the storage keys of their
     * uploaded statement files, which the caller removes from file storage once the rows are gone.
     */
    List<String> eraseAll(UUID userId);
}
