/*
 * Copyright 2025-2026 Hancom Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opendataloader.pdf.hybrid;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Hybrid client entry point for Docling output enriched with GROBID structure hints.
 *
 * <p>The current implementation intentionally reuses the docling-fast backend while the
 * GROBID enrichment logic is added incrementally in the schema transformation layer.
 * This keeps the orchestration point stable: OpenDataLoader still owns the final JSON to
 * IObject transformation and all downstream output generation.
 */
public class GrobidDoclingClient implements HybridClient {

    private final DoclingFastServerClient delegate;

    public GrobidDoclingClient(HybridConfig config) {
        this.delegate = new DoclingFastServerClient(config);
    }

    @Override
    public void checkAvailability() throws IOException {
        delegate.checkAvailability();
    }

    @Override
    public HybridResponse convert(HybridRequest request) throws IOException {
        return delegate.convert(request);
    }

    @Override
    public CompletableFuture<HybridResponse> convertAsync(HybridRequest request) {
        return delegate.convertAsync(request);
    }

    public void shutdown() {
        delegate.shutdown();
    }

    static HybridResponse enrichResponseWithTei(HybridResponse response, String teiXml) throws Exception {
        if (response == null || response.getJson() == null || teiXml == null || teiXml.trim().isEmpty()) {
            return response;
        }
        JsonNode hints = GrobidHintExtractor.extractSectionHints(teiXml);
        JsonNode enrichedJson = GrobidDoclingEnricher.enrich(response.getJson(), hints);
        return new HybridResponse(
            response.getMarkdown(),
            response.getHtml(),
            enrichedJson,
            response.getPageContents(),
            response.getFailedPages()
        );
    }
}
