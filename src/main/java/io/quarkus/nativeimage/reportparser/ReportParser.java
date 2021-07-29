package io.quarkus.nativeimage.reportparser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReportParser {

    private static final Map<String, LineType> LINE_TYPE_MAP;

    static {
        Map<String, LineType> lineTypes = new HashMap<>();
        lineTypes.put("is overridden by ", LineType.OVERRIDDEN_BY);
        lineTypes.put("virtually calls ", LineType.VIRTUALLY_CALLS);
        lineTypes.put("directly calls ", LineType.DIRECTLY_CALLS);
        lineTypes.put("entry ", LineType.ENTRY_POINT);
        LINE_TYPE_MAP = Collections.unmodifiableMap(lineTypes);
    }

    public static ParsedReport parse(InputStream reportFile) throws IOException {
        final Set<ReportNode> entryPoints = new HashSet<>();
        final Map<MethodDesc, ReportNode> nodes = new HashMap<>();
        final Map<String, List<ReportNode>> nodesByClass = new HashMap<>();
        StringCache internMap = new StringCache();
        Deque<ReportNode> current = new ArrayDeque<>();
        Deque<Integer> depths = new ArrayDeque<>();
        int lineNo = 0;
        try (reportFile) {
            //these reports are huge
            //don't try and load everything into memory at once
            ByteBuffer buffer = ByteBuffer.allocate(10000);
            String stringLine = null;
            ByteArrayOutputStream stringBuffer = new ByteArrayOutputStream();
            boolean first = true;
            while ((stringLine = readLine(reportFile, buffer, stringBuffer)) != null) {
                lineNo++;
                if (first) {
                    first = false;
                    continue;
                }
                if (stringLine.isEmpty()) {
                    continue;
                }
                Line line = parseLine(stringLine, internMap);
                ReportNode node = nodes.get(line.methodDesc);
                if (node == null) {
                    nodes.put(line.methodDesc, node = new ReportNode(line.methodDesc));
                    List<ReportNode> byClass = nodesByClass.get(line.methodDesc.className);
                    if (byClass == null) {
                        nodesByClass.put(line.methodDesc.className, byClass = new ArrayList<>());
                    }
                    byClass.add(node);
                }
                if (line.type == LineType.ENTRY_POINT) {
                    current.clear();
                    depths.clear();
                    entryPoints.add(node);
                    current.push(node);
                    depths.push(line.depth);
                } else {
                    while (depths.peek() >= line.depth) {
                        if (current.size() == 1) {
                            throw new RuntimeException("Cannot parse report: can't understand structure at line " + lineNo);
                        }
                        depths.pop();
                        current.pop();
                    }
                    ReportNode parent = current.peek();
                    parent.callees.add(node);
                    if (line.type == LineType.OVERRIDDEN_BY) {
                        node.overrides.add(parent);
                    } else {
                        node.callers.add(parent);
                    }
                    depths.push(line.depth);
                    current.push(node);
                }

            }
        }
        return new ParsedReport(entryPoints,nodes, nodesByClass);
    }

    static Line parseLine(String stringLine, StringCache internMap) {
        int indentCount = 0;
        int parseState = 0;
        LineType type = null;
        String className = null;
        String methodName = null;
        String signature = null;
        boolean done = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stringLine.length() && !done; ++i) {
            char c = stringLine.charAt(i);
            switch (parseState) {
                case 0: {
                    if (c == '│' || c == ' ') {
                        indentCount++;
                        break;
                    }
                    //fall through
                    parseState = 1;
                }
                case 1: {
                    if (c == '└' || c == '-' || c == '├' || c == ' ' || c == '─') {
                        break;
                    }
                    //fall through
                    parseState = 2;
                }
                case 2: {
                    if (c == 'i') {
                        type = LineType.OVERRIDDEN_BY;
                        i+=("is overridden by".length() );
                    } else if (c == 'v') {
                        type= LineType.VIRTUALLY_CALLS;
                        i+=("virtually calls".length() );
                    } else if (c == 'd') {
                        type= LineType.DIRECTLY_CALLS;
                        i+=("directly calls".length() );
                    } else if (c == 'e') {
                        type= LineType.ENTRY_POINT;
                        i+=("entry".length() );
                    } else {
                        throw new RuntimeException("Unknown type " + stringLine);
                    }
                        parseState = 3;
                    break;
                }
                case 3: {
                    if (c != '(') {
                        sb.append(c);
                        break;
                    }
                    String full = sb.toString();
                    int endIndex = full.lastIndexOf('.');
                    className = full.substring(0, endIndex);
                    methodName = full.substring(endIndex + 1);
                    sb.setLength(0);
                    parseState = 4;
                }
                case 4: {
                    sb.append(c);
                    if (c == ')') {
                        parseState = 5;
                    }
                    break;
                }
                case 5: {
                    if (c == ' ') {
                        signature = sb.toString();
                        done = true;
                    } else {
                        sb.append(c);
                    }
                    break;
                }
            }
        }
        if (parseState == 2) {
            throw new RuntimeException("Unknown line type " + stringLine);
        }
        return new Line(type, indentCount, new MethodDesc(internMap.intern(className ), internMap.intern(methodName), internMap.intern(signature)), null);
    }

    private static String readLine(InputStream reportFile, ByteBuffer buffer, ByteArrayOutputStream stringBuffer) throws IOException {
        stringBuffer.reset();
        for (; ; ) {
            while (buffer.hasRemaining()) {
                byte data = buffer.get();
                if (data == '\n') {
                    return stringBuffer.toString(StandardCharsets.UTF_8);
                } else {
                    stringBuffer.write(data);
                }
            }
            int read = reportFile.read(buffer.array());
            if (read == -1) {
                return null;
            }
            buffer.position(0);
            buffer.limit(read);
        }
    }

    static class Line {
        final LineType type;
        final int depth;
        final MethodDesc methodDesc;
        final String idRef;

        Line(LineType type, int depth, MethodDesc methodDesc, String idRef) {
            this.type = type;
            this.depth = depth;
            this.methodDesc = methodDesc;
            this.idRef = idRef;
        }
    }

    enum LineType {
        ENTRY_POINT,
        OVERRIDDEN_BY,
        DIRECTLY_CALLS,
        VIRTUALLY_CALLS
    }

}
