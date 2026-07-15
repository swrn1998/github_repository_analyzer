package com.githubanalyzer.service.enrichment;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the HATEOAS {@code _links} map for an {@code AnalysisResponse}.
 *
 * <p>HATEOAS (Hypermedia As The Engine Of Application State) makes the API
 * self-discoverable. Each response includes links to related operations.
 *
 * <p>This satisfies Level 3 of the Richardson REST Maturity Model.
 *
 * <p>Links included:
 * <ul>
 *   <li>{@code self}        — the current request</li>
 *   <li>{@code compare}     — compare this repo with another</li>
 *   <li>{@code github}      — direct link to the repo on GitHub</li>
 *   <li>{@code cache-stats} — current cache performance stats</li>
 * </ul>
 */
@Component
public class HateoasLinkEnricher {

    private static final String GITHUB_BASE = "https://github.com";

    /**
     * Builds the {@code _links} map for the given owner/repo combination.
     *
     * @param owner repository owner
     * @param repo  repository name
     * @return ordered map of link name → {"href": url}
     */
    public Map<String, Map<String, String>> buildLinks(String owner, String repo) {
        Map<String, Map<String, String>> links = new LinkedHashMap<>();

        links.put("self", href("/api/analyze?owner=" + owner + "&repo=" + repo));
        links.put("compare", href("/api/compare?ownerA=" + owner + "&repoA=" + repo));
        links.put("github", href(GITHUB_BASE + "/" + owner + "/" + repo));
        links.put("cache-stats", href("/api/cache/stats"));

        return links;
    }

    private Map<String, String> href(String url) {
        return Map.of("href", url);
    }
}
