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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GrobidDoclingEnricherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testEnrichPromotesMatchingTextToGrobidSectionHeader() {
        ObjectNode doclingJson = objectMapper.createObjectNode();
        ArrayNode texts = doclingJson.putArray("texts");

        ObjectNode textNode = texts.addObject();
        textNode.put("label", "text");
        textNode.put("text", "Methods");
        addProvenance(textNode, 1);

        ObjectNode hints = objectMapper.createObjectNode();
        ArrayNode textHints = hints.putArray("text_hints");
        ObjectNode hint = textHints.addObject();
        hint.put("page_no", 1);
        hint.put("text", "Methods");
        hint.put("grobid_label", "section_header");
        hint.put("grobid_level", 2);

        JsonNode enriched = GrobidDoclingEnricher.enrich(doclingJson, hints);

        JsonNode enrichedText = enriched.get("texts").get(0);
        Assertions.assertNotNull(enrichedText.get("meta"));
        Assertions.assertEquals("section_header", enrichedText.get("meta").get("grobid_label").asText());
        Assertions.assertEquals(2, enrichedText.get("meta").get("grobid_level").asInt());
    }

    @Test
    void testEnrichLeavesNonMatchingTextUnchanged() {
        ObjectNode doclingJson = objectMapper.createObjectNode();
        ArrayNode texts = doclingJson.putArray("texts");

        ObjectNode textNode = texts.addObject();
        textNode.put("label", "text");
        textNode.put("text", "Results");
        addProvenance(textNode, 1);

        ObjectNode hints = objectMapper.createObjectNode();
        ArrayNode textHints = hints.putArray("text_hints");
        ObjectNode hint = textHints.addObject();
        hint.put("page_no", 1);
        hint.put("text", "Methods");
        hint.put("grobid_label", "section_header");
        hint.put("grobid_level", 2);

        JsonNode enriched = GrobidDoclingEnricher.enrich(doclingJson, hints);

        JsonNode enrichedText = enriched.get("texts").get(0);
        Assertions.assertTrue(enrichedText.get("meta") == null || enrichedText.get("meta").isMissingNode());
    }

    private void addProvenance(ObjectNode node, int pageNo) {
        ArrayNode prov = node.putArray("prov");
        ObjectNode provItem = prov.addObject();
        provItem.put("page_no", pageNo);
        ObjectNode bbox = provItem.putObject("bbox");
        bbox.put("l", 100);
        bbox.put("t", 700);
        bbox.put("r", 300);
        bbox.put("b", 740);
    }
}
