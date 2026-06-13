package com.cyna.modules.account.application.query.export;

import com.cyna.shared.application.Query;

import java.util.UUID;

/**
 * RGPD Art. 15 (right of access) + Art. 20 (portability). Returns every piece
 * of personal data the platform holds about the authenticated user in a
 * structured, machine-readable form. {@code userId} comes from the security
 * principal, never the request.
 */
public record ExportMyDataQuery(UUID userId) implements Query<MyDataExport> {}
