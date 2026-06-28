package io.github.mrrefactoring.bitbucket.codeowners.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses the {@code codeowners.yml}/{@code codeowners.yaml} content into a {@link CodeOwnersConfig}.
 *
 * <p>Parsing is defensive and map-based (no YAML-to-bean binding) so that hostile or malformed
 * input results in a {@link CodeOwnersConfigException} with a clear message rather than an
 * arbitrary deserialization side effect.
 */
public final class CodeOwnersConfigParser {

    private CodeOwnersConfigParser() {
    }

    public static CodeOwnersConfig parse(String yamlContent) {
        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            throw new CodeOwnersConfigException("Configuration file is empty.");
        }

        Object root;
        try {
            root = new Yaml().load(yamlContent);
        } catch (YAMLException e) {
            throw new CodeOwnersConfigException("YAML syntax error: " + e.getMessage(), e);
        }

        if (!(root instanceof Map)) {
            throw new CodeOwnersConfigException("Top-level YAML must be a mapping with 'rules'.");
        }
        Map<?, ?> top = (Map<?, ?>) root;

        Map<String, List<String>> groups = parseGroups(top.get("groups"));
        List<OwnershipRule> rules = parseRules(top.get("rules"));
        Options options = parseOptions(top.get("options"));

        if (rules.isEmpty()) {
            throw new CodeOwnersConfigException("At least one rule is required under 'rules'.");
        }
        return new CodeOwnersConfig(groups, rules, options);
    }

    private static Map<String, List<String>> parseGroups(Object raw) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (raw == null) {
            return result;
        }
        if (!(raw instanceof Map)) {
            throw new CodeOwnersConfigException("'groups' must be a mapping of name -> { users: [...] }.");
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
            String name = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            List<String> users;
            if (value instanceof Map) {
                users = toStringList(((Map<?, ?>) value).get("users"));
            } else if (value instanceof List) {
                // tolerate the shorthand "groupName: [alice, bob]"
                users = toStringList(value);
            } else {
                throw new CodeOwnersConfigException("Group '" + name + "' must contain a 'users' list.");
            }
            result.put(name, users);
        }
        return result;
    }

    private static List<OwnershipRule> parseRules(Object raw) {
        List<OwnershipRule> rules = new ArrayList<>();
        if (raw == null) {
            return rules;
        }
        if (!(raw instanceof List)) {
            throw new CodeOwnersConfigException("'rules' must be a list.");
        }
        int index = 0;
        for (Object item : (List<?>) raw) {
            index++;
            if (!(item instanceof Map)) {
                throw new CodeOwnersConfigException("rules[" + index + "] must be a mapping.");
            }
            Map<?, ?> rule = (Map<?, ?>) item;
            List<String> paths = toStringList(rule.get("paths"));
            if (paths.isEmpty()) {
                throw new CodeOwnersConfigException("rules[" + index + "] requires a non-empty 'paths' list.");
            }
            List<String> owners = toStringList(rule.get("owners"));
            if (owners.isEmpty()) {
                throw new CodeOwnersConfigException("rules[" + index + "] requires a non-empty 'owners' list.");
            }
            int minApprovals = toInt(rule.get("minApprovals"), 1);
            if (minApprovals < 0) {
                throw new CodeOwnersConfigException("rules[" + index + "] 'minApprovals' must be >= 0 "
                        + "(0 means advisory: owners are suggested as reviewers but do not block the merge).");
            }
            rules.add(new OwnershipRule(paths, owners, minApprovals));
        }
        return rules;
    }

    private static Options parseOptions(Object raw) {
        if (raw == null) {
            return Options.defaults();
        }
        if (!(raw instanceof Map)) {
            throw new CodeOwnersConfigException("'options' must be a mapping.");
        }
        Map<?, ?> options = (Map<?, ?>) raw;
        boolean autoAddReviewers = toBool(options.get("autoAddReviewers"), true);
        Options.UnmatchedPolicy policy = parsePolicy(options.get("unmatchedPolicy"));
        return new Options(autoAddReviewers, policy);
    }

    private static Options.UnmatchedPolicy parsePolicy(Object raw) {
        if (raw == null) {
            return Options.UnmatchedPolicy.ALLOW;
        }
        String value = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
        switch (value) {
            case "allow":
                return Options.UnmatchedPolicy.ALLOW;
            case "require-default":
            case "require_default":
                return Options.UnmatchedPolicy.REQUIRE_DEFAULT;
            default:
                throw new CodeOwnersConfigException(
                        "Unknown unmatchedPolicy '" + raw + "' (expected 'allow' or 'require-default').");
        }
    }

    private static List<String> toStringList(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        if (raw instanceof List) {
            for (Object element : (List<?>) raw) {
                if (element != null) {
                    String value = String.valueOf(element).trim();
                    if (!value.isEmpty()) {
                        out.add(value);
                    }
                }
            }
        } else {
            String value = String.valueOf(raw).trim();
            if (!value.isEmpty()) {
                out.add(value);
            }
        }
        return out;
    }

    private static int toInt(Object raw, int defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            throw new CodeOwnersConfigException("Expected an integer but got '" + raw + "'.");
        }
    }

    private static boolean toBool(Object raw, boolean defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        String value = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
        return value.equals("true") || value.equals("yes") || value.equals("on");
    }
}
