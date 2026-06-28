package io.github.mrrefactoring.bitbucket.codeowners.eval;

import io.github.mrrefactoring.bitbucket.codeowners.match.Requirement;

import java.util.Collections;
import java.util.List;

/**
 * Outcome of evaluating a pull request against the code-owners configuration. Shared by the merge
 * check (which turns requirements into a veto) and the reviewer listener (which adds owners).
 */
public final class EvaluationResult {

    private final boolean configPresent;
    private final String configError;
    private final List<Requirement> requirements;
    private final boolean autoAddReviewers;

    private EvaluationResult(boolean configPresent,
                            String configError,
                            List<Requirement> requirements,
                            boolean autoAddReviewers) {
        this.configPresent = configPresent;
        this.configError = configError;
        this.requirements = Collections.unmodifiableList(requirements);
        this.autoAddReviewers = autoAddReviewers;
    }

    /** No configuration file in the repository: the plugin does nothing. */
    public static EvaluationResult absent() {
        return new EvaluationResult(false, null, Collections.emptyList(), false);
    }

    /** Configuration present but invalid: the merge is blocked with {@code message}. */
    public static EvaluationResult error(String message) {
        return new EvaluationResult(true, message, Collections.emptyList(), false);
    }

    public static EvaluationResult of(List<Requirement> requirements, boolean autoAddReviewers) {
        return new EvaluationResult(true, null, requirements, autoAddReviewers);
    }

    public boolean isConfigPresent() {
        return configPresent;
    }

    public boolean hasError() {
        return configError != null;
    }

    public String getConfigError() {
        return configError;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public boolean isAutoAddReviewers() {
        return autoAddReviewers;
    }
}
