package com.chamchamcham.application.pesticide.sync;

import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.*;

/**
 * 데이터포털/PSIS류 XML 응답에서 반복되는 <item> 엘리먼트를 태그명 -> 텍스트값 맵으로 평탄화한다.
 * 실제 태그명(예: cropNm, aplyPestNm 등)은 API 키 발급 후 실응답으로 확정하고
 * [PsisPesticideRowMapper]에서만 매핑하면 되도록, 이 파서는 특정 필드명을 알 필요가 없게 만든다.
 */
@Component
public class PsisPesticideResponseParser {
    public List<Map<String, String>> parse(String xml) throws Exception {
        Objects.requireNonNull(xml, "xml");
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        var itemNodes = document.getElementsByTagName("item");
        var result = new ArrayList<Map<String, String>>(itemNodes.getLength());
        for (int index = 0; index < itemNodes.getLength(); index++) {
            var itemElement = (Element) itemNodes.item(index);
            var children = itemElement.getChildNodes();
            var row = new LinkedHashMap<String, String>();
            for (int i = 0; i < children.getLength(); i++) if (children.item(i) instanceof Element child) row.put(child.getTagName(), trim(child.getTextContent()));
            result.add(row);
        }
        return result;
    }
    private static String trim(String value) {
        int start = 0, end = value.length();
        while (start < end && whitespace(value.charAt(start))) start++;
        while (end > start && whitespace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }
    private static boolean whitespace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }
}
