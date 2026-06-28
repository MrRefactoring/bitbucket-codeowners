package io.github.mrrefactoring.bitbucket.codeowners.check;

import io.github.mrrefactoring.bitbucket.codeowners.eval.EvaluationResult;
import io.github.mrrefactoring.bitbucket.codeowners.match.Requirement;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CodeOwnersMergeCheckDecideTest {

    private static Requirement seniorGate(int minApprovals, boolean resolvable, String... owners) {
        return new Requirement("frontend/x.tsx", "@frontend-seniors",
                new HashSet<>(Arrays.asList(owners)), minApprovals, resolvable);
    }

    private static Set<String> approved(String... users) {
        return new HashSet<>(Arrays.asList(users));
    }

    @Test
    public void acceptsWhenConfigAbsent() {
        CheckDecision d = CodeOwnersMergeCheck.decide(EvaluationResult.absent(), Collections.<String>emptySet());
        assertTrue(d.isAccepted());
    }

    @Test
    public void rejectsOnConfigError() {
        CheckDecision d = CodeOwnersMergeCheck.decide(EvaluationResult.error("boom"), Collections.<String>emptySet());
        assertFalse(d.isAccepted());
        assertEquals(CodeOwnersMergeCheck.INVALID_SUMMARY, d.getSummary());
        assertEquals("boom", d.getDetails());
    }

    @Test
    public void acceptsWhenNoRulesMatch() {
        CheckDecision d = CodeOwnersMergeCheck.decide(
                EvaluationResult.of(Collections.<Requirement>emptyList(), true), Collections.<String>emptySet());
        assertTrue(d.isAccepted());
    }

    @Test
    public void rejectsWhenSeniorApprovalMissing() {
        Requirement r = seniorGate(1, true, "alice", "bob");
        CheckDecision d = CodeOwnersMergeCheck.decide(
                EvaluationResult.of(Collections.singletonList(r), true), Collections.<String>emptySet());
        assertFalse(d.isAccepted());
        assertEquals(CodeOwnersMergeCheck.VETO_SUMMARY, d.getSummary());
        assertTrue(d.getDetails().contains("frontend/x.tsx"));
    }

    @Test
    public void acceptsWhenSeniorApproved() {
        Requirement r = seniorGate(1, true, "alice", "bob");
        CheckDecision d = CodeOwnersMergeCheck.decide(
                EvaluationResult.of(Collections.singletonList(r), true), approved("alice"));
        assertTrue(d.isAccepted());
    }

    @Test
    public void juniorApprovalDoesNotUnblock() {
        Requirement r = seniorGate(1, true, "alice", "bob");
        CheckDecision d = CodeOwnersMergeCheck.decide(
                EvaluationResult.of(Collections.singletonList(r), true), approved("carol"));
        assertFalse(d.isAccepted());
    }

    @Test
    public void rejectsUnresolvableOwners() {
        Requirement r = seniorGate(1, false);
        CheckDecision d = CodeOwnersMergeCheck.decide(
                EvaluationResult.of(Collections.singletonList(r), true), Collections.<String>emptySet());
        assertFalse(d.isAccepted());
        assertTrue(d.getDetails().contains("could not be resolved"));
    }
}
