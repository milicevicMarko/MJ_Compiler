package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

import java.util.ArrayList;
import java.util.Stack;

public class CodeGenerator extends VisitorAdaptor {
    private int mainPc;
    private boolean isNegative = false;

    private Obj currentMethod;
    private Obj currentReturn;

    public CodeGenerator() {
        initializeHelpMethods("ord", Code.load_n);
        initializeHelpMethods("chr", Code.load_n);
        initializeHelpMethods("len", Code.arraylength);
    }

    private void initializeHelpMethods(String name, int size) {
        Obj ordMethod = Tab.find(name);
        ordMethod.setAdr(Code.pc);
        enterMethod(1, 1);
        Code.put(size);
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    private void enterMethod(int formal, int formalAndLocal) {
        Code.put(Code.enter);
        Code.put(formal);
        Code.put(formalAndLocal);
    }

    int getMainPc() {
        return mainPc;
    }


    @Override
    public void visit(MethodSignature methodSignature) {
        if (methodSignature.getMethodName().equalsIgnoreCase("main")) {
            mainPc = Code.pc;
        }
        methodSignature.obj.setAdr(Code.pc);
        SyntaxNode method = methodSignature.getParent();

        CounterVisitor.FormalParamaterCounter formalCount = new CounterVisitor.FormalParamaterCounter();
        method.traverseTopDown(formalCount);

        CounterVisitor.LocalVariableCounter localVarCount = new CounterVisitor.LocalVariableCounter();
        method.traverseTopDown(localVarCount);

        currentMethod = methodSignature.obj;

        enterMethod(formalCount.getCount(), formalCount.getCount() + localVarCount.getCount() + 1);
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration) {
        if (!needsReturn() && !hasReturn()) {
            Code.put(Code.exit);
            Code.put(Code.return_);
        }
        checkRuntimeError();
    }

    private boolean hasReturn() {
        return currentReturn != null;
    }

    private boolean needsReturn() {
        return !TabAdapter.isNoType(currentMethod.getType());
    }

    private void checkRuntimeError() {
        if (!needsReturn() && hasReturn() || needsReturn() && !hasReturn()) {
            throwRuntimeError();
        }
        currentReturn = null;
        currentMethod = null;
    }

    private void throwRuntimeError() {
        Code.loadConst(1);
        Code.put(Code.trap);
    }

    @Override
    public void visit(StatementReturn statementReturn) {
        returnStatement(null);
    }

    @Override
    public void visit(StatementReturnExpr returnExpr) {
        returnStatement(returnExpr.getExpr().obj);
    }

    private void returnStatement(Obj obj) {
        Code.put(Code.exit);
        Code.put(Code.return_);
        currentReturn = null;
    }

    @Override
    public void visit(FactorConst constant) {
        Code.load(constant.obj);
    }

    @Override
    public void visit(FactorDesignator factorDesignator) {
        Code.load(factorDesignator.obj);
    }


    @Override
    public void visit(DesignatorArray designatorArray) {
        memoryExpr = designatorArray.getExpr();
    }

    @Override
    public void visit(DesignatorArrayHelper arrayHelper) {
        Code.load(arrayHelper.obj);
    }

    private Designator memoryDesignator;
    private Expr memoryExpr;

    @Override
    public void visit(DesignatorSingle designatorSingle) {
        memoryDesignator = designatorSingle;
        memoryExpr = null;
    }

    @Override
    public void visit(DesignatorStatementAssignOpExpr statementAssign) {
        if (statementAssign.getAssingOp().getClass() == AssignEqual.class) {
            Code.store(statementAssign.getDesignator().obj);
        }
    }

    @Override
    public void visit(DesignatorStatementIncrement increment) {
        postcrementDesignator(increment.getDesignator(), true);
    }

    @Override
    public void visit(DesignatorStatementDecrement decrement) {
        postcrementDesignator(decrement.getDesignator(), false);
    }

    private void postcrementDesignator(Designator designator, boolean isIncrement) {
        int op = isIncrement? Code.add : Code.sub;
        duplicate2IfArray(designator);
        Code.load(designator.obj);
        Code.loadConst(1);
        Code.put(op);
        Code.store(designator.obj);
    }

    private void duplicate2IfArray(Designator designator) {
        if (designator.getClass() == DesignatorArray.class) {
            Code.put(Code.dup2);
        }
    }

    @Override
    public void visit(ExprTermAddOpTerm exprTermAddOpTerm) {
        if (exprTermAddOpTerm.getAddop() instanceof Addop_AddopRight) {
            Code.put(Code.dup_x1);
            Code.put(Code.pop);
            Code.put(Code.pop);
        } else {
            putAddopLeft((Addop_AddopLeft) exprTermAddOpTerm.getAddop());
        }
    }

    Stack<Designator> designatorStack = new Stack<>(); // either normal or array
    Stack<Expr> exprStack = new Stack<>(); // array index or null
    Stack<Integer> combinedOpStack = new Stack<>(); // op

    @Override
    public void visit(LoadDesignator1 loadDesignator1) {
        if (memoryDesignator != null) {
            designatorStack.push(memoryDesignator);
            if (!TabAdapter.isArrayType(memoryDesignator.obj.getType())) {
                exprStack.push(null);
            } else {
                exprStack.push(memoryExpr);
            }
            memoryDesignator = null;
            memoryExpr = null;
        }
    }

    @Override
    public void visit(AddopRightPlusEqual addopRight) {
        combinedOpStack.push(Code.add);
    }

    @Override
    public void visit(AddopRightMinusEqual addopRight) {
        combinedOpStack.push(Code.sub);
    }

    @Override
    public void visit(MulopRightMulEqual mulopRight) {
        combinedOpStack.push(Code.mul);
    }

    @Override
    public void visit(MulopRightDivEqual mulopRight) {
        combinedOpStack.push(Code.div);
    }

    @Override
    public void visit(MulopRightModEqual mulopRight) {
        combinedOpStack.push(Code.rem);
    }

    // there was a mistake in the text of the problem, this was necessary
    // expr -> array index
    // designator -> regular or array
    // if expr == null -> designator is regular
    @Override
    public void visit(EndExprStack endExprStack) {
        // result on stack
        while (!combinedOpStack.empty()) {
            Designator designator = designatorStack.pop();
            Expr expr = exprStack.pop();
            Integer op = combinedOpStack.pop();
            Code.load(designator.obj);
            if (expr != null) { // is array[i] ?
                stack_swap();
                Code.load(expr.obj);
                stack_swap();
                Code.load(designator.obj);
                Code.load(expr.obj);
                Code.put(Code.aload);
                fixIfWrongWayOp(op);
                Code.put(op);
                Code.put(Code.dup_x2);
                Code.put(Code.astore);
            } else {
                fixIfWrongWayOp(op);
                Code.put(op);
                Code.put(Code.dup);
                Code.store(designator.obj);
            }
        }
    }

    private void fixIfWrongWayOp(int op) {
        if (op != Code.add && op != Code.mul) {
            stack_swap();
        }
    }

    public void visit(TermMulOpFactor termMulOpFactor) {
        if (termMulOpFactor.getMulop() instanceof Mulop_MulopRight) {
            Code.put(Code.dup_x1);
            Code.put(Code.pop);
            Code.put(Code.pop);
        } else {
            putMullopLeft((Mulop_MulopLeft) termMulOpFactor.getMulop());
        }
    }

    private void putAddopLeft(Addop_AddopLeft addop_addopLeft) {
        int codeOp = getAddopLeft(addop_addopLeft);
        Code.put(codeOp);
    }

    private void putMullopLeft(Mulop_MulopLeft mulopLeft) {
        int codeOp = getMullopLeft(mulopLeft);
        Code.put(codeOp);
    }

    private int getAddopLeft(Addop_AddopLeft addop_addopLeft) {
        if (addop_addopLeft.getAddopLeft().getClass() == AddopLeftPlus.class) {
            return Code.add;
        } else {
            return Code.sub;
        }
    }

    private int getMullopLeft(Mulop_MulopLeft mulopLeft) {
        if (mulopLeft.getMulopLeft().getClass() == MulopLeftMul.class) {
            return Code.mul;
        } else if (mulopLeft.getMulopLeft().getClass() == MulopLeftDiv.class) {
            return Code.div;
        } else {
            return Code.rem;
        }
    }

    private int getMulOpRightCode(Mulop_MulopRight mulopLeft) {
        if (mulopLeft.getMulopRight().getClass() == MulopRightMulEqual.class) {
            return Code.mul;
        } else if (mulopLeft.getMulopRight().getClass() == MulopRightDivEqual.class) {
            return Code.div;
        } else {
            return Code.rem;
        }
    }

    private int getAddOpRightCode(Addop_AddopRight addopRight) {
        if (addopRight.getAddopRight().getClass() == AddopRightPlusEqual.class) {
            return Code.add;
        }  else {
            return Code.sub;
        }
    }

    @Override
    public void visit(NegativeTrue negativeTrue) {
        isNegative = true;
    }

    @Override
    public void visit(ExprTerm exprTerm) {
        if (isNegative) {
            Code.put(Code.neg);
        }
        isNegative = false;
    }

    @Override
    public void visit(FactorNewTypeArray newTypeArray) {
        boolean isChar = TabAdapter.isCharType(newTypeArray.obj.getType().getElemType());
        Obj expr = newTypeArray.getExpr().obj;
        stack_create_array_given_size(expr, isChar);
    }


    // IF

    private final Stack<ArrayList<Integer>> fixupAdrStack = new Stack<>();
    private final Stack<ArrayList<Integer>> fixupPositiveAdrStack = new Stack<>();
    private final Stack<Integer> postElseFixupAdrStack = new Stack<>();
    private final Stack<Integer> relOpStack = new Stack<>();

    @Override
    public void visit(ConditionFactExpr exprTrueFalse) {
        Code.loadConst(1);
        int op = Code.eq;
        relOpStack.push(op);
    }

    @Override
    public void visit(StatementIf statementIf) {
        // lastly
        fixupAdrStack.pop();
        fixupPositiveAdrStack.pop();
    }

    @Override
    public void visit(PrepareCondition prepareIf) {
        fixupAdrStack.push(new ArrayList<>());
        fixupPositiveAdrStack.push(new ArrayList<>());
    }

    @Override
    public void visit(FinalizeCondition ifKeyword) {
        int op = relOpStack.pop();
        Code.putFalseJump(op, 0);
        int adr = Code.pc - 2;
        fixupAdrStack.peek().add(adr);
    }

    @Override
    public void visit(AndKeyword andKeyword) {
        int op = relOpStack.pop();
        Code.putFalseJump(op, 0);
        int adr = Code.pc - 2;
        fixupAdrStack.peek().add(adr);
    }

    @Override
    public void visit(OrKeyword orKeyword) {
        // needs to jump to ifs 1st statement if positive!  (double negative)
        //needs new stack for jump
        int op = relOpStack.pop();
        int positiveOp = Code.inverse[op];
        Code.putFalseJump(positiveOp, 0);
        int adr = Code.pc - 2;
        fixupPositiveAdrStack.peek().add(adr);

        for (Integer adrJump : fixupAdrStack.peek()) {
            Code.fixup(adrJump);
        }
        fixupAdrStack.peek().clear();
    }

    @Override
    public void visit(StatementAdr statementAdr) {
        for (Integer putBackAdr : fixupPositiveAdrStack.peek()) {
            Code.fixup(putBackAdr);
        }
        fixupPositiveAdrStack.peek().clear();
    }

    @Override
    public void visit(OptionalElseFalse elseFalse) {
        for (Integer putBackAdr : fixupAdrStack.peek()) {
            Code.fixup(putBackAdr);
        }
        fixupAdrStack.peek().clear();
    }

    @Override
    public void visit(ElseKeyword elseKeyword) {
        Code.putJump(0);
        for (Integer putBackAdr : fixupAdrStack.peek()) {
            Code.fixup(putBackAdr);
        }
        fixupAdrStack.peek().clear();

        int elsePc = Code.pc - 2;
        postElseFixupAdrStack.push(elsePc);
    }

    @Override
    public void visit(OptionalElseTrue elseTrue) {
        int elseFixupAdr = postElseFixupAdrStack.pop();
        Code.fixup(elseFixupAdr);
    }

    @Override
    public void visit(RelOpEq eq) {
        relOpStack.push(Code.eq);
    }
    @Override
    public void visit(RelOpNeq neq) {
        relOpStack.push(Code.ne);
    }
    @Override
    public void visit(RelOpGt eq) {
        relOpStack.push(Code.gt);
    }
    @Override
    public void visit(RelOpGte neq) {
        relOpStack.push(Code.ge);
    }
    @Override
    public void visit(RelOpLt eq) {
        relOpStack.push(Code.lt);
    }
    @Override
    public void visit(RelOpLte neq) {
        relOpStack.push(Code.le);
    }

    // FOREACH

    private final Stack<Integer> foreachAdrRepeatTop = new Stack<>();
    private final Stack<ArrayList<Integer>> breakFixup = new Stack<>();
    private final Stack<ArrayList<Integer>> continueFixup = new Stack<>();
    private final Stack<Obj> foreachIteratorObj = new Stack<>();


    @Override
    public void visit(ForeachStart foreachStart) {
        fixupAdrStack.push(new ArrayList<>());
        breakFixup.push(new ArrayList<>());
        continueFixup.push(new ArrayList<>());
    }

    @Override
    public void visit(ForeachIterator iterator) {
        Obj iteratorObj = iterator.obj;
        foreachIteratorObj.push(iteratorObj);
    }

    @Override
    public void visit(ForeachHiddenIterator hiddenIterator) {
        Code.loadConst(0);
    }

    @Override
    public void visit(ForeachArrayDesignator arrayDesignator) {
        int top = Code.pc;
        foreachAdrRepeatTop.push(top);
        Code.put(Code.dup); // iterator_index dup
        Obj array = arrayDesignator.obj;
        Code.load(array);
        Code.put(Code.arraylength);
        Code.putFalseJump(Code.ne, 0);
        int adr = Code.pc - 2;
        fixupAdrStack.peek().add(adr);

        Obj iteratorObj = foreachIteratorObj.peek();
        Code.put(Code.dup); // ind, ind
        Code.load(array); // ind, ind, arr
        Code.put(Code.dup_x1); // ind, arr, ind, arr
        Code.put(Code.pop); // ind, arr, ind
        Code.put(Code.aload); // ind
        Code.store(iteratorObj);
    }

    @Override
    public void visit(ForeachBegin foreachBegin) {
        // todo : is this used?
    }

    @Override
    public void visit(ForeachEnd foreachEnd) {
        if (!continueFixup.empty()) {
            for (Integer continueAdr : continueFixup.peek()) {
                Code.fixup(continueAdr);
            }
            continueFixup.pop();
        }

        Code.loadConst(1);
        Code.put(Code.add); // leave on stack

        int top = foreachAdrRepeatTop.pop();
        Code.putJump(top);
    }

    @Override
    public void visit(ForeachExit foreachExit) {
        for (Integer adrFixup : fixupAdrStack.peek()) {
            Code.fixup(adrFixup);
        }
        if (!breakFixup.empty()) {
            for(Integer breakAdr : breakFixup.peek()) {
                Code.fixup(breakAdr);
            }
            breakFixup.pop();
        }
        foreachIteratorObj.pop();
        fixupAdrStack.pop();
        Code.put(Code.pop);
    }

    // FOR

    private final Stack<Integer> forJumpToEnd = new Stack<>();
    private final Stack<Integer> forDesignatorJumpBackAdr = new Stack<>();

    @Override
    public void visit(ForStart forStart) {
        breakFixup.push(new ArrayList<>());
        continueFixup.push(new ArrayList<>());
    }

    @Override
    public void visit(ForRepeatCondition topCondition) {
        int top = Code.pc;
        foreachAdrRepeatTop.push(top);
    }

    @Override
    public void visit(ForConditionEmpty conditionEmpty) {
        fixupAdrStack.push(new ArrayList<>());
        fixupPositiveAdrStack.push(new ArrayList<>());
        Code.putJump(0);
        int adr = Code.pc - 2;
        fixupPositiveAdrStack.peek().add(adr);

    }

    @Override
    public void visit(DesignatorJumpBack designatorJumpBack) {
        int pc = Code.pc;
        forDesignatorJumpBackAdr.push(pc);
    }

    @Override
    public void visit(JumpToDesignatorStatement2 jumpToDesignator2) {
        if (!continueFixup.empty()) {
            for (Integer continueAdr : continueFixup.peek()) {
                Code.fixup(continueAdr);
            }
            continueFixup.pop();
        }
        int pcDesignator2 = forDesignatorJumpBackAdr.pop();
        Code.putJump(pcDesignator2);
    }

    @Override
    public void visit(JumpToEnd jumpToEnd) {
        Code.putJump(0);
        int adr = Code.pc - 2;
        forJumpToEnd.push(adr);
    }

    @Override
    public void visit(ForEnd forEnd) {
        // jump to des2
        int pcForEnd = forJumpToEnd.pop();
        Code.fixup(pcForEnd);

        int top = foreachAdrRepeatTop.peek();
        Code.putJump(top);
    }

    @Override
    public void visit(ForExit forExit) {
        for (Integer putBackAdr : fixupAdrStack.peek()) {
            Code.fixup(putBackAdr);
        }
        fixupAdrStack.peek().clear();
        if (!breakFixup.empty()) {
            for(Integer breakAdr : breakFixup.peek()) {
                Code.fixup(breakAdr);
            }
            breakFixup.pop();
        }
        fixupAdrStack.pop();
        fixupPositiveAdrStack.pop();
        foreachAdrRepeatTop.pop();
    }

    @Override
    public void visit(JumpToStatements jumpToStatements) {
        Code.putJump(0);
        int adr = Code.pc - 2;
        fixupPositiveAdrStack.peek().add(adr);
    }

    // CONTINUE AND BREAK

    @Override
    public void visit(StatementContinue statementContinue) {
        Code.putJump(0);
        int continueAdr = Code.pc-2;
        continueFixup.peek().add(continueAdr);
    }

    @Override
    public void visit(StatementBreak statementBreak) {
        // jump out
        Code.putJump(0);
        int breakAdr = Code.pc - 2;
        breakFixup.peek().add(breakAdr);
    }

    // PRINT READ

    @Override
    public void visit(StatementPrintExpr printExpr) {
        if (TabAdapter.isCharType(printExpr.getExpr().obj.getType())) {
            Code.loadConst(1);
            Code.put(Code.bprint);
        } else {
            Code.loadConst(5);
            Code.put(Code.print);
        }
    }

    @Override
    public void visit(StatementPrintExprNumber printExpr) {
        Code.loadConst(printExpr.getN2());
        if (TabAdapter.isCharType(printExpr.getExpr().obj.getType())) {
            Code.put(Code.bprint);
        } else {
            Code.put(Code.print);
        }
    }

    @Override
    public void visit(StatementRead statementRead) {
        Obj obj = statementRead.getDesignator().obj;
        if (TabAdapter.isCharType(obj.getType())) {
            Code.put(Code.bread);
        } else {
            Code.put(Code.read);
        }
        Code.store(obj);
    }


    // FUNCTIONS


    @Override
    public void visit(FunCall funCall) {
        Obj function = funCall.obj;
        int offset = function.getAdr() - Code.pc;
        Code.put(Code.call);
        Code.put2(offset);
    }


    // STACK HELPERS
    // Intentionally written in snake_case to be easier to notice and differentiate
    // Should be used for quick modifications, eg. in exam
    // todo: Use them in the whole class

    private final Stack<Integer> stack_if_adr = new Stack<>();
    private final Stack<Integer> stack_cycle_adr = new Stack<>();

    private void stack_if(int op) {
        // expects 2 operands on stack
        int opposite_op = Code.inverse[op];
        Code.putFalseJump(opposite_op, 0);
        int jump_out = Code.pc - 2;
        stack_if_adr.push(jump_out);
    }

    private void stack_if_fixup() {
        int jump_fixup = stack_if_adr.pop();
        Code.fixup(jump_fixup);
    }

    private void stack_array_length(Obj arr) {
        Code.load(arr);
        Code.put(Code.arraylength);
    }

    public void stack_increment() {
        Code.loadConst(1);
        Code.put(Code.add);
    }

    public void stack_decrement() {
        Code.loadConst(1);
        Code.put(Code.sub);
    }

    private void stack_swap() {
        Code.put(Code.dup_x1);
        Code.put(Code.pop);
    }

    private void stack_start_cycle() {
        int cycle_start_adr = Code.pc;
        stack_cycle_adr.push(cycle_start_adr);
    }

    private void stack_cycle_fixup() {
        int cycle_fixup = stack_cycle_adr.pop();
        Code.putJump(cycle_fixup);
    }
    private void stack_create_array_given_size(Obj size, boolean isChar) {
        Code.load(size);
        Code.put(Code.newarray);
        if (isChar) {
            Code.put(0);
        } else {
            Code.put(1);
        }
    }

    private void stack_clean_by(int by) {
        for(int i=0; i < by; i++) {
            Code.put(Code.pop);
        }
    }

    private void stack_debug() {
        Code.loadConst(1234);
        Code.loadConst(5);
        Code.put(Code.print);
        // <arg value="-debug"/>
    }


    // Exam test1: Find the max in an array i think
    @Override
    public void visit(ExprMaxArray array) {
        Code.put(Code.pop);
        Code.loadConst(-100); // [max]
        Code.loadConst(0); // [iter]

        // [max, iter]
        stack_start_cycle();

        // begin cycle
        Code.put(Code.dup);  // [max, iter, iter]
        stack_array_length(array.getTerm().obj); // [max, iter, iter, len]


        stack_if(Code.eq); // jump if ite == len

            // body
            // [max, iter]
            Code.put(Code.dup_x1);// [iter, nax, iter]
            Code.load(array.getTerm().obj);// [iter, max, iter, arr]
            stack_swap(); // [iter, max, arr, iter]
            Code.put(Code.aload); // [iter, max, arr[iter] ]
            Code.put(Code.dup2); // [iter, max, arr[iter], max, arr[iter]]

            stack_if(Code.ge); // skip code if max is ge than arr[iter]
                stack_debug();
                // [iter, max, arr[iter]]
                stack_swap(); // [iter, arr[iter], max]

            // continue
            // [iter, max, arr[iter]] or [iter, arr[iter], max] last one is smaller
            stack_if_fixup(); // close

            Code.put(Code.pop); // [iter, max]
            stack_swap(); // [max, iter]

            stack_increment(); // [max, iter+1]

        stack_cycle_fixup();
        stack_if_fixup(); // close if
        // [max, iter]
        Code.put(Code.pop);
    }

    // Exam test2: Find the number of occurrences in an array. Given the max
    @Override
    public void visit(ExprGetCounter getCounter) {
        Obj maxElement = getCounter.getTerm().obj;
        Obj array = getCounter.getExpr().obj;
        stack_clean_by(2); // []

        // creates an array
        stack_create_array_given_size(maxElement, false);
        // arr2
        Code.loadConst(0); // arr2, iter
        // iterate arr1, update iter
        stack_start_cycle();
        Code.put(Code.dup); // arr2, iter, iter
        stack_array_length(array); // arr2, iter, iter, len1
        stack_if(Code.eq);
            // arr2, iter
            Code.put(Code.dup2); // arr2, iter, arr2, iter
            Code.load(array); // arr2, iter, arr2, iter, arr1
            stack_swap(); // arr2, iter, arr2, arr1, iter
            Code.put(Code.aload); // arr2, iter, arr2, arr1[iter]
            Code.put(Code.dup2); // arr2, iter, arr2, iter2, arr2, iter2
            Code.put(Code.aload); // arr2, iter, arr2, iter2, arr2[iter2]
            stack_increment();
            Code.put(Code.astore);// arr2, iter,

            stack_increment(); // arr2, iter+1
        stack_cycle_fixup();
        stack_if_fixup(); // jump out
        stack_clean_by(1); // arr2
    }
}
