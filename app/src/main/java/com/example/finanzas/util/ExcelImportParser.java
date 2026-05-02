package com.example.finanzas.util;

import androidx.annotation.NonNull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

public final class ExcelImportParser {

    private ExcelImportParser() { }

    @NonNull
    public static List<Map<String, String>> parseFirstSheet(@NonNull InputStream input) throws Exception {
        Map<String, byte[]> entries = unzip(input);
        List<String> sharedStrings = parseSharedStrings(entries.get("xl/sharedStrings.xml"));
        byte[] sheet = findFirstSheet(entries);
        if (sheet == null) {
            throw new IllegalArgumentException("No se encontro una hoja valida en el Excel.");
        }

        Document doc = parseXml(sheet);
        NodeList rowNodes = doc.getElementsByTagName("row");
        List<Map<String, String>> rawRows = new ArrayList<>();
        for (int i = 0; i < rowNodes.getLength(); i++) {
            Node rowNode = rowNodes.item(i);
            if (!(rowNode instanceof Element)) continue;
            Element row = (Element) rowNode;
            NodeList cells = row.getElementsByTagName("c");
            Map<String, String> values = new LinkedHashMap<>();
            for (int c = 0; c < cells.getLength(); c++) {
                Node cellNode = cells.item(c);
                if (!(cellNode instanceof Element)) continue;
                Element cell = (Element) cellNode;
                String ref = cell.getAttribute("r");
                String column = columnName(ref);
                if (column.isEmpty()) continue;
                values.put(column, readCell(cell, sharedStrings));
            }
            rawRows.add(values);
        }
        return toHeaderRows(rawRows);
    }

    private static Map<String, byte[]> unzip(InputStream input) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int read;
                while ((read = zip.read(buffer)) >= 0) {
                    out.write(buffer, 0, read);
                }
                entries.put(entry.getName(), out.toByteArray());
            }
        }
        return entries;
    }

    private static byte[] findFirstSheet(Map<String, byte[]> entries) {
        byte[] sheet1 = entries.get("xl/worksheets/sheet1.xml");
        if (sheet1 != null) return sheet1;
        return entries.entrySet().stream()
                .filter(e -> e.getKey().startsWith("xl/worksheets/sheet") && e.getKey().endsWith(".xml"))
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static List<Map<String, String>> toHeaderRows(List<Map<String, String>> rawRows) {
        List<Map<String, String>> out = new ArrayList<>();
        Map<String, String> headers = null;
        for (Map<String, String> row : rawRows) {
            if (isEmpty(row)) continue;
            if (headers == null) {
                headers = row;
                continue;
            }
            Map<String, String> mapped = new LinkedHashMap<>();
            for (Map.Entry<String, String> header : headers.entrySet()) {
                String label = header.getValue() == null ? "" : header.getValue().trim();
                if (label.isEmpty()) continue;
                mapped.put(label, row.getOrDefault(header.getKey(), ""));
            }
            if (!isEmpty(mapped)) out.add(mapped);
        }
        return out;
    }

    private static boolean isEmpty(Map<String, String> row) {
        for (String value : row.values()) {
            if (value != null && !value.trim().isEmpty()) return false;
        }
        return true;
    }

    private static List<String> parseSharedStrings(byte[] bytes) throws Exception {
        List<String> out = new ArrayList<>();
        if (bytes == null) return out;
        Document doc = parseXml(bytes);
        NodeList items = doc.getElementsByTagName("si");
        for (int i = 0; i < items.getLength(); i++) {
            Node item = items.item(i);
            StringBuilder text = new StringBuilder();
            NodeList children = ((Element) item).getElementsByTagName("t");
            for (int j = 0; j < children.getLength(); j++) {
                text.append(children.item(j).getTextContent());
            }
            out.add(text.toString());
        }
        return out;
    }

    private static String readCell(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");
        String raw = "";
        NodeList values = cell.getElementsByTagName("v");
        if (values.getLength() > 0) {
            raw = values.item(0).getTextContent();
        } else {
            NodeList inline = cell.getElementsByTagName("t");
            if (inline.getLength() > 0) raw = inline.item(0).getTextContent();
        }
        if ("s".equals(type)) {
            try {
                int index = Integer.parseInt(raw.trim());
                return index >= 0 && index < sharedStrings.size() ? sharedStrings.get(index) : "";
            } catch (NumberFormatException ignored) {
                return "";
            }
        }
        if ("b".equals(type)) {
            return "1".equals(raw) ? "true" : "false";
        }
        return raw == null ? "" : raw.trim();
    }

    private static String columnName(String ref) {
        if (ref == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < ref.length(); i++) {
            char ch = ref.charAt(i);
            if (Character.isLetter(ch)) out.append(Character.toUpperCase(ch));
            else break;
        }
        return out.toString();
    }

    private static Document parseXml(byte[] bytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setExpandEntityReferences(false);
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception ignored) { }
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }
}
