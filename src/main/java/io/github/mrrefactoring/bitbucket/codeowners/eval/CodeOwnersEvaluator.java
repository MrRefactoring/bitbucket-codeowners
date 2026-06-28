package io.github.mrrefactoring.bitbucket.codeowners.eval;

import com.atlassian.bitbucket.content.Change;
import com.atlassian.bitbucket.content.ChangeCallback;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestChangesRequest;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import io.github.mrrefactoring.bitbucket.codeowners.config.CodeOwnersConfig;
import io.github.mrrefactoring.bitbucket.codeowners.config.CodeOwnersConfigException;
import io.github.mrrefactoring.bitbucket.codeowners.config.CodeOwnersConfigParser;
import io.github.mrrefactoring.bitbucket.codeowners.match.RuleResolver;
import io.github.mrrefactoring.bitbucket.codeowners.repo.ConfigLoader;
import io.github.mrrefactoring.bitbucket.codeowners.user.GroupMemberResolver;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Loads the configuration from a pull request's target branch, computes the changed paths, and
 * resolves the resulting approval requirements. Used by both the merge check and the reviewer
 * listener so the two stay consistent.
 */
@Named
public class CodeOwnersEvaluator {

    /** Safety cap on changed paths inspected, to bound work on very large pull requests. */
    static final int MAX_CHANGED_PATHS = 5000;

    private final ConfigLoader configLoader;
    private final PullRequestService pullRequestService;
    private final GroupMemberResolver groupMemberResolver = new GroupMemberResolver();

    @Inject
    public CodeOwnersEvaluator(ConfigLoader configLoader,
                               @ComponentImport PullRequestService pullRequestService) {
        this.configLoader = configLoader;
        this.pullRequestService = pullRequestService;
    }

    public EvaluationResult evaluate(PullRequest pullRequest) {
        Repository repository = pullRequest.getToRef().getRepository();
        String objectId = pullRequest.getToRef().getLatestCommit();

        Optional<String> raw = configLoader.loadRaw(repository, objectId);
        if (!raw.isPresent()) {
            return EvaluationResult.absent();
        }

        CodeOwnersConfig config;
        try {
            config = CodeOwnersConfigParser.parse(raw.get());
        } catch (CodeOwnersConfigException e) {
            return EvaluationResult.error(e.getMessage());
        }

        Set<String> changedPaths = collectChangedPaths(pullRequest);
        return EvaluationResult.of(
                RuleResolver.resolve(changedPaths, config, groupMemberResolver),
                config.getOptions().isAutoAddReviewers());
    }

    private Set<String> collectChangedPaths(PullRequest pullRequest) {
        final Set<String> paths = new HashSet<>();
        PullRequestChangesRequest request = new PullRequestChangesRequest.Builder(pullRequest).build();
        pullRequestService.streamChanges(request, new ChangeCallback() {
            @Override
            public boolean onChange(Change change) {
                paths.add(change.getPath().toString());
                return paths.size() < MAX_CHANGED_PATHS;
            }
        });
        return paths;
    }
}
