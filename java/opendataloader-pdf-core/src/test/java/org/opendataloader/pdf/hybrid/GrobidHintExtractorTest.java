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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GrobidHintExtractorTest {

    @Test
    void testExtractTextHintsFromSectionHeadsWithCoords() throws Exception {
        String tei = ""
            + "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">"
            + "  <text>"
            + "    <body>"
            + "      <div>"
            + "        <head coords=\"1,100,200,300,240\">Introduction</head>"
            + "        <div>"
            + "          <head coords=\"2,110,180,310,220\">Methods</head>"
            + "        </div>"
            + "      </div>"
            + "    </body>"
            + "  </text>"
            + "</TEI>";

        JsonNode hints = GrobidHintExtractor.extractSectionHints(tei);

        Assertions.assertNotNull(hints.get("text_hints"));
        Assertions.assertEquals(2, hints.get("text_hints").size());

        JsonNode first = hints.get("text_hints").get(0);
        Assertions.assertEquals(1, first.get("page_no").asInt());
        Assertions.assertEquals("Introduction", first.get("text").asText());
        Assertions.assertEquals("section_header", first.get("grobid_label").asText());
        Assertions.assertEquals(1, first.get("grobid_level").asInt());

        JsonNode second = hints.get("text_hints").get(1);
        Assertions.assertEquals(2, second.get("page_no").asInt());
        Assertions.assertEquals("Methods", second.get("text").asText());
        Assertions.assertEquals(2, second.get("grobid_level").asInt());
    }

    @Test
    void testExtractSectionHintsIgnoresHeadsWithoutCoords() throws Exception {
        String tei = ""
            + "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">"
            + "  <text>"
            + "    <body>"
            + "      <div>"
            + "        <head>Introduction</head>"
            + "      </div>"
            + "    </body>"
            + "  </text>"
            + "</TEI>";

        JsonNode hints = GrobidHintExtractor.extractSectionHints(tei);

        Assertions.assertNotNull(hints.get("text_hints"));
        Assertions.assertEquals(0, hints.get("text_hints").size());
    }
}
