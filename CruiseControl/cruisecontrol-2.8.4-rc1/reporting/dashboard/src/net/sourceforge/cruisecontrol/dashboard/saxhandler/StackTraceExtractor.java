package net.sourceforge.cruisecontrol.dashboard.saxhandler;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class StackTraceExtractor extends SAXBasedExtractor {

    public static final String KEY_STACKTRACE = "stacktrace.stacktrace";

    public static final String KEY_ERROR = "stacktrace.error";

    private boolean readingBuild;

    private boolean readingStatckTrace;

    private String stackTrace = "";

    private String error = "";

    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        if ("build".equals(qName)) {
            readingBuild = true;
            error = StringUtils.defaultString(attributes.getValue("error"));
        }
        if ("stacktrace".equals(qName)) {
            readingStatckTrace = true;
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (readingBuild && readingStatckTrace) {
            String text = new String(ch, start, length);
            if (StringUtils.isBlank(text)) {
                return;
            }
            stackTrace += text;
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("build".equals(qName)) {
            readingBuild = false;
        }
        if ("stacktrace".equals(qName)) {
            readingStatckTrace = false;
        }
    }

    public void report(Map resultSet) {
        resultSet.put(KEY_STACKTRACE, stackTrace);
        resultSet.put(KEY_ERROR, error);
    }
}
