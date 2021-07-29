package io.quarkus.nativeimage.reportparser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
 class ReportNode implements Serializable {
    final MethodDesc method;
    final List<ReportNode> callers = new ArrayList<>();
    final List<ReportNode> callees = new ArrayList<>();
    final List<ReportNode> overrides = new ArrayList<>();

    ReportNode(MethodDesc method) {
        this.method = method;
    }

     @Override
     public String toString() {
         return "ReportNode{" +
                 "method=" + method +
                 '}';
     }
 }
