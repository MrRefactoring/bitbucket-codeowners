package io.github.mrrefactoring.bitbucket.codeowners.match;

import io.github.mrrefactoring.bitbucket.codeowners.config.CodeOwnersConfig;
import io.github.mrrefactoring.bitbucket.codeowners.config.OwnershipRule;
import io.github.mrrefactoring.bitbucket.codeowners.user.GroupMemberResolver;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Turns the set of paths changed by a pull request into the list of approval requirements that
 * must be satisfied, by matching them against the configured ownership rules.
 */
public final class RuleResolver {

    private RuleResolver() {
    }

    public static List<Requirement> resolve(Set<String> changedPaths,
                                            CodeOwnersConfig config,
                                            GroupMemberResolver groupMemberResolver) {
        List<Requirement> requirements = new ArrayList<>();
        for (OwnershipRule rule : config.getRules()) {
            List<GlobMatcher> matchers = compile(rule.getPaths());
            String sample = firstMatch(changedPaths, matchers);
            if (sample == null) {
                continue;
            }

            Set<String> owners = new LinkedHashSet<>();
            boolean resolvable = true;
            for (String ref : rule.getOwners()) {
                ResolvedOwners resolved = resolveOwnerRef(ref, config, groupMemberResolver);
                if (!resolved.resolvable) {
                    resolvable = false;
                }
                owners.addAll(resolved.users);
            }
            if (owners.isEmpty()) {
                resolvable = false;
            }

            String label = String.join(", ", rule.getOwners());
            requirements.add(new Requirement(sample, label, owners, rule.getMinApprovals(), resolvable));
        }
        return requirements;
    }

    private static List<GlobMatcher> compile(List<String> globs) {
        List<GlobMatcher> matchers = new ArrayList<>(globs.size());
        for (String glob : globs) {
            matchers.add(new GlobMatcher(glob));
        }
        return matchers;
    }

    private static String firstMatch(Set<String> paths, List<GlobMatcher> matchers) {
        for (String path : paths) {
            for (GlobMatcher matcher : matchers) {
                if (matcher.matches(path)) {
                    return path;
                }
            }
        }
        return null;
    }

    private static ResolvedOwners resolveOwnerRef(String ref,
                                                  CodeOwnersConfig config,
                                                  GroupMemberResolver groupMemberResolver) {
        String reference = ref.trim();
        Set<String> users = new LinkedHashSet<>();

        if (reference.startsWith("@@")) {
            Set<String> members = groupMemberResolver.resolveBitbucketGroup(reference.substring(2));
            if (members.isEmpty()) {
                return new ResolvedOwners(users, false);
            }
            users.addAll(members);
            return new ResolvedOwners(users, true);
        }
        if (reference.startsWith("@")) {
            List<String> members = config.localGroupMembers(reference.substring(1));
            if (members.isEmpty()) {
                return new ResolvedOwners(users, false);
            }
            users.addAll(members);
            return new ResolvedOwners(users, true);
        }
        users.add(reference);
        return new ResolvedOwners(users, true);
    }

    private static final class ResolvedOwners {
        final Set<String> users;
        final boolean resolvable;

        ResolvedOwners(Set<String> users, boolean resolvable) {
            this.users = users;
            this.resolvable = resolvable;
        }
    }
}
