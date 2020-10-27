package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class SemanticAnalyzer extends VisitorAdaptor {

    private Obj currentMethod = null;
    private Obj currentMethodType = null;
    int errorDetected = 0;
    private int nVars;
    private Obj currentType = null;
    private boolean isNegative = false;
    private boolean isConst = false;
    private int loopLevel = 0;
    private boolean hasMain = false;
    private boolean isEqNeq = false;

    private final HashMap<Obj, ArrayList<Obj>> functionParameters = new HashMap<>();
    private ArrayList<Obj> paramList = null;

    private final Stack<ArrayList<Struct>> functionArguments = new Stack<>();

    private final Stack<Boolean> stackOfCombinedOperations = new Stack<>();

    private final Stack<Obj> usedDesignator = new Stack<>();


    private final Logger log = Logger.getLogger(getClass());

    public SemanticAnalyzer() {
        initializeLen();
        initializeOrd();
        initializeChr();
    }

    private void initializeLen() {
        ArrayList<Obj> expected = new ArrayList<>();
        expected.add(new Obj(Obj.Var, "len", new Struct(Struct.Array)));
        functionParameters.put(Tab.find("len"), expected);
    }

    private void initializeOrd() {
        ArrayList<Obj> expected = new ArrayList<>();
        expected.add(new Obj(Obj.Var, "ord", Tab.charType));
        functionParameters.put(Tab.find("ord"), expected);
    }

    private void initializeChr() {
        ArrayList<Obj> expected = new ArrayList<>();
        expected.add(new Obj(Obj.Var, "chr", Tab.intType));
        functionParameters.put(Tab.find("chr"), expected);
    }

    public void report_error(String message, SyntaxNode info) {
        errorDetected++;
        StringBuilder msg = report(message, info);
        log.error(msg.toString());
    }

    public void report_info(String message, SyntaxNode info) {
        StringBuilder msg = report(message, info);
        log.info(msg.toString());
    }

    private StringBuilder report(String message, SyntaxNode info) {
        StringBuilder msg = new StringBuilder(message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0)
            msg.append(" on line ").append(line);
        return msg;
    }

    public void visit(ProgramName programName) {
        programName.obj = TabAdapter.insert(Obj.Prog, programName.getProgramName(), Tab.noType, programName.getLine());
        Tab.openScope();
    }

    public void visit(Program program) {
        nVars = Tab.currentScope.getnVars();
        Tab.chainLocalSymbols(program.getProgramName().obj);
        Tab.closeScope();
        if (!hasMain) {
            report_error("Program does not contain a main function", program);
        }
        errorDetected += TabAdapter.hasErrors;
    }

    @Override
    public void visit(VarSingle varSingle) {
        varSingle.obj = TabAdapter.insert(Obj.Var, varSingle.getS1(), currentType.getType(), varSingle.getLine());
    }

    @Override
    public void visit(VarArray varArray) {
        varArray.obj = TabAdapter.insert(Obj.Var, varArray.getS1(), new Struct(Struct.Array, currentType.getType()), varArray.getLine());
    }


    @Override
    public void visit(StartOfConstAssign startConst) {
        isConst = true;
    }

    @Override
    public void visit(EndOfConstAssign endOfConstAssign) {
        isConst = false;
    }

    @Override
    public void visit(ConstVarAssing constAssign) {
        Struct type = currentType.getType();
        Struct typeOfConst = constAssign.getConstGroup().obj.getType();
        if (!TabAdapter.isPrimitiveType(type) || !TabAdapter.isPrimitiveType(typeOfConst)) {
            report_error("Const must be of type Int, Char or Bool", constAssign);
            constAssign.obj = Tab.noObj;
        }
        if (!type.equals(typeOfConst)) {
            report_error("Const must be of type Int, Char or Bool", constAssign);
            constAssign.obj = Tab.noObj;
        }
        if (constAssign.obj == null) {
            constAssign.obj = TabAdapter.insert(Obj.Con, constAssign.getName(), type, constAssign.getLine());
            constAssign.obj.setAdr(constAssign.getConstGroup().obj.getAdr());
        }
    }

    @Override
    public void visit(TypeString type) {
        String typeName = type.getTypeName();

        Obj typeObj = TabAdapter.find(typeName, type.getLine());

        if (typeObj == Tab.noObj) {
            report_error("Unknown type", type);
        } else if (typeObj.getKind() != Obj.Type) {
            report_error("Type is not an object", type);
        } else {
            type.obj = typeObj;
            currentType = type.obj;
        }
    }

    @Override
    public void visit(TypeVoid typeVoid) { // todo: could prove troublesome
        typeVoid.obj = Tab.noObj;
        currentType = Tab.noObj;
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration) {
        Tab.chainLocalSymbols(currentMethod);
        currentMethod.setLevel(functionParameters.get(currentMethod).size());
        Tab.closeScope();

        currentMethod = null;
    }

    @Override
    public void visit(MethodSignature methodSignature) {
        String name = methodSignature.getMethodName();
        int line = methodSignature.getLine();
        if (Tab.currentScope.findSymbol(name) != null) {
            report_error("Method '" + name + "' already defined", methodSignature);
            currentMethod = TabAdapter.insert(Obj.Meth, "dummyMethod", currentMethodType.getType(), line);
        } else {
            currentMethod = TabAdapter.insert(Obj.Meth, name, currentMethodType.getType(), line);
        }

        Tab.openScope();

        if ("main".equals(name)) {
            if (hasMain) {
                report_error("Main is already defined", methodSignature);
            } else {
                hasMain = true;
            }
            if (!TabAdapter.isNoneType(currentMethod.getType())) {
                report_error("Main must be void", methodSignature);
            }
            if (paramList.size() > 0) {
                report_error("Main should not have any parameters", methodSignature);
            }
        }

        currentMethod.setLevel(paramList.size());
        for (int i = 0; i < paramList.size(); i++) {
            Obj param = paramList.get(i);
            param = TabAdapter.insert(param, line);
            param.setFpPos(i);
        }

        methodSignature.obj = currentMethod;
        functionParameters.put(currentMethod, paramList);
        paramList = null;
    }


    @Override
    public void visit(MethodStartFunctionParameters mstart) {
        paramList = new ArrayList<>();
        currentMethodType = currentType;
    }


    @Override
    public void visit(ParameterSingle parameterSingle) {
        String name = parameterSingle.getPsingle();
        Struct struct = parameterSingle.getType().obj.getType();
        insertParameter(name, struct, parameterSingle);
    }

    @Override
    public void visit(ParameterArr parameterArr) {
        String name = parameterArr.getParr();
        Struct struct = new Struct(Struct.Array, parameterArr.getType().obj.getType());
        insertParameter(name, struct, parameterArr);
    }

    private boolean checkExistsParameter(String name) {
        for (Obj o : paramList) {
            if (o.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void insertParameter(String name, Struct type, Parameter parameter) {
        Obj toPut;
        if (checkExistsParameter(name)) {
            report_error("Parameter already declared", parameter);
            toPut = new Obj(Obj.Var, "dummyParameter", type);
        } else {
            toPut = new Obj(Obj.Var, name, type);
        }
        paramList.add(toPut);
    }


    @Override
    public void visit(LocalVarDeclSinge localVarDeclSinge) {
        localVarDeclSinge.obj = TabAdapter.insert(Obj.Var, localVarDeclSinge.getVar(), currentType.getType(), localVarDeclSinge.getLine());
    }

    @Override
    public void visit(LocalVarDeclArray localVarDeclArray) {
        localVarDeclArray.obj = TabAdapter.insert(Obj.Var, localVarDeclArray.getVar(), new Struct(Struct.Array, currentType.getType()), localVarDeclArray.getLine());
    }

    @Override
    public void visit(FactorConst factorConst) {
        factorConst.obj = factorConst.getConstGroup().obj;
    }


    @Override
    public void visit(ConstNumber constNumber) {
        constNumber.obj = new Obj(Obj.Con, "constant", Tab.intType, constNumber.getN1(), Obj.NO_VALUE);
    }

    @Override
    public void visit(ConstChar constChar) {
        constChar.obj = new Obj(Obj.Con, "constant", Tab.charType, constChar.getC1(), Obj.NO_VALUE);
    }

    @Override
    public void visit(ConstBool constBool) {
        String bool = constBool.getB1();
        int boolConstActual = -1;
        if ("true".equals(bool)) {
            boolConstActual = 1;
        } else if ("false".equals(bool)) {
            boolConstActual = 0;
        }

        constBool.obj = new Obj(Obj.Con, "constant", TabAdapter.booleanType, boolConstActual, Obj.NO_VALUE);
    }

    @Override
    public void visit(FactorFunCallPars funCall) {
        if (TabAdapter.isNoType(funCall.getFunctionCall().obj.getType())) {
            report_error("Cannot use a Void function as a factor", funCall);
        }
        funCall.obj = funCall.getFunctionCall().obj;
    }

    @Override
    public void visit(FactorDesignator factorDesignator) {
        factorDesignator.obj = factorDesignator.getDesignator().obj;
    }

    @Override
    public void visit(FactorParenExpr expr) {
        expr.obj = expr.getExpr().obj;
    }

    @Override
    public void visit(DesignatorArrayHelper designatorArrayHelper) {
        designatorArrayHelper.obj = designatorArrayHelper.getDesignator().obj;
    }

    @Override
    public void visit(DesignatorArray designatorArray) {
        Obj designator = designatorArray.getDesignatorArrayHelper().obj;
        Struct expr = designatorArray.getExpr().obj.getType();
        if (!TabAdapter.isArrayType(designator.getType())) {
            report_error("Designator must be an Array type", designatorArray);
            designatorArray.obj = Tab.noObj;
        } else {
            if (!TabAdapter.isIntType(expr)) {
                report_error("Expression must be of type Int", designatorArray);
            }
            designatorArray.obj = new Obj(Obj.Elem, designator.getName() + "_elem", designator.getType().getElemType());
        }
    }

    @Override
    public void visit(FactorNewTypeArray newType) {
        Struct exprStruct = newType.getExpr().obj.getType();
        if (!TabAdapter.isIntType(exprStruct)) {
            report_error("Array size must be of type Int", newType);
        }
        newType.obj = new Obj(Obj.Type, "arr", new Struct(Struct.Array, currentType.getType()));
    }

    @Override
    public void visit(NegativeTrue negativeTrue) {
        isNegative = true;
    }



    @Override
    public void visit(NegativeFalse negativeFalse) {
        isNegative = false;
    }

    @Override
    public void visit(DesignatorStatementIncrement designatorIncrement) {
        canPostcrement(designatorIncrement.getDesignator().obj, designatorIncrement);
    }

    @Override
    public void visit(DesignatorStatementDecrement designatorDecrement) {
        canPostcrement(designatorDecrement.getDesignator().obj, designatorDecrement);
    }

    private void canPostcrement(Obj obj, DesignatorStatement designator) {
        if (isNotVarFldElem(obj)) {
            report_error("Not a Var, a Fld or an Elem", designator);
        } else if (!obj.getType().compatibleWith(Tab.intType)) {
            report_error("Designator must be Int type", designator);
        }
    }

    @Override
    public void visit(DesignatorSingle designatorSingle) {
        Obj designObj = TabAdapter.find(designatorSingle.getName(), designatorSingle.getLine());
        if (designObj.equals(Tab.noObj)) {
            report_error("Designator not declared", designatorSingle);
            designatorSingle.obj = Tab.noObj;
        } else {
            designatorSingle.obj = designObj;
        }
    }


    @Override
    public void visit(ExprTerm exprTerm) {
        if (isNegative && !TabAdapter.isIntType(exprTerm.getTerm().obj.getType())) {
            report_error("Only Int can be negative", exprTerm);
        }
        exprTerm.obj = exprTerm.getTerm().obj;
        isNegative = false;
    }

    @Override
    public void visit(ExprTermAddOpTerm exprTerm) {
        if (!TabAdapter.isIntType(exprTerm.getExpr().obj.getType())) {
            report_error("First operand must be of type Int", exprTerm);
        }
        if (!TabAdapter.isIntType(exprTerm.getTerm().obj.getType())) {
            report_error("Second operand must be of type Int", exprTerm);
        }
        if (!exprTerm.getExpr().obj.getType().compatibleWith(exprTerm.getTerm().obj.getType())) {
            report_error("Incompatible types", exprTerm);
        }
        boolean isOpCombined = stackOfCombinedOperations.pop();
        if (isOpCombined && isNotVarFldElem(exprTerm.getExpr().obj)) {
            report_error("With combined arithmetic operators (+= / -=) only Vars, Elems and Flds can be used", exprTerm);
        }
        exprTerm.obj = exprTerm.getExpr().obj;
    }

    @Override
    public void visit(TermFactor termFactor) {
        termFactor.obj = termFactor.getFactor().obj;
    }

    @Override
    public void visit(TermMulOpFactor termMulOpFactor) {
        if (!TabAdapter.isIntType(termMulOpFactor.getTerm().obj.getType())) {
            report_error("First operand must be of type Int", termMulOpFactor);
        }
        if (!TabAdapter.isIntType(termMulOpFactor.getFactor().obj.getType())) {
            report_error("Second operand must be of type Int", termMulOpFactor);
        }
        boolean op = stackOfCombinedOperations.pop();
        if (op && isNotVarFldElem(termMulOpFactor.getTerm().obj)) {
            report_error("With combined arithmetic operators (*= / /= / %=) only Vars, Elems and Flds can be used", termMulOpFactor);
        }
        termMulOpFactor.obj = termMulOpFactor.getTerm().obj;

    }

    @Override
    public void visit(AssignEqual assingOp) {
        assingOp.obj = assingOp.getExpr().obj;
    }

    @Override
    public void visit(AssignAddopRight assingOp) {
        stackOfCombinedOperations.push(true);
        assingOp.obj = assingOp.getExpr().obj;
    }

    @Override
    public void visit(AssignMulopRight assingOp) {
        stackOfCombinedOperations.push(true);
        assingOp.obj = assingOp.getExpr().obj;
    }

    @Override
    public void visit(DesignatorStatementAssignOpExpr designatorStatement) {
        if (isNotVarFldElem(designatorStatement.getDesignator().obj)) {
            report_error("You can only assign to a Var, a Fld or an Elem", designatorStatement);
        }
        if (!designatorStatement.getDesignator().obj.getType().compatibleWith(designatorStatement.getAssingOp().obj.getType())) {
            report_error("Incompatible types at assignment", designatorStatement);
        }
        if (!designatorStatement.getAssingOp().obj.getType().assignableTo(designatorStatement.getDesignator().obj.getType())) {
            report_error("Assign error", designatorStatement);
        }
    }

    private boolean isNotVarFldElem(Obj obj) {
        int kind = obj.getKind();
        return kind != Obj.Var && kind != Obj.Fld && kind != Obj.Elem;
    }

    @Override
    public void visit(StatementReturn statementReturn) {
        if (!currentMethod.getType().compatibleWith(Tab.noType)) {
            report_error("Function should return a not-null", statementReturn);
        }
    }

    @Override
    public void visit(StatementReturnExpr returnExpr) {
        if (TabAdapter.isNoType(currentMethod.getType())) {
            report_error("Function should return void", returnExpr);
        } else if (!currentMethod.getType().equals(returnExpr.getExpr().obj.getType())) {
            report_error("Return type must be equal to expected type", returnExpr);

        }
    }

    @Override
    public void visit(StatementRead statementRead) {
        Obj designator = statementRead.getDesignator().obj;
        if (isNotVarFldElem(designator)) {
            report_error("Read takes a Var, a Fld or an Elem", statementRead);
        }
        if (!TabAdapter.isPrimitiveType(designator.getType())) {
            report_error("Designator must a primitive type: Int, Char or Bool", statementRead);
        }
    }

    @Override
    public void visit(FunCall funCall) {
        if (funCall.getDesignator().obj == null) {
            report_error("Function not defined", funCall);
            funCall.obj = Tab.noObj;
            return;
        }
        Obj methodObj = funCall.getDesignator().obj;
        if (methodObj.getKind() != Obj.Meth) {
            report_error("Not calling a method", funCall);
        } else {
            ArrayList<Obj> paramsExpected = functionParameters.get(methodObj);
            ArrayList<Struct> arguments = functionArguments.pop();

            if (paramsExpected.size() != arguments.size()) {
                report_error("Incorrect number of arguments", funCall);
            } else {
                for (int i = 0; i < paramsExpected.size(); i++) {
                    Struct expectedStruct = paramsExpected.get(i).getType();
                    Struct givenStruct = arguments.get(i);
                    if ((isLen(funCall.getDesignator()) && TabAdapter.isArrayType(givenStruct))) {
                        continue;
                    }
                    if ( !givenStruct.compatibleWith(expectedStruct)) {
                        report_error("Type mismatch at argument ("+(i+1)+")", funCall);
                        return;
                    }
                }
            }
            funCall.obj = methodObj;
        }
    }

    private boolean isLen(Designator des) {
        return "len".equals(des.obj.getName());
    }

    @Override
    public void visit(ActParsStart actParsStart) {
        functionArguments.push(new ArrayList<>());
    }

    @Override
    public void visit(ActPar actPar) {
        functionArguments.peek().add(actPar.getExpr().obj.getType());
    }

    @Override
    public void visit(ConditionFactExpr expr) {
        Struct exprStruct = expr.getExpr().obj.getType();
        if (!TabAdapter.isBoolType(exprStruct)) {
            report_error("Expresion must be a boolean", expr);
            expr.struct = TabAdapter.noType;
        } else {
            expr.struct = exprStruct;
        }
    }


    @Override
    public void visit(ConditionFactExprOpExpr exprOpExpr) {
        Struct first = exprOpExpr.getExpr().obj.getType();
        Struct second = exprOpExpr.getExpr1().obj.getType();

        if (TabAdapter.isNoneType(first) || TabAdapter.isNoneType(second)) {
            exprOpExpr.struct = TabAdapter.noType;
            return;
        }
        if (!first.compatibleWith(second)) {
            report_error("Incompatible types", exprOpExpr);
            exprOpExpr.struct = TabAdapter.noType;
        } else {
            if (TabAdapter.isArrayType(first) && TabAdapter.isArrayType(second) && !isEqNeq) {
                report_error("Arrays can only be compared with '==' or '!='", exprOpExpr);
                exprOpExpr.struct = TabAdapter.noType;
                return;
            }
            isEqNeq = false;
            exprOpExpr.struct = TabAdapter.booleanType;
        }

    }

    @Override
    public void visit(RelOpNeq relOpNeq) {
        isEqNeq = true;
    }

    @Override
    public void visit(RelOpEq relOpNeq) {
        isEqNeq = true;
    }


    @Override
    public void visit(ConditionTermSingle conditionTermSingle) {
        conditionTermSingle.struct = conditionTermSingle.getConditionFact().struct;
    }

    @Override
    public void visit(ConditionTermList conditionTermList) {
        int typeFirst = conditionTermList.getConditionFact().struct.getKind();
        int typeSecond = conditionTermList.getConditionTerm().struct.getKind();
        if (typeFirst == Struct.None || typeSecond == Struct.None) {
            conditionTermList.struct = TabAdapter.noType;
            return;
        }
        if (typeFirst != typeSecond) {
            report_error("Both expressions must be of same type to use AND", conditionTermList);
            conditionTermList.struct = TabAdapter.noType;
        } else {
            conditionTermList.struct = TabAdapter.booleanType;
        }
    }

    @Override
    public void visit(ConditionSingle conditionSingle) {
        conditionSingle.struct = conditionSingle.getConditionTerm().struct;
    }


    @Override
    public void visit(ConditionList conditionList) {
        int typeFirst = conditionList.getCondition().struct.getKind();
        int typeSecond = conditionList.getConditionTerm().struct.getKind();

        if (typeFirst == Struct.None || typeSecond == Struct.None) {
            conditionList.struct = TabAdapter.noType;
            return;
        }
        if (typeFirst != typeSecond) {
            report_error("Both expressions must be of same type to use OR", conditionList);
            conditionList.struct = TabAdapter.noType;

        } else {
            conditionList.struct = TabAdapter.booleanType;
        }
    }

    @Override
    public void visit(ForeachArrayDesignator arrayDesignator) {
        arrayDesignator.obj = arrayDesignator.getDesignator().obj;
    }

    @Override
    public void visit(ForeachIterator foreachIterator) {
        String ident = foreachIterator.getIdent();
        foreachIterator.obj = TabAdapter.find(ident, foreachIterator.getLine());
        if (usedDesignator.contains(foreachIterator.obj)) {
            report_error("Cannot use same iterator in the same scope for iterating", foreachIterator);
            usedDesignator.push(Tab.noObj);
        } else {
            usedDesignator.push(foreachIterator.obj);
        }
    }

    @Override
    public void visit(ForeachExit foreachExit) {
        usedDesignator.pop();
    }

    @Override
    public void visit(StatementForEach statementForEach) {
        Obj designator = statementForEach.getForeachArrayDesignator().obj;
        Obj ident = statementForEach.getForeachIterator().obj;
        if (!TabAdapter.isArrayType(designator.getType())) {
            report_error("Designator must be of Array type", statementForEach);
        } else if (!ident.equals(Tab.noObj) && !designator.getType().getElemType().equals(ident.getType())) {
            report_error("Type mismatch: identifier and array", statementForEach);
        }
        loopLevel--;
    }

    @Override
    public void visit(ForeachStart foreachStart) {
        loopLevel++;
    }

    @Override
    public void visit(StatementFor statementFor) {
        Struct condition = statementFor.getOptionalForCondition().struct;
        if (!TabAdapter.isBoolType(condition) && !TabAdapter.isNoType(condition)) {
            report_error("Bad condition ", statementFor);
        }
        loopLevel--;
    }

    @Override
    public void visit(StatementPrintExpr expr) {
        Struct exprStruct = expr.getExpr().obj.getType();
        typeCheckStatementPrintStruct(exprStruct, expr.getExpr());
    }

    @Override
    public void visit(StatementPrintExprNumber expr) {
        Struct exprStruct = expr.getExpr().obj.getType();
        typeCheckStatementPrintStruct(exprStruct, expr.getExpr());
    }

    private void typeCheckStatementPrintStruct( Struct exprStruct, Expr expr) {
        if (!TabAdapter.isPrimitiveType(exprStruct)) {
            report_error("Print takes primitive types: Int, Char or Bool", expr);
        }
    }

    @Override
    public void visit(ForConditionEmpty forCondition) {
        forCondition.struct = TabAdapter.booleanType;
        loopLevel++;
    }

    @Override
    public void visit(ForCondition forCondition) {
        Struct condition = forCondition.getCondition().struct;
        if (TabAdapter.isBoolType(condition)) {
            forCondition.struct = forCondition.getCondition().struct;
            loopLevel++;
        } else {
            report_error("Condition must be of Boolean type", forCondition);
            forCondition.struct = Tab.noType;
        }
    }



    @Override
    public void visit(StatementBreak statementBreak) {
        if (loopLevel <= 0) {
            report_error("Break is not within a loop", statementBreak);
        }
    }

    @Override
    public void visit(StatementContinue statementContinue) {
        if (loopLevel <= 0) {
            report_error("Continue is not within a loop", statementContinue);
        }
    }

    @Override
    public void visit(AddopLeftPlus plus) {
        stackOfCombinedOperations.push(false);
    }
    @Override
    public void visit(AddopLeftMinus plus) {
        stackOfCombinedOperations.push(false);
    }
    @Override
    public void visit(MulopLeftMul plus) {
        stackOfCombinedOperations.push(false);
    }
    @Override
    public void visit(MulopLeftDiv plus) {
        stackOfCombinedOperations.push(false);
    }
    @Override
    public void visit(MulopLeftMod plus) {
        stackOfCombinedOperations.push(false);
    }

    @Override
    public void visit(MulopRightMulEqual mul) {
        stackOfCombinedOperations.push(true);
    }

    @Override
    public void visit(MulopRightModEqual mul) {
        stackOfCombinedOperations.push(true);
    }

    @Override
    public void visit(MulopRightDivEqual mul) {
        stackOfCombinedOperations.push(true);
    }

    @Override
    public void visit(AddopRightPlusEqual mul) {
        stackOfCombinedOperations.push(true);
    }

    @Override
    public void visit(AddopRightMinusEqual mul) {
        stackOfCombinedOperations.push(true);
    }

    public int getNumberOfVars() {
        return nVars;
    }


    @Override
    public void visit(ExprMaxArray exprMaxArray) {
        // check if int array

        if (TabAdapter.isArrayType(exprMaxArray.getTerm().obj.getType()) && TabAdapter.isIntType(exprMaxArray.getTerm().obj.getType().getElemType())) {
            exprMaxArray.obj = new Obj(Obj.Var, "max_arr", Tab.intType);
        } else {
            report_error("Expression must be an array and of type int", exprMaxArray);
            exprMaxArray.obj = Tab.noObj;
        }
    }

    @Override
    public void visit(ExprGetCounter getCounter) {
        getCounter.obj = new Obj(Obj.Var, "counter_array", new Struct(Struct.Array, Tab.intType));
    }
}
