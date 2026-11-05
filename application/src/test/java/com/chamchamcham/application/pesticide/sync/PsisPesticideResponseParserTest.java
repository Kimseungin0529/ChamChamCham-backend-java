package com.chamchamcham.application.pesticide.sync;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class PsisPesticideResponseParserTest {
    private final PsisPesticideResponseParser parser = new PsisPesticideResponseParser();

    @Test
    @DisplayName("parses repeated item elements into tag-to-text maps")
    void parses_repeated_item_elements_into_tag_to_text_maps() throws Exception {
        var xml = "<response>\n  <body>\n    <items>\n      <item>\n        <cropNm>감자</cropNm>\n        <aplyPestNm>역병</aplyPestNm>\n        <prdtNm>만코제브 수화제</prdtNm>\n      </item>\n      <item>\n        <cropNm>강낭콩</cropNm>\n        <aplyPestNm>탄저병</aplyPestNm>\n        <prdtNm>만코제브 수화제</prdtNm>\n      </item>\n    </items>\n  </body>\n</response>";
        var rows = parser.parse(xml);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("cropNm", "감자").containsEntry("aplyPestNm", "역병");
        assertThat(rows.get(1)).containsEntry("cropNm", "강낭콩").containsEntry("aplyPestNm", "탄저병");
    }

    @Test
    @DisplayName("returns empty list when no item elements exist")
    void returns_empty_list_when_no_item_elements_exist() throws Exception {
        var xml = "<response><body><items/></body></response>";
        assertThat(parser.parse(xml)).isEmpty();
    }

    @Test
    @DisplayName("does not resolve external doctype entities")
    void does_not_resolve_external_doctype_entities() throws Exception {
        var xml = "<?xml version=\"1.0\"?><!DOCTYPE items [<!ENTITY xxe \"boom\">]><response/>";
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> parser.parse(xml));
    }
}
