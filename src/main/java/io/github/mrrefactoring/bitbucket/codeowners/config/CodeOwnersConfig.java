package io.github.mrrefactoring.bitbucket.codeowners.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Parsed representation of the {@code codeowners.yml}/{@code codeowners.yaml} config. */
public final class CodeOwnersConfig {

    private final Map<String, List<String>> groups;
    private final List<OwnershipRule> rules;
    private final Options options;

    public CodeOwnersConfig(Map<String, List<String>> groups, List<OwnershipRule> rules, Options options) {
        this.groups = Collections.unmodifiableMap(groups);
        this.rules = Collections.unmodifiableList(rules);
        this.options = options;
    }

    public Map<String, List<String>> getGroups() {
        return groups;
    }

    public List<OwnershipRule> getRules() {
        return rules;
    }

    public Options getOptions() {
        return options;
    }

    /** Returns the usernames of a locally-defined group, or an empty list if it is not defined. */
    public List<String> localGroupMembers(String groupName) {
        List<String> members = groups.get(groupName);
        return members != null ? members : Collections.emptyList();
    }
}
