package io.github.mrrefactoring.bitbucket.codeowners.match;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GlobMatcherTest {

    private static boolean m(String pattern, String path) {
        return new GlobMatcher(pattern).matches(path);
    }

    @Test
    public void doubleStarUnderDirectory() {
        assertTrue(m("frontend/**", "frontend/app/Button.tsx"));
        assertTrue(m("frontend/**", "frontend/x"));
        assertFalse(m("frontend/**", "backend/x"));
    }

    @Test
    public void bareExtensionMatchesAnyDepth() {
        assertTrue(m("*.tsx", "Button.tsx"));
        assertTrue(m("*.tsx", "a/b/Button.tsx"));
        assertFalse(m("*.tsx", "Button.ts"));
    }

    @Test
    public void doubleStarSlashMatchesRootAndNested() {
        assertTrue(m("**/*.java", "Main.java"));
        assertTrue(m("**/*.java", "src/main/Main.java"));
    }

    @Test
    public void directorySubtree() {
        assertTrue(m("docs/", "docs/readme.md"));
        assertTrue(m("docs/", "docs/a/b.md"));
        assertFalse(m("docs/", "src/docs/x"));
    }

    @Test
    public void rootedAnchor() {
        assertTrue(m("/frontend/**", "frontend/x"));
        assertFalse(m("/frontend/**", "a/frontend/x"));
    }

    @Test
    public void singleStarDoesNotCrossSlash() {
        assertTrue(m("src/*.js", "src/app.js"));
        assertFalse(m("src/*.js", "src/sub/app.js"));
    }

    @Test
    public void leadingSlashOnCandidateIsIgnored() {
        assertTrue(m("*.tsx", "/Button.tsx"));
    }
}
