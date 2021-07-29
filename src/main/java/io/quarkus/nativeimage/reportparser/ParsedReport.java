package io.quarkus.nativeimage.reportparser;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParsedReport implements Serializable {

    private final Set<ReportNode> entryPoints;
    private final Map<MethodDesc, ReportNode> nodes;
    private final Map<String, List<ReportNode>> nodesByClass;

    public ParsedReport(Set<ReportNode> entryPoints, Map<MethodDesc, ReportNode> nodes, Map<String, List<ReportNode>> nodesByClass) {
        this.entryPoints = entryPoints;
        this.nodes = nodes;
        this.nodesByClass = nodesByClass;
    }

    public Set<ReportNode> getEntryPoints() {
        return entryPoints;
    }

    public Map<MethodDesc, ReportNode> getNodes() {
        return nodes;
    }

    public Map<String, List<ReportNode>> getNodesByClass() {
        return nodesByClass;
    }
}
