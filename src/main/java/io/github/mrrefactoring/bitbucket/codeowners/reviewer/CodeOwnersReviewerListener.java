package io.github.mrrefactoring.bitbucket.codeowners.reviewer;

import com.atlassian.bitbucket.event.pull.PullRequestOpenedEvent;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import io.github.mrrefactoring.bitbucket.codeowners.eval.CodeOwnersEvaluator;
import io.github.mrrefactoring.bitbucket.codeowners.eval.EvaluationResult;
import io.github.mrrefactoring.bitbucket.codeowners.match.Requirement;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adds the matched code owners as reviewers when a pull request is opened (when
 * {@code options.autoAddReviewers} is enabled). Best-effort: failures never block PR creation.
 */
@Named
public class CodeOwnersReviewerListener {

    private static final Logger log = LoggerFactory.getLogger(CodeOwnersReviewerListener.class);

    private final EventPublisher eventPublisher;
    private final PullRequestService pullRequestService;
    private final CodeOwnersEvaluator evaluator;

    @Inject
    public CodeOwnersReviewerListener(@ComponentImport EventPublisher eventPublisher,
                                      @ComponentImport PullRequestService pullRequestService,
                                      CodeOwnersEvaluator evaluator) {
        this.eventPublisher = eventPublisher;
        this.pullRequestService = pullRequestService;
        this.evaluator = evaluator;
    }

    @PostConstruct
    public void register() {
        eventPublisher.register(this);
    }

    @PreDestroy
    public void unregister() {
        eventPublisher.unregister(this);
    }

    @EventListener
    public void onPullRequestOpened(PullRequestOpenedEvent event) {
        PullRequest pullRequest = event.getPullRequest();
        try {
            EvaluationResult result = evaluator.evaluate(pullRequest);
            if (!result.isConfigPresent() || result.hasError() || !result.isAutoAddReviewers()) {
                return;
            }

            String author = pullRequest.getAuthor().getUser().getName();
            int repositoryId = pullRequest.getToRef().getRepository().getId();
            long pullRequestId = pullRequest.getId();

            Set<String> owners = new LinkedHashSet<>();
            for (Requirement requirement : result.getRequirements()) {
                if (requirement.isResolvable()) {
                    owners.addAll(requirement.getOwnerUsers());
                }
            }
            owners.removeIf(username -> username.equalsIgnoreCase(author));

            for (String username : owners) {
                try {
                    pullRequestService.addReviewer(repositoryId, pullRequestId, username);
                } catch (RuntimeException e) {
                    log.debug("Could not add reviewer '{}' to PR {}: {}", username, pullRequestId, e.getMessage());
                }
            }
        } catch (RuntimeException e) {
            log.warn("Code owners reviewer auto-add failed for PR {}: {}",
                    pullRequest.getId(), e.getMessage());
        }
    }
}
