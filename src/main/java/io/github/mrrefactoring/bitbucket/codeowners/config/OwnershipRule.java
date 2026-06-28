package io.github.mrrefactoring.bitbucket.codeowners.config;

import java.util.Collections;
import java.util.List;

/** A single ownership rule: which paths require approvals from which owners, and how many. */
public final class OwnershipRule {

    private final List<String> paths;
    private final List<String> owners;
    private final int minApprovals;

    public OwnershipRule(List<String> paths, List<String> owners, int minApprovals) {
        this.paths = Collections.unmodifiableList(paths);
        this.owners = Collections.unmodifiableList(owners);
        this.minApprovals = minApprovals;
    }

    /** Glob patterns (gitignore-style). The rule applies if any changed path matches any glob. */
    public List<String> getPaths() {
        return paths;
    }

    /** Owner references: {@code @group}, a bare {@code username}, or {@code @@bitbucket-group}. */
    public List<String> getOwners() {
        return owners;
    }

    /** Number of approvals required from this rule's owners. */
    public int getMinApprovals() {
        return minApprovals;
    }
}
