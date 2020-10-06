package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.ArrayList;
import java.util.Stack;

public class CodeGenerator extends VisitorAdaptor {
    private int mainPc;

    public CodeGenerator() {
        initializeOrd();
        initializeChar();
        initializeLen();
    }

    private void initializeOrd() {
        Obj ordMethod = Tab.find("ord");
        ordMethod.setAdr(Code.pc);
        Code.put(Code.enter);
        Code.put(1);
        Code.put(1);
        Code.put(Code.load_n);
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    private void initializeChar() {
        Obj ordMethod = Tab.find("chr");
        ordMethod.setAdr(Code.pc);
        Code.put(Code.enter);
        Code.put(1);
        Code.put(1);
        Code.put(Code.load_n);
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    private void initializeLen() {
        Obj ordMethod = Tab.find("len");
        ordMethod.setAdr(Code.pc);
        Code.put(Code.enter);
        Code.put(1);
        Code.put(1);
        Code.put(Code.load_n);
        Code.put(Code.arraylength);
        Code.put(Code.exit);
        Code.put(Code.return_);
    }

    int getMainPc() {
        return mainPc;
    }
    private Obj currentMethod;
    private Obj currentReturn;



    private boolean isNegative = false;

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

        Code.put(Code.enter);
        Code.put(formalCount.getCount());
        Code.put(formalCount.getCount() + localVarCount.getCount() + 1);
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration) {
        if (!needsReturn() && !hasReturn()) {
            Code.put(Code.exit);
            Code.put(Code.return_);
        }
        checkRuntimeError();
    }

    private boolean voidAndDoesNotReturn() {
        return currentReturn == null && currentMethod.getType() == Tab.noType;
    }

    private boolean hasReturn() {
        return currentReturn != null;
    }

    private boolean needsReturn() {
        return currentMethod.getType() != Tab.noType;
    }



    private void checkRuntimeError() {
        if (!needsReturn() && hasReturn() || needsReturn() && !hasReturn()) {
            Code.loadConst(1);
            Code.put(Code.trap);
        }
        currentReturn = null;
        currentMethod = null;

    }

    @Override
    public void visit(StatementReturn statementReturn) {
        Code.put(Code.exit);
        Code.put(Code.return_);
        currentReturn = null;
    }

    @Override
    public void visit(StatementReturnExpr returnExpr) {
        Code.put(Code.exit);
        Code.put(Code.return_);
        currentReturn = returnExpr.getExpr().obj;
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
        // todo : ovo ubije expr, ne valja!
//        Code.put(Code.pop);
//        Code.load(designatorArray.getDesignator().obj);
//        Code.load(designatorArray.getExpr().obj);
//        Code.put(Code.aload);
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
//        if (designatorSingle.obj.getKind() == Obj.Elem) {
//            Code.load(designatorSingle.obj);
//        }
        memoryDesignator = designatorSingle;
        memoryExpr = null;
    }


    @Override
    public void visit(DesignatorStatementAssignOpExpr statementAssign) {

        if (statementAssign.getAssingOp().getClass() == AssignEqual.class) {
            // a =
//            Code.store(statementAssign.getPrepareDesignatorForCombined().obj);
            Code.store(statementAssign.getDesignator().obj);
        } else {
            // a += v a*=
            // imace ili
            // [a], e
            // ili
            // niz,2, niz[2], e
//            putRightOp(statementAssign.getAssingOp());
//                Code.store(statementAssign.getPrepareDesignatorForCombined().obj);

        }

    }

//    private Stack<Designator> desginatorAssign = new Stack<>();
//
//    @Override
//    public void visit(LoadDesignator loadDesignator) {
//        Designator designator = desginatorAssign.pop();
//        if (designator instanceof DesignatorSingle) {
//            Code.load(designator.obj);
//        } else {
//            Code.put(Code.dup2);
//            Code.put(Code.aload);
//        }
//    }
//
//    @Override
//    public void visit(UnloadDesignator unloadDesignator) {
//        desginatorAssign.pop();
//    }


    private void putRightOp(AssingOp assingOp) {
        if (assingOp.getClass() == AssignAddopRight.class ){
            if (((AssignAddopRight) assingOp).getAddopRight().getClass() == AddopRightPlusEqual.class) {
                Code.put(Code.add);
            } else {
                Code.put(Code.sub);
            }
        } else {
            if (((AssignMulopRight) assingOp).getMulopRight().getClass() == MulopRightMulEqual.class) {
                Code.put(Code.mul);
            } else if (((AssignMulopRight) assingOp).getMulopRight().getClass() == MulopRightDivEqual.class) {
                Code.put(Code.div);
            } else {
                Code.put(Code.rem);
            }
        }
    }

    @Override
    public void visit(DesignatorStatementIncrement increment) {
        duplicate2IfArray(increment.getDesignator());
        Code.load(increment.getDesignator().obj);
        Code.loadConst(1);
        Code.put(Code.add);
        Code.store(increment.getDesignator().obj);
    }

    @Override
    public void visit(DesignatorStatementDecrement decrement) {
        duplicate2IfArray(decrement.getDesignator());
        Code.load(decrement.getDesignator().obj);
        Code.loadConst(1);
        Code.put(Code.sub);
        Code.store(decrement.getDesignator().obj);
    }

    private void duplicate2IfArray(Designator designator) {
        if (designator.getClass() == DesignatorArray.class) {
            Code.put(Code.dup2);
        }
    }

    private void incDecOp(Designator designator, int op) {
        if (designator.getClass() == DesignatorArray.class) {
            Code.put(Code.dup2);
            Code.put(Code.aload);
            Code.put(Code.dup_x2); // leaves value on stack
            Code.loadConst(1);
            Code.put(op); // add or sub
            Code.store(designator.obj);
        } else {
            Code.put(Code.dup);
            Code.loadConst(1);
            Code.put(op); // add or sub
            Code.store(designator.obj);
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
    Stack<Expr> exprStack = new Stack<>(); // array index
    Stack<Integer> combinedOpStack = new Stack<>(); // op
    @Override
    public void visit(LoadDesignator1 loadDesignator1) {
        if (memoryDesignator != null) {
            designatorStack.push(memoryDesignator);
            if (memoryDesignator.obj.getType().getKind() != Struct.Array) {
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
        int op = Code.add;
        combinedOpStack.push(op);
    }

    @Override
    public void visit(AddopRightMinusEqual addopRight) {
        int op = Code.sub;
        combinedOpStack.push(op);
    }

    @Override
    public void visit(MulopRightMulEqual mulopRight) {
        int op = Code.mul;
        combinedOpStack.push(op);
    }

    @Override
    public void visit(MulopRightDivEqual mulopRight) {
        int op = Code.div;
        combinedOpStack.push(op);
    }

    @Override
    public void visit(MulopRightModEqual mulopRight) {
        int op = Code.rem;
        combinedOpStack.push(op);
    }

//    @Override
//    public void visit(Mulop_MulopRight mulopRight) {
//        int op = getMulOpRightCode(mulopRight);
//        combinedOpStack.push(op);
//    }

    // expr -> array index
    // designator -> regular or array
    // if expr == null -> designator is regular
    @Override
    public void visit(EndExprStack endExprStack) {
        // resenje na steku
        while (!combinedOpStack.empty()) {
            Designator designator = designatorStack.pop();
            Expr expr = exprStack.pop();
            Integer op = combinedOpStack.pop();
            if (expr != null) {
                Code.load(designator.obj);
                stack_swap();
                Code.load(expr.obj);
                stack_swap();
                Code.load(designator.obj);
                Code.load(expr.obj);
                Code.put(Code.aload);
                fixWrongWayOp(op); // important a - b != b - a
                Code.put(op);
                Code.put(Code.dup_x2);
                Code.put(Code.astore);
            } else {
                Code.load(designator.obj);
                fixWrongWayOp(op);
                Code.put(op);
                Code.put(Code.dup);
                Code.store(designator.obj);
            }
        }
    }

    private void fixWrongWayOp(int op) {
        if (op == Code.add || op == Code.mul) {
            return;
        }
        stack_swap();
    }



    public void visit(TermMulOpFactor termMulOpFactor) {
        if (termMulOpFactor.getMulop() instanceof Mulop_MulopRight) {
            Code.put(Code.dup_x1);
            Code.put(Code.pop);
            Code.put(Code.pop);
//            combinedOpStack.push(Code.mul);
        } else {

                putMullopLeft((Mulop_MulopLeft) termMulOpFactor.getMulop());
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

    private void putAddopLeft(Addop_AddopLeft addop_addopLeft) {
        if (addop_addopLeft.getAddopLeft().getClass() == AddopLeftPlus.class) {
            Code.put(Code.add);
        } else {
            Code.put(Code.sub);
        }
    }

    private void putMullopLeft(Mulop_MulopLeft mulopLeft) {
        if (mulopLeft.getMulopLeft().getClass() == MulopLeftMul.class) {
            Code.put(Code.mul);
        } else if (mulopLeft.getMulopLeft().getClass() == MulopLeftDiv.class) {
            Code.put(Code.div);
        } else {
            Code.put(Code.rem);
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
        Code.put(Code.newarray);
        if (newTypeArray.obj.getType().getElemType() == Tab.charType) {
            Code.put(0);
        } else {
            Code.put(1);
        }
    }


    // IF

    private Stack<ArrayList<Integer>> fixupAdrStack = new Stack<>();
    private Stack<ArrayList<Integer>> fixupPostivieAdrStack = new Stack<>();
    private Stack<Integer> postElseFixupAdrStack = new Stack<>();
    private Stack<Integer> relOpStack = new Stack<>();

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
        fixupPostivieAdrStack.pop();
    }

    @Override
    public void visit(PrepareCondition prepareIf) {
        fixupAdrStack.push(new ArrayList<>());
        fixupPostivieAdrStack.push(new ArrayList<>());
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
        // means we have read Term && (Factor, but not yet)
        int op = relOpStack.pop(); // ovo cita iz Terma, loadovane su svi vec
        Code.putFalseJump(op, 0);
        int adr = Code.pc - 2;
        fixupAdrStack.peek().add(adr);
    }

    @Override
    public void visit(OrKeyword orKeyword) {
        // needs to jump to ifs statement if pozitive!  (double negative)
        //needs new stack for jump
        int op = relOpStack.pop();
        int positiveOp = Code.inverse[op];
        Code.putFalseJump(positiveOp, 0);
        int adr = Code.pc - 2;
        fixupPostivieAdrStack.peek().add(adr);

        for (Integer adrJump : fixupAdrStack.peek()) {
            Code.fixup(adrJump);
        }
        fixupAdrStack.peek().clear();
    }

    @Override
    public void visit(StatementAdr statementAdr) {
        for (Integer putBackAdr : fixupPostivieAdrStack.peek()) {
            Code.fixup(putBackAdr);
        }
        fixupPostivieAdrStack.peek().clear();
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

    private Stack<Integer> foreachAdrRepeatTop = new Stack<>();
    private Stack<ArrayList<Integer>> breakFixup = new Stack<>();
    private Stack<ArrayList<Integer>> continueFixup = new Stack<>();
    private Stack<Obj> foreachIteratorObj = new Stack<>();
//    private Stack<Obj> foreachIteratorIndex = new Stack<>();


    @Override
    public void visit(ForeachStart foreachStart) {
//        forAdrFixup.push(new ArrayList<>());
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
//        Obj iteratorIndex = hiddenIterator.obj;
//        foreachIteratorIndex.push(iteratorIndex);
        Code.loadConst(0);
//        Code.store(iteratorIndex);
////        Code.load(iteratorIndex);
////        Code.put(Code.pop);
    }

    @Override
    public void visit(ForeachArrayDesignator arrayDesignator) {
//        top = Code.pc ;
        int top = Code.pc;
        foreachAdrRepeatTop.push(top);
//        Obj iteratorIndex = foreachIteratorIndex.peek();
//        Code.load(iteratorIndex); // already on top
        Code.put(Code.dup); // iterarot_index dup
        Obj array = arrayDesignator.obj;
        Code.load(array);
        Code.put(Code.arraylength);
        Code.putFalseJump(Code.ne, 0);
        int adr = Code.pc - 2;
//        forAdrFixup.peek().add(adr);
        fixupAdrStack.peek().add(adr);
//        foreachAdr = Code.pc - 2;

        Obj iteratorObj = foreachIteratorObj.peek();
        Code.put(Code.dup); // ind, ind
        Code.load(array); // ind, ind, niz
        Code.put(Code.dup_x1); // ind, niz, ind, niz
        Code.put(Code.pop); // ind, niz, ind
//        Code.load(iteratorIndex);
        Code.put(Code.aload); // ind
        Code.store(iteratorObj);
    }

    @Override
    public void visit(ForeachBegin foreachBegin) {
    }

    @Override
    public void visit(ForeachEnd foreachEnd) {
        if (!continueFixup.empty()) {
            for (Integer continueAdr : continueFixup.peek()) {
                Code.fixup(continueAdr);
            }
            continueFixup.pop();
        }
//        Obj iteratorIndex = foreachIteratorIndex.peek();
//        Code.load(iteratorIndex);
        Code.loadConst(1);
        Code.put(Code.add); // leave on stack

//        Code.store(iteratorIndex);
        int top = foreachAdrRepeatTop.pop();
        Code.putJump(top);
    }

    @Override
    public void visit(ForeachExit foreachExit) {
//        for (Integer adrFixup : forAdrFixup.peek()) {
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
//        foreachIteratorIndex.pop();
//        forAdrFixup.pop();
        fixupAdrStack.pop();
        Code.put(Code.pop);
    }

    // FOR

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
        // todo: conditionEmpty
        // jump to statements
        fixupAdrStack.push(new ArrayList<>());
        fixupPostivieAdrStack.push(new ArrayList<>());
        Code.putJump(0);
        int adr = Code.pc - 2;
        fixupPostivieAdrStack.peek().add(adr);

    }

    private Stack<Integer> forDesignatorJumpBackAdr = new Stack<>();
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

    private Stack<Integer> forJumpToEnd = new Stack<>();
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
        // if break jumps not null, do the same
        if (!breakFixup.empty()) {
            for(Integer breakAdr : breakFixup.peek()) {
                Code.fixup(breakAdr);
            }
            breakFixup.pop();
        }
        fixupAdrStack.pop();
        fixupPostivieAdrStack.pop();
        foreachAdrRepeatTop.pop();
    }

    @Override
    public void visit(JumpToStatements jumpToStatements) {
        // bezuslovni skok na Statement Adr
        Code.putJump(0);
        int adr = Code.pc - 2;
        fixupPostivieAdrStack.peek().add(adr);
    }

    // CONTINUE AND BREAK

    @Override
    public void visit(StatementContinue statementContinue) {
        // treba da skoci na univerzalni naziv adr za inkrement sranja
        // todo: sta se desava ako for nema designator 2?
        Code.putJump(0);
        int continueAdr = Code.pc-2;
        continueFixup.peek().add(continueAdr);
    }

    @Override
    public void visit(StatementBreak statementBreak) {
        // jump out
        Code.putJump(0);
        int breakAdr = Code.pc - 2;
//        forAdrFixup.peek().add(breakAdr);
        breakFixup.peek().add(breakAdr);
    }

    // PRINT READ

    @Override
    public void visit(StatementPrintExpr printExpr) {
        if (printExpr.getExpr().obj.getType() == Tab.charType) { // da moze i bool!
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
        if (printExpr.getExpr().obj.getType() == Tab.charType) { // da moze i bool!
            Code.put(Code.bprint);
        } else {
            Code.put(Code.print);
        }
    }

    @Override
    public void visit(StatementRead statementRead) {
        Obj obj = statementRead.getDesignator().obj;
        if (obj.getType().getKind() == Struct.Char) {
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


    private Stack<Integer> stack_if_adr = new Stack<>();
    private Stack<Integer> stack_cycle_adr = new Stack<>();
    private void stack_if(int op) {
        // expects 2 operands on stack
        int oposite_op = Code.inverse[op];
        Code.putFalseJump(oposite_op, 0);
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
    private void stack_create_array_given_size(Obj size, boolean isInt) {
        Code.load(size);
        Code.put(Code.newarray);
        if (isInt) {
            Code.put(1);
        } else {
            Code.put(0);
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

    @Override
    public void visit(ExprGetCounter getCounter) {
        Obj maxElement = getCounter.getTerm().obj;
        Obj array = getCounter.getExpr().obj;
        stack_clean_by(2); // []

        // creates an array
        stack_create_array_given_size(maxElement, true);
        // arr2
        Code.loadConst(0); // arr2, iter
        // zelim da prodjem kroz niz1, da updateujem counter
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

