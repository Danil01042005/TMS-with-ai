package ru.tms.user.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeoInfraController {

    @Value("${app.frontend.base-url:http://localhost:3030}")
    private String frontendBaseUrl;

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        String content = """
                User-agent: *
                Allow: /login
                Disallow: /
                                
                Sitemap: %s/sitemap.xml
                """.formatted(frontendBaseUrl);
        return ResponseEntity.ok(content);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        String content = """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                  <url>
                    <loc>%s/login</loc>
                    <changefreq>weekly</changefreq>
                    <priority>1.0</priority>
                  </url>
                </urlset>
                """.formatted(frontendBaseUrl);
        return ResponseEntity.ok(content);
    }
}

