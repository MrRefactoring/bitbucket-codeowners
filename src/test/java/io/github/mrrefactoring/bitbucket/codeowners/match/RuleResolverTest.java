package io.github.mrrefactoring.bitbucket.codeowners.match;

import io.github.mrrefactoring.bitbucket.codeowners.config.CodeOwnersConfig;
import io.github.mrrefactoring.bitbucket.codeowners.config.Options;
import io.github.mrrefactoring.bitbucket.codeowners.config.OwnershipRule;
import io.github.mrrefactoring.bitbucket.codeowners.user.GroupMemberResolver;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RuleResolverTest {

    private static CodeOwnersConfig config(Map<String, List<String>> groups, OwnershipRule... rules) {
        return new CodeOwnersConfig(groups, Arrays.asList(rules), Options.defaults());
    }

    private static Map<String, List<String>> groups(String name, String... users) {
        Map<String, List<String>> g = new LinkedHashMap<>();
        g.put(name, Arrays.asList(users));
        return g;
    }

    private static Set<String> paths(String... p) {
        return new HashSet<>(Arrays.asList(p));
    }

    @Test
    public void matchesRuleAndResolvesLocalGroup() {
        CodeOwnersConfig config = config(
                groups("frontend-seniors", "alice", "bob"),
                new OwnershipRule(Arrays.asList("frontend/**", "*.tsx"),
                        Collections.singletonList("@frontend-seniors"), 1));

        List<Requirement> reqs = RuleResolver.resolve(
                paths("frontend/app/Button.tsx", "README.md"), config, new GroupMemberResolver());

        assertEquals(1, reqs.size());
        Requirement r = reqs.get(0);
        assertTrue(r.isResolvable());
        assertEquals(new HashSet<>(Arrays.asList("alice", "bob")), r.getOwnerUsers());
        assertEquals(1, r.getMinApprovals());
    }

    @Test
    public void noMatchYieldsNoRequirements() {
        CodeOwnersConfig config = config(
                groups("frontend-seniors", "alice"),
                new OwnershipRule(Collections.singletonList("frontend/**"),
                        Collections.singletonList("@frontend-seniors"), 1));

        List<Requirement> reqs = RuleResolver.resolve(
                paths("README.md", "backend/Service.java"), config, new GroupMemberResolver());

        assertTrue(reqs.isEmpty());
    }

    @Test
    public void unresolvedBitbucketGroupIsNotResolvable() {
        CodeOwnersConfig config = config(
                Collections.emptyMap(),
                new OwnershipRule(Collections.singletonList("*.java"),
                        Collections.singletonList("@@some-bb-group"), 1));

        List<Requirement> reqs = RuleResolver.resolve(paths("Main.java"), config, new GroupMemberResolver());

        assertEquals(1, reqs.size());
        assertFalse(reqs.get(0).isResolvable());
    }

    @Test
    public void unknownLocalGroupIsNotResolvable() {
        CodeOwnersConfig config = config(
                Collections.emptyMap(),
                new OwnershipRule(Collections.singletonList("*.java"),
                        Collections.singletonList("@missing"), 1));

        List<Requirement> reqs = RuleResolver.resolve(paths("Main.java"), config, new GroupMemberResolver());

        assertFalse(reqs.get(0).isResolvable());
    }

    @Test
    public void bareUserOwnerResolves() {
        CodeOwnersConfig config = config(
                Collections.emptyMap(),
                new OwnershipRule(Collections.singletonList("*.java"),
                        Collections.singletonList("carol"), 1));

        List<Requirement> reqs = RuleResolver.resolve(paths("Main.java"), config, new GroupMemberResolver());

        assertTrue(reqs.get(0).isResolvable());
        assertEquals(Collections.singleton("carol"), reqs.get(0).getOwnerUsers());
    }
}
