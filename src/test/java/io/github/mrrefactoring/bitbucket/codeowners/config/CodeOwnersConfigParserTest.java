package io.github.mrrefactoring.bitbucket.codeowners.config;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CodeOwnersConfigParserTest {

    @Test
    public void parsesGroupsRulesAndOptions() {
        String yaml =
                "version: 1\n" +
                "groups:\n" +
                "  frontend-seniors:\n" +
                "    users: [alice, bob]\n" +
                "rules:\n" +
                "  - paths: ['frontend/**', '*.tsx']\n" +
                "    owners: ['@frontend-seniors']\n" +
                "    minApprovals: 2\n" +
                "options:\n" +
                "  autoAddReviewers: false\n" +
                "  unmatchedPolicy: allow\n";

        CodeOwnersConfig config = CodeOwnersConfigParser.parse(yaml);

        assertEquals(Arrays.asList("alice", "bob"), config.localGroupMembers("frontend-seniors"));
        assertEquals(1, config.getRules().size());
        assertEquals(2, config.getRules().get(0).getPaths().size());
        assertEquals(2, config.getRules().get(0).getMinApprovals());
        assertFalse(config.getOptions().isAutoAddReviewers());
        assertEquals(Options.UnmatchedPolicy.ALLOW, config.getOptions().getUnmatchedPolicy());
    }

    @Test
    public void defaultsMinApprovalsAndAutoAdd() {
        String yaml =
                "rules:\n" +
                "  - paths: ['*.java']\n" +
                "    owners: ['@backend']\n";

        CodeOwnersConfig config = CodeOwnersConfigParser.parse(yaml);

        assertEquals(1, config.getRules().get(0).getMinApprovals());
        assertTrue(config.getOptions().isAutoAddReviewers());
    }

    @Test(expected = CodeOwnersConfigException.class)
    public void rejectsEmptyContent() {
        CodeOwnersConfigParser.parse("   ");
    }

    @Test(expected = CodeOwnersConfigException.class)
    public void rejectsConfigWithoutRules() {
        CodeOwnersConfigParser.parse("groups: {}\n");
    }

    @Test(expected = CodeOwnersConfigException.class)
    public void rejectsRuleWithoutOwners() {
        CodeOwnersConfigParser.parse("rules:\n  - paths: ['*.java']\n");
    }

    @Test
    public void allowsAdvisoryMinApprovalsZero() {
        CodeOwnersConfig config = CodeOwnersConfigParser.parse(
                "rules:\n  - paths: ['frontend/**']\n    owners: ['@frontend-all']\n    minApprovals: 0\n");
        assertEquals(0, config.getRules().get(0).getMinApprovals());
    }

    @Test(expected = CodeOwnersConfigException.class)
    public void rejectsNegativeMinApprovals() {
        CodeOwnersConfigParser.parse(
                "rules:\n  - paths: ['*.x']\n    owners: ['@a']\n    minApprovals: -1\n");
    }

    @Test(expected = CodeOwnersConfigException.class)
    public void rejectsInvalidYaml() {
        CodeOwnersConfigParser.parse("rules: [a, b");
    }

    @Test(expected = CodeOwnersConfigException.class)
    public void rejectsUnknownUnmatchedPolicy() {
        CodeOwnersConfigParser.parse(
                "rules:\n  - paths: ['*.x']\n    owners: ['@a']\n" +
                "options:\n  unmatchedPolicy: nope\n");
    }
}
