package io.github.mrrefactoring.bitbucket.codeowners.config;

/**
 * Thrown when the code owners config file is present but cannot be parsed into a valid
 * configuration. The message is surfaced to the user in the merge-check veto so the
 * misconfiguration is visible and fixable.
 */
public class CodeOwnersConfigException extends RuntimeException {

    public CodeOwnersConfigException(String message) {
        super(message);
    }

    public CodeOwnersConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
