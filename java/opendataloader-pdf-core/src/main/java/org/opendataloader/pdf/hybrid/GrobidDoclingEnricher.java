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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Applies external GROBID structure hints to Docling JSON before ODL transformation.
 *
 * <p>The first vertical slice keeps matching intentionally conservative:
 * same page number plus normalized text equality. This is sufficient for stable
 * section heading promotion without introducing layout-driven false positives.
 */
public final class GrobidDoclingEnricher {

    private static final String TEXT_HINTS = "text_hints";
    private static final String TEXTS = "texts";
    private static final String META = "meta";
    private static final String PAGE_NO = "page_no";
    private static final String TEXT = "text";
    private static final String GROBID_LABEL = "grobid_label";
    private static final String GROBID_LEVEL = "grobid_level";

    private GrobidDoclingEnricher() {
    }

    public static JsonNode enrich(JsonNode doclingJson, JsonNode grobidHints) {
        if (doclingJson == null || doclingJson.isNull()) {
            return doclingJson;
        }
        JsonNode cloned = doclingJson.deepCopy();
        if (!(cloned instanceof ObjectNode) || grobidHints == null || grobidHints.isNull()) {
            return cloned;
        }

        Map<String, JsonNode> hintsByKey = buildTextHintIndex(grobidHints.get(TEXT_HINTS));
        if (hintsByKey.isEmpty()) {
            return cloned;
        }

        JsonNode texts = cloned.get(TEXTS);
        if (!(texts instanceof ArrayNode)) {
            return cloned;
        }

        for (JsonNode textNode : texts) {
            if (!(textNode instanceof ObjectNode)) {
                continue;
            }
            int pageNo = extractPageNumber(textNode);
            String text = normalizedText(textNode.get(TEXT) != null ? textNode.get(TEXT).asText() : null);
            JsonNode hint = hintsByKey.get(buildKey(pageNo, text));
            if (hint == null) {
                continue;
            }

            ObjectNode objectNode = (ObjectNode) textNode;
            ObjectNode metaNode = objectNode.has(META) && objectNode.get(META) instanceof ObjectNode
                ? (ObjectNode) objectNode.get(META)
                : objectNode.putObject(META);

            if (hint.hasNonNull(GROBID_LABEL)) {
                metaNode.put(GROBID_LABEL, hint.get(GROBID_LABEL).asText());
            }
            if (hint.has(GROBID_LEVEL) && hint.get(GROBID_LEVEL).canConvertToInt()) {
                metaNode.put(GROBID_LEVEL, hint.get(GROBID_LEVEL).asInt());
            }
        }

        return cloned;
    }

    private static Map<String, JsonNode> buildTextHintIndex(JsonNode textHints) {
        Map<String, JsonNode> hintsByKey = new HashMap<>();
        if (!(textHints instanceof ArrayNode)) {
            return hintsByKey;
        }
        Iterator<JsonNode> iterator = textHints.elements();
        while (iterator.hasNext()) {
            JsonNode hint = iterator.next();
            int pageNo = hint.has(PAGE_NO) ? hint.get(PAGE_NO).asInt(0) : 0;
            String text = normalizedText(hint.has(TEXT) ? hint.get(TEXT).asText() : null);
            if (pageNo > 0 && !text.isEmpty()) {
                hintsByKey.put(buildKey(pageNo, text), hint);
            }
        }
        return hintsByKey;
    }

    private static int extractPageNumber(JsonNode textNode) {
        JsonNode prov = textNode.get("prov");
        if (prov instanceof ArrayNode && prov.size() > 0 && prov.get(0).has(PAGE_NO)) {
            return prov.get(0).get(PAGE_NO).asInt(0);
        }
        return 0;
    }

    private static String buildKey(int pageNo, String normalizedText) {
        return pageNo + "::" + normalizedText;
    }

    private static String normalizedText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
