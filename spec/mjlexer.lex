
package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

%%

%{

	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type) {
		return new Symbol(type, yyline+1, yycolumn);
	}
	
	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type, Object value) {
		return new Symbol(type, yyline+1, yycolumn, value);
	}

%}

%cup
%line
%column

%xstate COMMENT

%eofval{
	return new_symbol(sym.EOF);
%eofval}

%%

" " 	{ }
"\b" 	{ }
"\t" 	{ }
"\r\n" 	{ }
"\f" 	{ }

"program"   { return new_symbol(sym.PROG, yytext());	}
"print" 	{ return new_symbol(sym.PRINT, yytext()); 	}
"return" 	{ return new_symbol(sym.RETURN, yytext()); 	}
"void" 		{ return new_symbol(sym.VOID, yytext()); 	}
"read"		{ return new_symbol(sym.READ, yytext()); 	}
"new"		{ return new_symbol(sym.NEW, yytext()); 	}
"if"		{ return new_symbol(sym.IF, yytext()); 	}
"else"		{ return new_symbol(sym.ELSE, yytext()); 	}
"const"		{ return new_symbol(sym.CONST, yytext()); }

"for"		{ return new_symbol(sym.FOR, yytext()); 	}
"break"		{ return new_symbol(sym.BREAK, yytext()); 	}
"continue"	{ return new_symbol(sym.CONTINUE, yytext()); 	}
"foreach"	{ return new_symbol(sym.FOREACH, yytext()); 	}

"abstract"	{ return new_symbol(sym.ABSTRACT, yytext()); 	}
"extends"	{ return new_symbol(sym.EXTENDS, yytext()); 	}
"class" 	{ return new_symbol(sym.CLASS, yytext()); 	}

"public"	{ return new_symbol(sym.PUBLIC, yytext()); 	}
"private"	{ return new_symbol(sym.PRIVATE, yytext()); 	}
"protected"	{ return new_symbol(sym.PROTECTED, yytext()); 	}

"+" 		{ return new_symbol(sym.PLUS, yytext()); 	}
"++" 		{ return new_symbol(sym.INC, yytext()); 	}
"+=" 		{ return new_symbol(sym.PLUSEQUAL, yytext()); 	}

"-" 		{ return new_symbol(sym.MINUS, yytext()); 	}
"--" 		{ return new_symbol(sym.DEC, yytext()); 	}
"-=" 		{ return new_symbol(sym.MINUSEQUAL, yytext()); 	}

"*" 		{ return new_symbol(sym.MUL, yytext()); 	}
"*=" 		{ return new_symbol(sym.MULEQUAL, yytext()); 	}

"/" 		{ return new_symbol(sym.DIV, yytext()); 	}
"/=" 		{ return new_symbol(sym.DIVEQUAL, yytext()); 	}

"%" 		{ return new_symbol(sym.MOD, yytext()); 	}
"%=" 		{ return new_symbol(sym.MODEQUAL, yytext()); 	}

"." 		{ return new_symbol(sym.DOT, yytext()); 	}
":" 		{ return new_symbol(sym.COLON, yytext()); 	}

"||" 		{ return new_symbol(sym.OR, yytext()); 	}
"&&" 		{ return new_symbol(sym.AND, yytext()); 	}

"=" 		{ return new_symbol(sym.EQUAL, yytext()); 	}
";" 		{ return new_symbol(sym.SEMI, yytext()); 	}	
"," 		{ return new_symbol(sym.COMMA, yytext()); 	}
"(" 		{ return new_symbol(sym.LPAREN, yytext()); 	}
")" 		{ return new_symbol(sym.RPAREN, yytext()); 	}
"{" 		{ return new_symbol(sym.LBRACE, yytext()); 	}
"}"			{ return new_symbol(sym.RBRACE, yytext()); 	}
"[" 		{ return new_symbol(sym.LSQUARE, yytext()); 	}
"]"			{ return new_symbol(sym.RSQUARE, yytext()); 	}

">" 		{ return new_symbol(sym.GT, yytext()); 	}
"<" 		{ return new_symbol(sym.LT, yytext()); 	}
">="		{ return new_symbol(sym.GTE, yytext()); 	}
"<=" 		{ return new_symbol(sym.LTE, yytext()); 	}
"=="		{ return new_symbol(sym.EQ, yytext()); 	}
"!="        { return new_symbol(sym.NEQ, yytext()); 	}

"^"         { return new_symbol(sym.MAX, yytext()); 	}
"#"         { return new_symbol(sym.HASH, yytext()); 	}

"//" { yybegin(COMMENT); } 
<COMMENT> . { yybegin(COMMENT); }
<COMMENT> "\r\n" { yybegin(YYINITIAL); }

[0-9]+  { return new_symbol(sym.NUMBER, new Integer (yytext())); }
true | false { return new_symbol(sym.BOOL, yytext()); }

"'"."'" { return new_symbol(sym.CHAR, new Character (yytext().charAt(1))); }


([a-z]|[A-Z])[a-z|A-Z|0-9|_]* 	{ return new_symbol (sym.STRING, yytext()); }

. { System.err.println("Leksicka greska ("+yytext()+") u liniji "+(yyline+1)+" na mestu " + (yycolumn)); }










