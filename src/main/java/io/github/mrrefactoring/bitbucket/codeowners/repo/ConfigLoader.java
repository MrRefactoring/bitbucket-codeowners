package io.github.mrrefactoring.bitbucket.codeowners.repo;

import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.content.NoSuchPathException;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Reads the raw {@code .bitbucket/codeowners.yml} content from a repository at a given commit. */
@Named
public class ConfigLoader {

    public static final String CONFIG_PATH = ".bitbucket/codeowners.yml";

    private final ContentService contentService;

    @Inject
    public ConfigLoader(@ComponentImport ContentService contentService) {
        this.contentService = contentService;
    }

    /**
     * Returns the raw YAML at {@link #CONFIG_PATH} for the given object id (commit/ref), or
     * {@link Optional#empty()} if the file does not exist there.
     */
    public Optional<String> loadRaw(Repository repository, String objectId) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            contentService.streamFile(repository, objectId, CONFIG_PATH, contentType -> out);
            return Optional.of(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } catch (NoSuchPathException e) {
            return Optional.empty();
        }
    }
}
