package com.looksee.pageBuilder.schemas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-level request body DTO for the Pub/Sub push envelope.
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Invariant:</b> A valid instance has a non-null {@code message}
 *       whose {@code data} field is a non-blank Base64-encoded string.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "Body",
    description = "Top-level request body containing the message",
    requiredProperties = {"message"}
)
public class BodySchema {

    @Schema(
        description = "Message containing the audit request data",
        required = true
    )
    private MessageSchema message;
}
