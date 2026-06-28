package io.github.mrrefactoring.bitbucket.codeowners.match;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A resolved approval requirement produced by {@link RuleResolver} for a rule that matched at
 * least one changed path.
 */
public final class Requirement {

    private final String matchedPath;
    private final String ownersLabel;
    private final Set<String> ownerUsers;
    private final int minApprovals;
    private final boolean resolvable;

    public Requirement(String matchedPath,
                       String ownersLabel,
                       Set<String> ownerUsers,
                       int minApprovals,
                       boolean resolvable) {
        this.matchedPath = matchedPath;
        this.ownersLabel = ownersLabel;
        this.ownerUsers = Collections.unmodifiableSet(new LinkedHashSet<>(ownerUsers));
        this.minApprovals = minApprovals;
        this.resolvable = resolvable;
    }

    /** A sample changed path that triggered this rule (for the veto message). */
    public String getMatchedPath() {
        return matchedPath;
    }

    /** Human-readable owners label (the raw owner references), e.g. {@code @frontend-seniors}. */
    public String getOwnersLabel() {
        return ownersLabel;
    }

    /** Owner usernames (original case as written in config / resolved from a group). */
    public Set<String> getOwnerUsers() {
        return ownerUsers;
    }

    public int getMinApprovals() {
        return minApprovals;
    }

    /**
     * {@code false} when an owner reference could not be resolved to any users (e.g. an
     * unresolved {@code @@bitbucket-group} or an unknown {@code @group}). Such a requirement is
     * treated as fail-closed so the misconfiguration is surfaced rather than silently passing.
     */
    public boolean isResolvable() {
        return resolvable;
    }
}
