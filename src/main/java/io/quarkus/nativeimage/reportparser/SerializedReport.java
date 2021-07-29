package io.quarkus.nativeimage.reportparser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SerializedReport implements Serializable {
    private static final long serialVersionUID =1;

    MethodDesc[] methods;
    int[][] callers;
    int[][] callees;
    int[][] overrides;
    int[] entryPoints;

    public SerializedReport(ParsedReport report) {
        IdentityHashMap<ReportNode, Integer> positionMap = new IdentityHashMap<>();

        int count = 0;
        methods = new MethodDesc[report.getNodes().size()];
        callers = new int[report.getNodes().size()][];
        callees = new int[report.getNodes().size()][];
        overrides = new int[report.getNodes().size()][];
        for (Map.Entry<MethodDesc, ReportNode> node : report.getNodes().entrySet()) {
            int pos = count++;
            positionMap.put(node.getValue(), pos);
            methods[pos] = node.getKey();
        }
        for (Map.Entry<MethodDesc, ReportNode> node : report.getNodes().entrySet()) {
            int[] callersLocal = new int[node.getValue().callers.size()];
            int[] calleesLocal = new int[node.getValue().callees.size()];
            int[] overridesLocal = new int[node.getValue().overrides.size()];

            for (int i = 0; i < calleesLocal.length; ++i) {
                calleesLocal[i] = positionMap.get(node.getValue().callees.get(i));
            }
            for (int i = 0; i < callersLocal.length; ++i) {
                callersLocal[i] = positionMap.get(node.getValue().callers.get(i));
            }
            for (int i = 0; i < overridesLocal.length; ++i) {
                overridesLocal[i] = positionMap.get(node.getValue().overrides.get(i));
            }
            int pos = positionMap.get(node.getValue());
            overrides[pos] = overridesLocal;
            callees[pos] = calleesLocal;
            callers[pos] = callersLocal;
        }
        entryPoints = new int[report.getEntryPoints().size()];
        int c = 0;
        for (ReportNode e : report.getEntryPoints()) {
            entryPoints[c++] = positionMap.get(e);
        }
    }

    public ParsedReport toReport() {

         final Set<ReportNode> entryPoints = new HashSet<>();
         final Map<MethodDesc, ReportNode> nodes = new HashMap<>();
         final Map<String, List<ReportNode>> nodesByClass = new HashMap<>();
         ReportNode[] nodeList = new ReportNode[methods.length];
        for (int i = 0; i < methods.length; i++) {
            MethodDesc m = methods[i];
            ReportNode reportNode = new ReportNode(m);
            nodeList[i] = reportNode;
            nodes.put(m, reportNode);
        }
        for (int i = 0; i < methods.length; i++) {
            ReportNode node = nodeList[i];
            for (int n : callers[i]) {
                node.callers.add(nodeList[n]);
            }
            for (int n : callees[i]) {
                node.callees.add(nodeList[n]);
            }
            for (int n : overrides[i]) {
                node.overrides.add(nodeList[n]);
            }
        }
        for (ReportNode n : nodeList) {
            List<ReportNode> list = nodesByClass.get(n.method.className);
            if (list == null) {
                nodesByClass.put(n.method.className, list = new ArrayList<>());
            }
            list.add(n);
        }
        for (int pos : this.entryPoints) {
            entryPoints.add(nodeList[pos]);
        }
        return new ParsedReport(entryPoints, nodes, nodesByClass);

    }

}
