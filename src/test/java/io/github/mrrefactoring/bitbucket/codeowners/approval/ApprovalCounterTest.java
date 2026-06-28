package io.github.mrrefactoring.bitbucket.codeowners.approval;

import io.github.mrrefactoring.bitbucket.codeowners.match.Requirement;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApprovalCounterTest {

    private static Requirement requirement(int minApprovals, boolean resolvable, String... owners) {
        return new Requirement("frontend/x.tsx", "@frontend-seniors",
                new HashSet<>(Arrays.asList(owners)), minApprovals, resolvable);
    }

    private static Set<String> approved(String... users) {
        return new HashSet<>(Arrays.asList(users));
    }

    @Test
    public void countsCaseInsensitively() {
        Requirement r = requirement(1, true, "Alice", "Bob");
        assertEquals(1, ApprovalCounter.countApprovals(r, approved("alice")));
        assertTrue(ApprovalCounter.isSatisfied(r, approved("alice")));
    }

    @Test
    public void notSatisfiedBelowThreshold() {
        Requirement r = requirement(2, true, "alice", "bob");
        assertEquals(1, ApprovalCounter.countApprovals(r, approved("alice")));
        assertFalse(ApprovalCounter.isSatisfied(r, approved("alice")));
    }

    @Test
    public void juniorApprovalDoesNotCount() {
        Requirement r = requirement(1, true, "alice", "bob");
        assertEquals(0, ApprovalCounter.countApprovals(r, approved("carol")));
        assertFalse(ApprovalCounter.isSatisfied(r, approved("carol")));
    }

    @Test
    public void unresolvableNeverSatisfied() {
        Requirement r = requirement(1, false);
        assertFalse(ApprovalCounter.isSatisfied(r, approved("alice", "bob")));
    }

    @Test
    public void satisfiedWhenThresholdMet() {
        Requirement r = requirement(2, true, "alice", "bob", "dave");
        assertTrue(ApprovalCounter.isSatisfied(r, approved("alice", "dave")));
    }

    @Test
    public void emptyApprovalsIsZero() {
        Requirement r = requirement(1, true, "alice");
        assertEquals(0, ApprovalCounter.countApprovals(r, Collections.<String>emptySet()));
    }
}
