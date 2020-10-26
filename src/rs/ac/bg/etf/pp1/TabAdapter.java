package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class TabAdapter extends Tab {

    static final Struct booleanType = new Struct(Struct.Bool);
    static int hasErrors = 0;

    static {
        Tab.insert(Obj.Type, "bool", TabAdapter.booleanType);
    }

    static String getObjKind(int kind) {
        switch(kind) {
            case Obj.Con : return "Con";
            case Obj.Var : return"Var";
            case Obj.Type : return "Type";
            case Obj.Meth : return "Meth";
            case Obj.Fld: return "Fld";
            case Obj.Elem : return "Elem";
            case Obj.Prog : return "Prog";
            default: return "Error";
        }
    }

    static String getStructType(int type) {
        switch(type) {
            case -1 : return "No_Value";
            case Struct.None : return "None";
            case Struct.Int : return "Int";
            case Struct.Char : return "Char";
            case Struct.Array : return "Array";
            case Struct.Class : return "Class";
            case Struct.Bool : return "Bool";
            case Struct.Enum : return "Enum";
            case Struct.Interface : return "Interface";
            default: return "Error";
        }
    }

    static void printHandler(Obj node, int line) {
        if (node.getKind() == Obj.Var || node.getKind() == Obj.Con || node.getKind() == Obj.Elem || node.getKind() == Obj.Meth) {
            String objKind = getObjKind(node.getKind());
            String structType = getStructType(node.getType().getKind());
            String name = node.getName();
            int adr = node.getAdr();
            int level = node.getLevel();
            System.out.println("Searching for " + line + "(" + name + "), found " + objKind + " "+ name + ": " + structType + ", " + adr + ", " + level );
        }
    }

    static void notFoundHandler(String name, int line) {
        System.err.println("Error on line " + line + "(" + name + ") not found");
        hasErrors++;
    }

    static void alreadyExistsHandler(String name, int line) {
        System.err.println("Error on line " + line + "(" + name + ") already exists");
        hasErrors++;
    }

    public static boolean inCurrentScope(String name) {
        Obj resultObj = currentScope.findSymbol(name);
        return resultObj != null;
    }

    public static Obj find(String name, int line) {
//        if (name.equals("fun1")) {
//            Thread.dumpStack();
//        }
        Obj resultObj = Tab.find(name);
        if (resultObj == Tab.noObj) {
            TabAdapter.notFoundHandler(name, line);
            return noObj;
        } else {
            TabAdapter.printHandler(resultObj, line);
            return resultObj;
        }
    }


    public static Obj insert(int kind, String name, Struct type, int line) {
        if (inCurrentScope(name)) {
            alreadyExistsHandler(name, line);
            // todo: test
//            return Tab.noObj;
        }
        Obj returnObj = Tab.insert(kind, name, type);
        if (returnObj.equals(Tab.noObj)) {
            alreadyExistsHandler(name, line);
        }
        return returnObj;
    }

    public static Obj insert(Obj old, int line) {
        return TabAdapter.insert(old.getKind(), old.getName(), old.getType(), line);
    }

}
