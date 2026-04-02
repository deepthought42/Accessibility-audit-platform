package com.looksee.pageBuilder.schemas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAPI schema definition for the decoded audit start payload.
 *
 * <p>This class mirrors the structure of
 * {@code com.looksee.models.message.AuditStartMessage} and is used
 * exclusively for Swagger/OpenAPI documentation.</p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Invariant:</b> A valid instance has all four fields
 *       ({@code url}, {@code type}, {@code accountId}, {@code auditId})
 *       set to non-null values.</li>
 *   <li><b>Invariant:</b> {@code type} must be one of {@code "PAGE"} or
 *       {@code "DOMAIN"}.</li>
 *   <li><b>Invariant:</b> {@code url} must be a well-formed URI.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "AuditStartMessage",
    description = "Decoded audit payload containing request details",
    requiredProperties = {"url", "type", "accountId", "auditId"}
)
public class AuditStartMessageSchema {

    @Schema(
        description = "URL to be processed and audited",
        example = "https://example.com",
        required = true,
        format = "uri"
    )
    private String url;

    @Schema(
        description = "Type of audit to perform",
        example = "PAGE",
        required = true,
        allowableValues = {"PAGE", "DOMAIN"}
    )
    private String type;

    @Schema(
        description = "Account identifier",
        example = "123",
        required = true
    )
    private String accountId;

    @Schema(
        description = "Audit identifier",
        example = "456",
        required = true
    )
    private String auditId;
}
