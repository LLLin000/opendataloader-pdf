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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * Extracts conservative structure hints from GROBID TEI output.
 *
 * <p>The first implementation only extracts section heads that include page coordinates.
 * This keeps the signal high and directly supports the heading promotion path used by
 * Docling enrichment.
 */
public final class GrobidHintExtractor {

    private static final String TEI_NS = "http://www.tei-c.org/ns/1.0";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private GrobidHintExtractor() {
    }

    public static JsonNode extractSectionHints(String teiXml) throws Exception {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ArrayNode textHints = root.putArray("text_hints");
        if (teiXml == null || teiXml.trim().isEmpty()) {
            return root;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(teiXml)));

        NodeList heads = document.getElementsByTagNameNS(TEI_NS, "head");
        for (int i = 0; i < heads.getLength(); i++) {
            Node headNode = heads.item(i);
            if (!(headNode instanceof Element)) {
                continue;
            }
            Element head = (Element) headNode;
            String coords = head.getAttribute("coords");
            if (coords == null || coords.isBlank()) {
                continue;
            }
            int pageNo = parsePageNo(coords);
            String text = head.getTextContent() != null ? head.getTextContent().trim() : "";
            if (pageNo <= 0 || text.isEmpty()) {
                continue;
            }

            ObjectNode hint = textHints.addObject();
            hint.put("page_no", pageNo);
            hint.put("text", text);
            hint.put("grobid_label", "section_header");
            hint.put("grobid_level", computeHeadingLevel(head));
        }

        return root;
    }

    private static int parsePageNo(String coords) {
        String[] parts = coords.split(",");
        if (parts.length == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[0].trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int computeHeadingLevel(Node head) {
        int depth = 0;
        Node current = head.getParentNode();
        while (current != null) {
            if (current instanceof Element && TEI_NS.equals(current.getNamespaceURI())
                && "div".equals(current.getLocalName())) {
                depth++;
            }
            current = current.getParentNode();
        }
        return Math.max(1, depth);
    }
}
