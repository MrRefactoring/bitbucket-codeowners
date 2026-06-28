package io.github.mrrefactoring.bitbucket.codeowners.match;

import java.util.regex.Pattern;

/**
 * Matches a repository file path against a single gitignore/CODEOWNERS-style glob.
 *
 * <p>Semantics:
 * <ul>
 *   <li>{@code *} matches within a path segment (does not cross {@code /}).</li>
 *   <li>{@code **} matches across path segments.</li>
 *   <li>A leading {@code /} anchors the pattern to the repository root.</li>
 *   <li>A pattern containing an internal {@code /} is anchored to the root; a pattern with no
 *       slash (e.g. {@code *.tsx}) matches at any directory depth.</li>
 *   <li>A trailing {@code /} (e.g. {@code docs/}) matches that directory's subtree.</li>
 * </ul>
 */
public final class GlobMatcher {

    private final String pattern;
    private final Pattern regex;

    public GlobMatcher(String globPattern) {
        this.pattern = globPattern;
        this.regex = Pattern.compile(toRegex(globPattern));
    }

    public String getPattern() {
        return pattern;
    }

    public boolean matches(String path) {
        if (path == null) {
            return false;
        }
        String candidate = path.startsWith("/") ? path.substring(1) : path;
        return regex.matcher(candidate).matches();
    }

    static String toRegex(String glob) {
        String g = glob.trim();
        if (g.startsWith("./")) {
            g = g.substring(2);
        }

        boolean rooted = g.startsWith("/");
        if (rooted) {
            g = g.substring(1);
        }

        boolean dirOnly = g.endsWith("/");
        if (dirOnly) {
            g = g + "**"; // "docs/" -> "docs/**"
        }

        boolean anchored = rooted || g.contains("/");

        StringBuilder regex = new StringBuilder("^");
        if (!anchored) {
            regex.append("(?:.*/)?");
        }
        regex.append(translate(g));
        regex.append('$');
        return regex.toString();
    }

    private static String translate(String g) {
        StringBuilder sb = new StringBuilder();
        int n = g.length();
        for (int i = 0; i < n; i++) {
            char c = g.charAt(i);
            if (c == '*') {
                boolean doubleStar = (i + 1 < n) && g.charAt(i + 1) == '*';
                if (doubleStar) {
                    i++; // consume the second '*'
                    if (i + 1 < n && g.charAt(i + 1) == '/') {
                        i++; // consume the trailing '/' so "**/" matches zero or more directories
                        sb.append("(?:.*/)?");
                    } else {
                        sb.append(".*");
                    }
                } else {
                    sb.append("[^/]*");
                }
            } else if (c == '?') {
                sb.append("[^/]");
            } else if (c == '/') {
                sb.append('/');
            } else if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
