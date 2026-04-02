package com.looksee.models.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GenericIssue is a class that represents a generic issue.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>description, title, cssSelector, and recommendation are never null when
 *       constructed via the all-args constructor.</li>
 * </ul>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GenericIssue {
    private String description;
    private String title;
    private String cssSelector;
    private String recommendation;
}
