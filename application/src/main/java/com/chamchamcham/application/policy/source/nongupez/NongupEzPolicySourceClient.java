package com.chamchamcham.application.policy.source.nongupez;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.IOException;

@Component
public class NongupEzPolicySourceClient {
    private static final String CONDITIONS_PATH = "/nsm/bizAply/wholeBiz/retrieveListBizSrchCnd";
    private static final String LIST_PATH = "/nsm/bizAply/wholeBiz/retrieveListBizSrch";
    private static final String DETAIL_PATH = "/nsm/bizAply/cstBiz/findBizSrchDtl";
    private final NongupEzHttpTransport transport;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter basicDate = DateTimeFormatter.BASIC_ISO_DATE;
    public NongupEzPolicySourceClient(NongupEzHttpTransport transport) { this.transport = Objects.requireNonNull(transport, "transport"); }
    public String detectLatestYear() throws IOException, InterruptedException {
        var root = objectMapper.readTree(transport.post(CONDITIONS_PATH, Map.of()));
        throwIfBusinessError(root, CONDITIONS_PATH);
        var years = new ArrayList<String>();
        for (var node : root.path("dsBizSrchCndList")) if (node.path("srchCndKywd").asText().equals("SRCH_BIZ_YR")) {
            String value = node.path("srchCndDtlCd").asText(null);
            if (value != null) years.add(value);
        }
        if (years.isEmpty()) for (var node : root.path("result").path("SRCH_BIZ_YR")) {
            String value = node.path("cd").asText(null);
            if (value != null) years.add(value);
        }
        return years.stream().filter(year -> !year.equals("0000") && year.length() == 4 && digits(year)).max(String::compareTo)
            .orElseThrow(() -> new IllegalStateException("NongupEZ business year condition is empty"));
    }
    public List<NongupEzPolicyListItem> fetchPrograms(String year) throws IOException, InterruptedException {
        Objects.requireNonNull(year, "year");
        var items = new ArrayList<NongupEzPolicyListItem>();
        int pageIndex = 1, totalCount = Integer.MAX_VALUE;
        while (items.size() < totalCount) {
            var condition = new LinkedHashMap<String, Object>();
            condition.put("srchBizYr", year);
            condition.put("sortCnd", "M");
            var paging = new LinkedHashMap<String, Object>();
            paging.put("curPage", pageIndex);
            paging.put("pageSize", 1000);
            var body = new LinkedHashMap<String, Object>();
            body.put("srchCnd", condition);
            body.put("paging", paging);
            var root = objectMapper.readTree(transport.post(LIST_PATH, body));
            throwIfBusinessError(root, LIST_PATH);
            var pageNodes = root.path("bizList");
            totalCount = root.path("paging").path("totalPage").asInt(root.path("paging").path("pageCount").asInt(pageNodes.size()));
            var pageItems = new ArrayList<NongupEzPolicyListItem>();
            for (var node : pageNodes) pageItems.add(toListItem(node, year));
            if (pageItems.isEmpty()) break;
            items.addAll(pageItems);
            pageIndex++;
        }
        return items;
    }
    public NongupEzPolicyDetail fetchDetail(String externalId, String year) throws IOException, InterruptedException {
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(year, "year");
        var body = new LinkedHashMap<String, Object>();
        body.put("afbzCd", externalId);
        body.put("bizYr", year);
        var root = objectMapper.readTree(transport.post(DETAIL_PATH, body));
        throwIfBusinessError(root, DETAIL_PATH);
        var detail = root.path("bizDtl");
        if (detail.isMissingNode() || detail.isNull()) detail = root.path("result");
        var contacts = new ArrayList<NongupEzPolicyContact>();
        for (var contact : root.path("bizPicList")) contacts.add(new NongupEzPolicyContact(
            firstText(contact, "bizTkcgInstCdNm", "instNm"), firstText(contact, "bizTkcgDeptInstCdNm", "deptNm"), firstText(contact, "bizPicTelno", "telno")));
        var attachments = new ArrayList<NongupEzPolicyAttachment>();
        for (var file : root.path("bizAtchFileList")) attachments.add(new NongupEzPolicyAttachment(
            firstText(file, "originalName", "atchFileNm"), firstText(file, "extension", "fileExtnNm"), size(file), nullableText(file, "url")));
        return new NongupEzPolicyDetail(fallback(text(detail, "afbzCd"), externalId), fallback(text(detail, "bizYr"), year), text(detail, "afbzNm"),
            nullableText(detail, "bizPrpsCn"), nullableText(detail, "bizCn"), nullableText(detail, "bizSprtQlfcCn"), nullableText(detail, "bizSprtCn"),
            localDate(detail, "bizAplyBgngYmd"), localDate(detail, "bizAplyEndYmd"), nullableText(detail, "bizAplyMthdCn"), nullableText(detail, "bizRqdcCn"),
            nullableText(detail, "bizSlctnCrtrCn"), Objects.requireNonNullElse(nullableText(detail, "bizTkcgInstNm"), "농업e지"), contacts, attachments, objectMapper.writeValueAsString(root));
    }
    private NongupEzPolicyListItem toListItem(JsonNode item, String year) throws IOException {
        String agency = firstText(item, "bizAtrbDtlNm", "bizTkcgInstNm");
        return new NongupEzPolicyListItem(text(item, "afbzCd"), fallback(text(item, "bizYr"), year), text(item, "afbzNm"), nullableText(item, "bizCn"),
            agency == null ? "농업e지" : agency, localDate(item, "bizAplyBgngYmd"), localDate(item, "bizAplyEndYmd"), objectMapper.writeValueAsString(item));
    }
    private String text(JsonNode node, String field) { return Objects.requireNonNullElse(nullableText(node, field), ""); }
    private String nullableText(JsonNode node, String field) {
        var value = node.path(field);
        if (value.isMissingNode() || value.isNull()) return null;
        String text = trim(value.asText());
        return text.isEmpty() ? null : text;
    }
    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) { String value = nullableText(node, field); if (value != null) return value; }
        return null;
    }
    private Long size(JsonNode file) {
        if (file.path("size").isNumber()) return file.path("size").asLong();
        return file.path("fileSz").isNumber() ? file.path("fileSz").asLong() : null;
    }
    private LocalDate localDate(JsonNode node, String field) {
        String value = nullableText(node, field);
        return value != null && value.length() == 8 && digits(value) ? LocalDate.parse(value, basicDate) : null;
    }
    private static boolean digits(String value) {
        for (int i = 0; i < value.length(); i++) if (!Character.isDigit(value.charAt(i))) return false;
        return true;
    }
    private String fallback(String value, String fallback) { return trim(value).isEmpty() ? fallback : value; }
    private void throwIfBusinessError(JsonNode root, String path) {
        var message = root.path("message");
        if (!message.isMissingNode() && !message.isNull() && message.path("status").asText().equalsIgnoreCase("ERROR"))
            throw new IllegalStateException(trim("NongupEZ business error for " + path + ": " + message.path("code").asText("") + " " + message.path("content").asText("")));
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
