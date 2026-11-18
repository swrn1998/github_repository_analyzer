package com.githubanalyzer.service.enrichment;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

// SOLID - Single Responsibility: only builds _links for HATEOAS responses
// HATEOAS (Hypermedia As The Engine Of Application State):
// Each response includes links to related operations so the client
// can discover what it can do next without hardcoding URLs.
// This satisfies Richardson REST Maturity Level 3.
@Component
public class HateoasLinkEnricher {

    private static final String GITHUB_BASE = "https://github.com";

    public Map<String, Map<String, String>> buildLinks(String owner, String repo) {
        // LinkedHashMap preserves insertion order in the JSON response
        Map<String, Map<String, String>> links = new LinkedHashMap<>();
        links.put("self", href("/api/analyze?owner=" + owner + "&repo=" + repo));      // current resource
        links.put("compare", href("/api/compare?ownerA=" + owner + "&repoA=" + repo)); // related action
        links.put("github", href(GITHUB_BASE + "/" + owner + "/" + repo));             // external resource
        links.put("cache-stats", href("/api/cache/stats"));                            // operational link
        return links;
    }

    private Map<String, String> href(String url) {
        // HAL-style link: { "href": "..." }
        return Map.of("href", url);
    }
}
