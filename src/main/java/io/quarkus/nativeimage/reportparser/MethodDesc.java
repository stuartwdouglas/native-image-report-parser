package io.quarkus.nativeimage.reportparser;

import java.io.Serializable;
import java.util.Objects;

public class MethodDesc implements Serializable {

    private static final long serialVersionUID = -4003415541929464302L;

    final String className;
    final String methodName;
    final String signature;

    MethodDesc(String className, String methodName, String signature) {
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodDesc that = (MethodDesc) o;
        return Objects.equals(className, that.className) && Objects.equals(methodName, that.methodName) && Objects.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, signature);
    }

    @Override
    public String toString() {
        return className + '.'  + methodName + signature;
    }
}
