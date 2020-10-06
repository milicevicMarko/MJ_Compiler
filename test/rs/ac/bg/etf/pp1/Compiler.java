package rs.ac.bg.etf.pp1;

import java.io.*;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;

public class Compiler {

	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}

	public static void main(String[] args) throws Exception {

		Logger log = Logger.getLogger(Compiler.class);

		Reader br = null;
		try {
			File sourceCode = new File("test/program3.mj");
			log.info("Compiling source file: " + sourceCode.getAbsolutePath());

			br = new BufferedReader(new FileReader(sourceCode));
			Yylex lexer = new Yylex(br);

			log.info("======================================	Lexer	======================================");
			MJParser p = new MJParser(lexer);
			Symbol s = p.parse();  //pocetak parsiranja

			log.info("======================================	Parser	======================================");
			Program prog = (Program)(s.value);
			Tab.init();
			// ispis sintaksnog stabla
			log.info(prog.toString(""));
			log.info("======================================	Semantic		======================================");

			// ispis prepoznatih programskih konstrukcija
			SemanticAnalyzer sp = new SemanticAnalyzer();
			prog.traverseBottomUp(sp);

			Tab.dump();
			log.info("======================================	Sem an		======================================");
//			sp.printCouners();

			log.info("======================================	End		======================================");
			if (p.errorDetected) {
				System.err.println("Parser detected an error!");
			}
			if (sp.errorDetected > 0 || p.errorDetected) {
				int numOfErrors = sp.errorDetected + (p.errorDetected? 1 : 0);
				System.err.println("Found " + numOfErrors + " errors!");
			} else {
			    File objFile = new File("test/program.obj");

			    if (objFile.exists()) {
			        objFile.delete();
                }

			    CodeGenerator codeGenerator = new CodeGenerator();
			    prog.traverseBottomUp(codeGenerator);
                Code.dataSize = sp.getNumberOfVars();
                Code.mainPc = codeGenerator.getMainPc();
                Code.write(new FileOutputStream(objFile));
                log.info("Parsiranje zavrseno!");
            }
		}
		finally {
			if (br != null) try { br.close(); } catch (IOException e1) { log.error(e1.getMessage(), e1); }
		}

	}


}
