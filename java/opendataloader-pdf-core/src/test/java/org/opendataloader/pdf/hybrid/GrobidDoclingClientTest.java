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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendataloader.pdf.hybrid.HybridClient.HybridResponse;

import java.util.Collections;

class GrobidDoclingClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testEnrichResponseWithTeiPromotesDoclingTextToHeadingHint() throws Exception {
        ObjectNode doclingJson = objectMapper.createObjectNode();
        ArrayNode texts = doclingJson.putArray("texts");
        ObjectNode textNode = texts.addObject();
        textNode.put("label", "text");
        textNode.put("text", "Methods");
        ArrayNode prov = textNode.putArray("prov");
        ObjectNode provItem = prov.addObject();
        provItem.put("page_no", 2);
        provItem.putObject("bbox");

        HybridResponse response = new HybridResponse("", "", doclingJson, Collections.emptyMap());
        String tei = ""
            + "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">"
            + "  <text><body><div><div><head coords=\"2,100,180,310,220\">Methods</head></div></div></body></text>"
            + "</TEI>";

        HybridResponse enriched = GrobidDoclingClient.enrichResponseWithTei(response, tei);

        Assertions.assertNotNull(enriched.getJson().get("texts").get(0).get("meta"));
        Assertions.assertEquals("section_header",
            enriched.getJson().get("texts").get(0).get("meta").get("grobid_label").asText());
        Assertions.assertEquals(2,
            enriched.getJson().get("texts").get(0).get("meta").get("grobid_level").asInt());
    }
}
