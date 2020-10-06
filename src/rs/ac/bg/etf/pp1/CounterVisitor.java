package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;

public class CounterVisitor extends VisitorAdaptor {
    protected int count;

    public int getCount() {
        return count;
    }

    public static class FormalParamaterCounter extends CounterVisitor {
        public void visit(ParameterSingle parameter) {
            count++;
        }
        public void visit(ParameterArr parameter) {
            count++;
        }
    }

    public static class LocalVariableCounter extends CounterVisitor {
        public void visit(LocalVarDeclSinge var) {
            count++;
        }
        public void visit(LocalVarDeclArray var) {
            count++;
        }
    }
}
