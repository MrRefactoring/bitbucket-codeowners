package io.github.mrrefactoring.bitbucket.codeowners.approval;

import io.github.mrrefactoring.bitbucket.codeowners.match.Requirement;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Counts how many of a requirement's owners have approved (case-insensitive on username). */
public final class ApprovalCounter {

    private ApprovalCounter() {
    }

    public static int countApprovals(Requirement requirement, Set<String> approvedUsernames) {
        Set<String> approvedLower = toLower(approvedUsernames);
        int count = 0;
        for (String owner : requirement.getOwnerUsers()) {
            if (approvedLower.contains(owner.toLowerCase(Locale.ROOT))) {
                count++;
            }
        }
        return count;
    }

    public static boolean isSatisfied(Requirement requirement, Set<String> approvedUsernames) {
        return requirement.isResolvable()
                && countApprovals(requirement, approvedUsernames) >= requirement.getMinApprovals();
    }

    private static Set<String> toLower(Set<String> values) {
        Set<String> lower = new HashSet<>();
        for (String value : values) {
            if (value != null) {
                lower.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return lower;
    }
}
