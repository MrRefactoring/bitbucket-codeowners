package io.github.mrrefactoring.bitbucket.codeowners.repo;

import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.content.NoSuchPathException;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Reads the raw code owners configuration from the <strong>repository root</strong> at a given
 * commit/ref.
 *
 * <p>The file lives at the repository root and may use either YAML extension. Candidate names are
 * tried in {@link #CONFIG_PATHS} order and the first one present wins:
 * <ol>
 *   <li>{@code codeowners.yml}</li>
 *   <li>{@code codeowners.yaml}</li>
 * </ol>
 */
@Component
public class ConfigLoader {

    /** Recommended file name, used in user-facing guidance and documentation. */
    public static final String PRIMARY_CONFIG_PATH = "codeowners.yml";

    /** Candidate config paths at the repository root, in resolution order (first match wins). */
    public static final List<String> CONFIG_PATHS =
            Collections.unmodifiableList(Arrays.asList("codeowners.yml", "codeowners.yaml"));

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    private final ContentService contentService;

    @Autowired
    public ConfigLoader(@ComponentImport ContentService contentService) {
        this.contentService = contentService;
    }

    /**
     * Returns the raw YAML of the first existing config file (see {@link #CONFIG_PATHS}) for the
     * given object id (commit/ref), or {@link Optional#empty()} if none of the candidates exist.
     */
    public Optional<String> loadRaw(Repository repository, String objectId) {
        for (String path : CONFIG_PATHS) {
            Optional<String> content = readFile(repository, objectId, path);
            if (content.isPresent()) {
                log.debug("Loaded code owners config from '{}' in {}", path, repository.getSlug());
                return content;
            }
        }
        return Optional.empty();
    }

    private Optional<String> readFile(Repository repository, String objectId, String path) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            contentService.streamFile(repository, objectId, path, contentType -> out);
            return Optional.of(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } catch (NoSuchPathException e) {
            return Optional.empty();
        }
    }
}
