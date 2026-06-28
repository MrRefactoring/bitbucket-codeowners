package io.github.mrrefactoring.bitbucket.codeowners.user;

import java.util.Collections;
import java.util.Set;

/**
 * Resolves a native Bitbucket group name to its member usernames.
 *
 * <p>v1 intentionally returns no members. Enumerating Bitbucket group membership depends on a host
 * API that must be verified per deployment, so the reliable path is to model the critical (e.g.
 * "seniors") subgroup with an explicit user list under {@code groups:} in
 * {@code codeowners.yml}. An unresolved {@code @@group} reference therefore makes its
 * rule fail-closed with a clear message rather than silently passing.
 *
 * <p>This is the single extension point for adding real Bitbucket-group resolution later.
 */
public class GroupMemberResolver {

    public Set<String> resolveBitbucketGroup(String groupName) {
        return Collections.emptySet();
    }
}
