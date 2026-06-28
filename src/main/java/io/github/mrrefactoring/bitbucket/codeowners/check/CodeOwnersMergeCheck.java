package io.github.mrrefactoring.bitbucket.codeowners.check;

import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.PullRequestMergeHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryMergeCheck;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestParticipant;
import com.atlassian.bitbucket.pull.PullRequestParticipantStatus;

import io.github.mrrefactoring.bitbucket.codeowners.approval.ApprovalCounter;
import io.github.mrrefactoring.bitbucket.codeowners.eval.CodeOwnersEvaluator;
import io.github.mrrefactoring.bitbucket.codeowners.eval.EvaluationResult;
import io.github.mrrefactoring.bitbucket.codeowners.match.Requirement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Merge check that blocks a pull request until the configured number of approvals from each
 * matched code-owner group has been collected for the paths the pull request changes.
 *
 * <p>Registered as {@code <repository-merge-check class="bean:codeOwnersMergeCheck"/>}; enabled
 * per repository/project under Settings &rarr; Merge checks.
 */
@Component("codeOwnersMergeCheck")
public class CodeOwnersMergeCheck implements RepositoryMergeCheck {

    static final String VETO_SUMMARY = "Code owner approval required";
    static final String INVALID_SUMMARY = "Invalid code owners configuration";

    private final CodeOwnersEvaluator evaluator;

    @Autowired
    public CodeOwnersMergeCheck(CodeOwnersEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public RepositoryHookResult preUpdate(PreRepositoryHookContext context,
                                          PullRequestMergeHookRequest request) {
        PullRequest pullRequest = request.getPullRequest();
        EvaluationResult result = evaluator.evaluate(pullRequest);
        CheckDecision decision = decide(result, approvedUsernames(pullRequest));

        return decision.isAccepted()
                ? RepositoryHookResult.accepted()
                : RepositoryHookResult.rejected(decision.getSummary(), decision.getDetails());
    }

    /** Pure decision logic, unit-tested without the Bitbucket hook-result type. */
    static CheckDecision decide(EvaluationResult result, Set<String> approvedUsernames) {
        if (!result.isConfigPresent()) {
            return CheckDecision.accepted();
        }
        if (result.hasError()) {
            return CheckDecision.rejected(INVALID_SUMMARY, result.getConfigError());
        }

        List<Requirement> requirements = result.getRequirements();
        if (requirements.isEmpty()) {
            return CheckDecision.accepted();
        }

        List<String> violations = new ArrayList<>();
        for (Requirement requirement : requirements) {
            if (!requirement.isResolvable()) {
                violations.add("Owners for \"" + requirement.getMatchedPath() + "\" could not be resolved ("
                        + requirement.getOwnersLabel()
                        + "). Define the group's users explicitly in codeowners.yml.");
                continue;
            }
            int have = ApprovalCounter.countApprovals(requirement, approvedUsernames);
            if (have < requirement.getMinApprovals()) {
                violations.add("\"" + requirement.getMatchedPath() + "\" requires "
                        + requirement.getMinApprovals() + " approval(s) from "
                        + requirement.getOwnersLabel() + " (currently " + have + ").");
            }
        }

        if (violations.isEmpty()) {
            return CheckDecision.accepted();
        }
        return CheckDecision.rejected(VETO_SUMMARY, String.join("\n", violations));
    }

    private static Set<String> approvedUsernames(PullRequest pullRequest) {
        Set<String> approved = new HashSet<>();
        for (PullRequestParticipant participant : pullRequest.getReviewers()) {
            if (participant.getStatus() == PullRequestParticipantStatus.APPROVED) {
                approved.add(participant.getUser().getName().toLowerCase(Locale.ROOT));
            }
        }
        return approved;
    }
}
