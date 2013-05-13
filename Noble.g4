grammar Noble;

r               : clazz*
                EOF
                ;

clazz           : function              #FunctionDef
                ;

function        : FUNCTION FUNCNAME LPAREN pars* RPAREN func_block SEMI
                ;            

pars            : (arraypar|ID) COMMA? ;
arraypar        : ID '|' INT '|';

func_block      : LCURLY NL* (expr SEMI NL*)* func_return NL* RCURLY
                ;

func_return     : (expr RETURN SEMI)?
                ;

expr            : expr op=(OP_MULT|OP_DIV) expr                         #MulDiv
                | expr op=(OP_ADD|OP_SUB) expr                          #AddSub
                | expr COMPARISON expr                                  #Comparison
                | NOT expr                                              #Not
                | expr AND expr                                         #And
                | expr OR expr                                          #Or
                | functioncall                                          #FunctionCall
                | IF LPAREN expr RPAREN func_block                      #If
                | IF LPAREN expr RPAREN func_block ELSE func_block      #IfElse
                | TRY func_block CATCH func_block (FINALLY func_block)? #TryCatch
                | FOREACH LPAREN ID IN ID RPAREN func_block             #Foreach
                | FOR LPAREN 
                    for_assign? SEMI for_test SEMI for_update? 
                  RPAREN func_block                                     #For
                | WHILE LPAREN expr RPAREN func_block                   #While
                | DO func_block WHILE LPAREN expr RPAREN                #DoWhile
                | REPEAT LPAREN (INT|ID) RPAREN func_block              #Repeat
                | BREAK                                                 #Break
                | exep                                                  #Exception
                | LPAREN expr RPAREN                                    #Parens
                | ID                                                    #Variable
                | STRING                                                #String
                | CHAR                                                  #Char
                | INT                                                   #Int
                | FLOAT                                                 #Float
                | NULL                                                  #Null
                | BOOL                                                  #Bool
                | HEXCOLOUR                                             #Colour
                | PRINT expr                                            #Print
                | LOG expr                                              #Log
                | array                                                 #ArrayCollective
                | object_def                                            #ObjectDef
                | object_call                                           #ObjectCall
                | expr ASSIGN expr                                      #AssignVar
                ;


array           : ID '|' INT '|'                                        #ArrayInit
                | ID '|' INT '|' LCURLY expr (COMMA expr)* RCURLY       #ArrayInitAssign
                | arr ASSIGN expr                                       #ArrayAssign
                | arr                                                   #ArrayCall
                ;

arr             : ID '[' (ID|INT) ']';

functioncall    : FUNCNAME LPAREN parlist* RPAREN;

parlist         : (arraypar|expr) COMMA? ;

object_def      : LCURLY NL? object_props NL? RCURLY;
object_props    : object_prop (COMMA NL? object_prop)*;
object_prop     : object_id COLON val=(STRING|CHAR|INT|FLOAT|NULL|BOOL|HEXCOLOUR);
object_id       : STRING | INT;

object_call     : ID LSQUARE (STRING|INT) RSQUARE;


for_assign      : expr;
for_test        : expr;
for_update      : expr;

exep            : INTERROBANG LPAREN expr RPAREN;


/* TOKENS */

fragment 
    LETTER  : [a-zA-Z];
fragment 
    DIGIT   : [0-9];

OP_MULT     : '*' ;
OP_DIV      : '/' ;
OP_SUB      : '-' ;
OP_ADD      : '+' ;


NOT         : ('!' | 'NOT');
COMPARISON  : ('<'|'<='|'>'|'>='|'=='|'!=');
AND         : ('&&'|'AND');
OR          : ('||'|'OR');

IF          : I F;
ELSE        : E L S E;
FOREACH     : F O R E A C H;
FOR         : F O R;
DO          : D O;
WHILE       : W H I L E;
REPEAT      : R E P E A T;
SWITCH      : S W I T C H;
CASE        : C A S E;
IN          : I N;
BREAK       : B R E A K;
TRY         : T R Y;
CATCH       : C A T C H;
FINALLY     : F I N A L L Y;

LCURLY      : '{';
RCURLY      :'}';
LPAREN      : '(';
RPAREN      : ')';
LSQUARE     : '[';
RSQUARE     : ']';
COMMA       : ',';
SEMI        : ';';
COLON       : ':';

ID          : '!'LETTER (LETTER|DIGIT|'_'|'.')* ;
FUNCTION    : F U N C T I O N;

ASSIGN      : '<-';
RETURN      : '->';
PRINT       : '!->';
LOG         : '?->';


STRING      : '"' .*? '"';
CHAR        : '\'' [a-zA-Z] '\'' ;
INT         : '-'? ('0' | [1-9] DIGIT*) [lL]? ;
FLOAT       : DIGIT+ '.' DIGIT+ ;
NULL        : N U L L;
BOOL        : TRUE | FALSE ;
fragment 
    TRUE    : T R U E;
fragment
    FALSE   : F A L S E;
HEXCOLOUR   : '#'[0-9A-F][0-9A-F][0-9A-F]([0-9A-F][0-9A-F][0-9A-F])?;

FUNCNAME    : [A-Z][a-zA-z_0-9]*;

INTERROBANG : ('\u203D'|'‽'|'?!'|'!?');

COMMENT     : '/!' .*? '!\\' {skip();};

NL          : '\r'? '\n' {skip();};
WS          :  [ \t\u000C]+ {skip();} ;

// Used for case-insensitivity
fragment A:('a'|'A');
fragment B:('b'|'B');
fragment C:('c'|'C');
fragment D:('d'|'D');
fragment E:('e'|'E');
fragment F:('f'|'F');
fragment G:('g'|'G');
fragment H:('h'|'H');
fragment I:('i'|'I');
fragment J:('j'|'J');
fragment K:('k'|'K');
fragment L:('l'|'L');
fragment M:('m'|'M');
fragment N:('n'|'N');
fragment O:('o'|'O');
fragment P:('p'|'P');
fragment Q:('q'|'Q');
fragment R:('r'|'R');
fragment S:('s'|'S');
fragment T:('t'|'T');
fragment U:('u'|'U');
fragment V:('v'|'V');
fragment W:('w'|'W');
fragment X:('x'|'X');
fragment Y:('y'|'Y');
fragment Z:('z'|'Z');