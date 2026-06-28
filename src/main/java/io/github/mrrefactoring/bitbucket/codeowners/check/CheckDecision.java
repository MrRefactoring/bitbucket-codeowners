package io.github.mrrefactoring.bitbucket.codeowners.check;

/**
 * Pure decision produced by the merge-check logic, independent of the Bitbucket
 * {@code RepositoryHookResult} type so the core logic stays unit-testable.
 */
public final class CheckDecision {

    private final boolean accepted;
    private final String summary;
    private final String details;

    private CheckDecision(boolean accepted, String summary, String details) {
        this.accepted = accepted;
        this.summary = summary;
        this.details = details;
    }

    public static CheckDecision accepted() {
        return new CheckDecision(true, null, null);
    }

    public static CheckDecision rejected(String summary, String details) {
        return new CheckDecision(false, summary, details);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetails() {
        return details;
    }
}
