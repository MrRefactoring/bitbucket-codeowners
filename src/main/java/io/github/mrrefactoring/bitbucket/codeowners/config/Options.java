package io.github.mrrefactoring.bitbucket.codeowners.config;

/** Behavioural options from the {@code options:} section of the configuration. */
public final class Options {

    public enum UnmatchedPolicy {
        /** Pull requests that touch no owned path may merge freely. */
        ALLOW,
        /** Reserved for a future "fallback owners" rule; currently treated like {@link #ALLOW}. */
        REQUIRE_DEFAULT
    }

    private final boolean autoAddReviewers;
    private final UnmatchedPolicy unmatchedPolicy;

    public Options(boolean autoAddReviewers, UnmatchedPolicy unmatchedPolicy) {
        this.autoAddReviewers = autoAddReviewers;
        this.unmatchedPolicy = unmatchedPolicy;
    }

    public static Options defaults() {
        return new Options(true, UnmatchedPolicy.ALLOW);
    }

    public boolean isAutoAddReviewers() {
        return autoAddReviewers;
    }

    public UnmatchedPolicy getUnmatchedPolicy() {
        return unmatchedPolicy;
    }
}
