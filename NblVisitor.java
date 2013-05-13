import java.util.Deque;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.PrintStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class NblVisitor extends NobleBaseVisitor<Object> {
	static final String CONSTANT_PREFIX = "const";
	static final String VAR_PREFIX = "var_";
	static final String ARR_PREFIX = "arr_";
	static final String LABEL_PREFIX = "l_";
	public static final String[] RESERVED_KEYWORDS = {
		"function", "switch", "if", "for", "repeat", "foreach", "do", "while", 
		"string_concat", "string_length", "string_equal", "string_to_integer",
		"get_input", "print_inline", 
		"integer_to_float", "float_to_integer", "integer_to_string"
	};
	class State {
		public int tmpCounter;
		public int labelCounter;
		public PrintWriter outputStream;

		public State(int _t, int _l, PrintWriter _o) {
			tmpCounter = _t;
			labelCounter = _l;
			outputStream = _o;
		}
	}

	HashMap<String, NblValue> constants;
	HashMap<String, NblValue> vars;
	HashMap<String, String> varTypes;
	HashMap<String, Integer> varIterations;
	HashMap<String, String> functions;
	HashMap<String, String> functionTypes;
	HashMap<String, Integer> arraySizes;
	HashMap<String, Integer> functionReturningArray;
	PrintWriter mainOutputStream;
	PrintWriter os;
	Deque<State> stateStack;
	Deque<String> varStack;
	int constantCounter;
	int varCounter;
	int funcCounter;
	int labelCounter;
	int tmpCounter;
	String currentFunction;
	Stack currentBreakLabel;

	public NblVisitor(OutputStream _os) {
		super();
		mainOutputStream = new PrintWriter(_os, true);
		os = mainOutputStream;
		stateStack = new ArrayDeque<State>();
		varStack = new ArrayDeque<String>();

		functions = new HashMap<String, String>();
		funcCounter = 0;
		constants = new HashMap<String, NblValue>();
		constantCounter = 0;
		vars = new HashMap<String, NblValue>();
		varTypes = new HashMap<String, String>();
		varIterations = new HashMap<String, Integer>();
		functionTypes = new HashMap<String, String>();
		arraySizes = new HashMap<String, Integer>();
		functionReturningArray = new HashMap<String, Integer>();
		varCounter = 0;
		tmpCounter = 1;
		currentFunction = "";
		currentBreakLabel = new Stack();
	}

	public Object visitR(NobleParser.RContext ctx){
		for(NobleParser.ClazzContext clazz : ctx.clazz()){
			NobleParser.FunctionDefContext function = (NobleParser.FunctionDefContext) clazz;
			String functionName = function.function().FUNCNAME().getText();
			if(function.function().func_block().func_return().getText()!=""){
				if(function.function().func_block().func_return().expr() instanceof NobleParser.ArrayCollectiveContext){
					NobleParser.ArrayCollectiveContext arrCtx = (NobleParser.ArrayCollectiveContext) function.function().func_block().func_return().expr();
					if(arrCtx.array() instanceof NobleParser.ArrayInitContext){
						NobleParser.ArrayInitContext arr = (NobleParser.ArrayInitContext)arrCtx.array();
						Integer arrSize = new Integer(arr.INT().getText());

						functionReturningArray.put(functionName, arrSize);
					}
				}
			}

			

		}
		return visitChildren(ctx);
	}


	public Integer visitFunctionDef(NobleParser.FunctionDefContext ctx) {
		tmpCounter = 1;
		currentFunction = ctx.function().FUNCNAME().getText();

		if(Arrays.asList(RESERVED_KEYWORDS).contains(currentFunction.toLowerCase())){
			throw new RuntimeException("\""+currentFunction+"\" is a reserved keyword and cannot be used as a function name.");
		}

		os.println("; Function def - "+currentFunction);
	
		String return_type = "i32";
		if(functionReturningArray.get(currentFunction)!=null){
			Integer arrSize = functionReturningArray.get(currentFunction);
			return_type = "["+arrSize+" x %stackelement]*";
		}

		os.print("define "+return_type+" @fun_" + ctx.function().FUNCNAME().getText());
		os.print("(");
		if(ctx.function().pars()!=null){
			for(int i=0;i<ctx.function().pars().size();i++){
				if(ctx.function().pars().get(i).ID()!=null){
					String literalName = new String(ctx.function().pars().get(i).ID().getText());
					String arg = getVarName(literalName).replace('@','%');
					os.print("%stackelement "+arg+"_arg");
					if(i<ctx.function().pars().size()-1){
						os.print(", ");
					}
				}else{
					

					String literalName = new String(ctx.function().pars().get(i).arraypar().ID().getText());
					Integer arrSize = new Integer(ctx.function().pars().get(i).arraypar().INT().getText());
					String arg = getVarName(literalName).replace('@','%');
					os.print("["+arrSize+" x %stackelement] "+arg+"_arg");
					if(i<ctx.function().pars().size()-1){
						os.print(", ");
					}

				}
			}
		}
		os.print(") ");
		os.println("{");

		if(ctx.function().pars()!=null){
			for(int i=0;i<ctx.function().pars().size();i++){
				if(ctx.function().pars().get(i).ID()!=null){
					String literalName = new String(ctx.function().pars().get(i).ID().getText());
					String varName = getVarName(literalName).replace('@','%');
					
					_("; Allocate arg into a variable");
					_(varName + " = alloca %stackelement");				
					NblValue type = new NblValue<Integer>(NblValue.NblValueType.INTEGER, 0);
					vars.put(varName, type);				
					_("store %stackelement "+varName+"_arg, %stackelement* "+varName);
				}else{

					String literalName = new String(ctx.function().pars().get(i).arraypar().ID().getText());
					Integer arrSize = new Integer(ctx.function().pars().get(i).arraypar().INT().getText());
					String varName = getVarName(literalName).replace('@','%');

					_("; Allocate arg into a variable");
					_(varName + " = alloca ["+arrSize+" x %stackelement]");				
					NblValue type = new NblValue<Integer>(NblValue.NblValueType.ARRAY, 0);
					vars.put(varName, type);
					arraySizes.put(varName, arrSize);
					_("store ["+arrSize+" x %stackelement] "+varName+"_arg, ["+arrSize+" x %stackelement]* "+varName);

				}
			}
		}

		
		visit(ctx.function().func_block());
		_("; Function block finished");

		if(ctx.function().func_block().func_return().getText().length()==0){
			_("ret i32 0");
		}else{
			if(functionReturningArray.get(currentFunction)==null){
				visit(ctx.function().func_block().func_return().expr());
				_("%rv = call i32 @get_top()");
				_("ret i32 %rv");
			}else{
				NobleParser.ArrayCollectiveContext arrCtx = (NobleParser.ArrayCollectiveContext) ctx.function().func_block().func_return().expr();
				if(arrCtx.array() instanceof NobleParser.ArrayInitContext){
					NobleParser.ArrayInitContext arr = (NobleParser.ArrayInitContext)arrCtx.array();
					Integer arrSize = new Integer(arr.INT().getText());
					String arrLiteralName = new String(arr.ID().getText());
					String arrVarName = getVarName(arrLiteralName);

					String arrIndexPtr1 = getTemporary();
					_(arrIndexPtr1 + " = getelementptr ["+arrSize+" x %stackelement]* "+arrVarName+", i32 0");

					_("ret ["+arrSize+" x %stackelement]* "+arrIndexPtr1);
				}
			}
		}
		os.println("}");

		return 0;
	}
	public String visitFunctionCall(NobleParser.FunctionCallContext ctx) {
		String func_name = ctx.functioncall().FUNCNAME().getText();
		_("; Calling function "+func_name);
		String param_vars = "";
		if(ctx.functioncall().parlist()!=null){
			for(int i=0; i<ctx.functioncall().parlist().size();i++){
				if(ctx.functioncall().parlist().get(i).arraypar() == null){
					visit(ctx.functioncall().parlist().get(i));
					_("; Arg "+i);
					_("call void @pop()");		
					String def1 = getTemporary();
					_("" + def1 + " = load %stackelement* @svalue");

					if(i>0){
						param_vars += ", ";
					}
					param_vars += "%stackelement "+def1;
				}else{
					String literalName = new String(ctx.functioncall().parlist().get(i).arraypar().ID().getText());
					String varName = getVarName(literalName);

					Integer arrSize = new Integer(ctx.functioncall().parlist().get(i).arraypar().INT().getText());

					String def1 = getTemporary();
					_(def1 + " = load ["+arrSize+" x %stackelement]* "+varName);

					if(i>0){
						param_vars += ", ";
					}
					param_vars += "["+arrSize+" x %stackelement] "+def1;
				}
			}
		}
		String call = getTemporary();
		if(functionReturningArray.get(func_name)==null){
			_(call + " = call i32 @fun_"+func_name+"("+param_vars+")");
		}else{
			Integer arrSize = functionReturningArray.get(func_name);
			_(call + " = call ["+arrSize+" x %stackelement]* @fun_"+func_name+"("+param_vars+")");			
		}
		
		return call;
	}
	public Integer visitVariable(NobleParser.VariableContext ctx) {		
		String literalName = new String(ctx.getText());
		_("; Load - " + literalName);
		String varType = getVarType(literalName);
		String varName = getVarName(literalName);



		String loadTmp = getTemporary();
		_(loadTmp + " = load %stackelement* "+varName);
		// Type
		String type = getTemporary();
		_(type + " = extractvalue  %stackelement "+loadTmp+", 0");
		// Value
		String val = getTemporary();
		_(val + " = extractvalue  %stackelement "+loadTmp+", 1");

		_("call void @push(i8 "+type+", i8* "+val+")");
	
		return 0;
	}
	public Integer visitBool(NobleParser.BoolContext ctx) {
		boolean b = ctx.BOOL().getText().toLowerCase().equals("true");

		_("; " + b);
		String bp = getTemporary();
		_("" + bp + " = inttoptr i1 " + (b ? "1" : "0") + " to i8*");
		_("call void @push(i8 3, i8* " + bp + ")");
		return 0;
	}
	public Integer visitInt(NobleParser.IntContext ctx) {
		Integer i = new Integer(ctx.getChild(0).getText());
		_("; " + i);

		String ip = getTemporary();
		_(ip + " = inttoptr i32 " + i + " to i8*");
		_("call void @push(i8 0, i8* " + ip + ")");

		return 0;
	}
	public Integer visitString(NobleParser.StringContext ctx) {
		String news = new String(ctx.STRING().getText());
		news = news.substring(1,news.length()-1);

		if(!news.contains("\\00")){
			news = news+"\\00";
		}
		news = "\""+news+"\"";
		String valName = getConstant();
		NblValue<String> pslv = new NblValue<String>(NblValue.NblValueType.STRING, news);
		constants.put(valName, pslv);
		String vp = getTemporary();
		_(vp + " = bitcast " + typeForString(news) + "* " + valName + " to i8*");
		_("call void @push(i8 1, i8* " + vp + ")");

		return 0;
	}
	public Integer visitChar(NobleParser.CharContext ctx) {
		String news = new String(ctx.CHAR().getText());
		news = news.substring(1,news.length()-1);
		_("; " + news);

		if(!news.contains("\\00")){
			news = news+"\\00";
		}
		news = "\""+news+"\"";
		String valName = getConstant();
		NblValue<String> pslv = new NblValue<String>(NblValue.NblValueType.CHAR, news);
		constants.put(valName, pslv);
		String vp = getTemporary();
		_(vp + " = bitcast " + typeForString(news) + "* " + valName + " to i8*");
		_("call void @push(i8 4, i8* " + vp + ")");

		return 0;
	}
	public Integer visitPrint(NobleParser.PrintContext ctx) {
		//Visit expression to print
		visit(ctx.expr());

		print(false);

		return 0;
	}
	public Integer visitLog(NobleParser.LogContext ctx) {
		//Visit expression to print
		visit(ctx.expr());
		print(true);
		return 0;
	}
	public Integer visitComparison(NobleParser.ComparisonContext ctx) {
		visit(ctx.expr(1));
		visit(ctx.expr(0));

		_("call void @pop()");		
		String def1 = getTemporary();
		_("" + def1 + " = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String def2 = getTemporary();
		_("" + def2 + " = load i8** "+ def1);
		String def3 = getTemporary();
		_("" + def3 + " = ptrtoint i8* " + def2 + " to i32");

		_("call void @pop()");
		String var1 = getTemporary();
		_("" + var1 + " = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String var2 = getTemporary();
		_("" + var2 + " = load i8** " + var1);
		String var3 = getTemporary();
		_("" + var3 + " = ptrtoint i8* " + var2 + " to i32");

		String boolTrue = getLabel();
		String boolFalse = getLabel();
		String breakString = getLabel();

		if(ctx.COMPARISON().getText().equals("==")){
			_("; ==");
			String result = getTemporary();
			_(result+" = icmp eq i32 "+def3+", "+var3);	
			_("br i1 "+result+", label %"+boolTrue+", label %"+boolFalse);
		}
		if(ctx.COMPARISON().getText().equals("!=")){
			_("; !=");
			String result = getTemporary();
			_(result+" = icmp eq i32 "+def3+", "+var3);	
			_("br i1 "+result+", label %"+boolFalse+", label %"+boolTrue);
		}
		if(ctx.COMPARISON().getText().equals("<=")){
			_("; <=");
			String result = getTemporary();
			_(result+" = icmp sle i32 "+def3+", "+var3);	
			_("br i1 "+result+", label %"+boolTrue+", label %"+boolFalse);
		}
		if(ctx.COMPARISON().getText().equals(">=")){
			_("; >=");
			String result = getTemporary();
			_(result+" = icmp sge i32 "+def3+", "+var3);	
			_("br i1 "+result+", label %"+boolTrue+", label %"+boolFalse);
		}
		if(ctx.COMPARISON().getText().equals("<")){
			_("; <");
			String result = getTemporary();
			_(result+" = icmp slt i32 "+def3+", "+var3);	
			_("br i1 "+result+", label %"+boolTrue+", label %"+boolFalse);
		}
		if(ctx.COMPARISON().getText().equals(">")){
			_("; >");
			String result = getTemporary();
			_(result+" = icmp sgt i32 "+def3+", "+var3);	
			_("br i1 "+result+", label %"+boolTrue+", label %"+boolFalse);
		}

		os.println(boolTrue+":");
		_("; Comparison - True");
		String bt = getTemporary();
		_(bt + " = inttoptr i1 1 to i8*");
		_("call void @push(i8 3, i8* " + bt + ")");
		_("br label %"+breakString);

		os.println(boolFalse+":");
		_("; Comparison - False");
		String bf = getTemporary();
		_(bf + " = inttoptr i1 0 to i8*");
		_("call void @push(i8 3, i8* " + bf + ")");
		_("br label %"+breakString);

		os.println(breakString+":");

		return 0;
	}
	public Integer visitAddSub(NobleParser.AddSubContext ctx) {
		//Visit right
		visit(ctx.expr(1));
		//Visit left
		visit(ctx.expr(0));

		_("call void @pop()");
		String v1 = getTemporary();
		_(v1 + " = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String v2 = getTemporary();
		_(v2 + " = load i8** " + v1);
		//int
		String val1Int = getTemporary();
		_(val1Int + " = ptrtoint i8* " + v2 + " to i32");
		
		//type
		String type1ptr = getTemporary();
		_(type1ptr + " = getelementptr %stackelement* @svalue, i32 0, i32 0");
		String type1val = getTemporary();
		_(type1val + " = load i8* " + type1ptr );

		_("call void @pop()");
		String v4 = getTemporary();
		_(v4 + " = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String v5 = getTemporary();
		_(v5 + " = load i8** " + v4);
		//int
		String val2Int = getTemporary();
		_(val2Int + " = ptrtoint i8* " + v5 + " to i32");
		
		//type
		String type2ptr = getTemporary();
		_(type2ptr + " = getelementptr %stackelement* @svalue, i32 0, i32 0");
		String type2val = getTemporary();
		_(type2val + " = load i8* " + type2ptr);

		String resumeLabel = getLabel();

		String ifSameType = getTemporary();
		_(ifSameType+" = icmp eq i8 "+type1val+", "+type2val);

		String isSameType = getLabel();
		String isNotSameType = getLabel();
		_("br i1 "+ifSameType+", label %"+isSameType+", label %"+isNotSameType);

		os.println(isNotSameType+":");
		String exTmp1 = getTemporary();
		_(exTmp1+" = getelementptr [43 x i8]* @calculationexception, i32 0, i32 0");
		_("call void @throw_exception(i8* "+exTmp1+")");
		_("unreachable");

		os.println(isSameType+":");

		String ifInt = getTemporary();
		_(ifInt+" = icmp eq i8 0, "+type1val);

		String isInt = getLabel();
		String isFloat = getLabel();
		_("br i1 "+ifInt+", label %"+isInt+", label %"+isFloat);
		
		os.println(isFloat+":");

		//float
		String v3 = getTemporary();
		_(v3 + " = bitcast i8* " + v2 + " to double*");
		String val1Float = getTemporary();
		_(val1Float + " = load double* " + v3);

		//float
		String v6 = getTemporary();
		_(v6 + " = bitcast i8* " + v5 + " to double*");
		String val2Float = getTemporary();
		_(val2Float + " = load double* " + v6);
		
		switch(ctx.op.getType()) {
			case NobleParser.OP_ADD:
				_("; Addition");
				// Add Floats
				_(";Add Floats");
				String r = getTemporary();
				_(r + " = fadd double " + val1Float + ", " + val2Float);
				String tmp1 = getTemporary();
				_(tmp1 +" = alloca double");
				_("store double " + r + ", double* "+tmp1);
				String tmp2 = getTemporary();
				_(tmp2 + " = bitcast double* "+tmp1 + " to i8*");
				_("call void @push(i8 6, i8* " + tmp2 + ")");
				break;
			case NobleParser.OP_SUB:
				_("; Subtraction");
				// Sub Floats
				_(";Sub Floats");
				r = getTemporary();
				_(r + " = fsub double " + val1Float + ", " + val2Float);
				tmp1 = getTemporary();
				_(tmp1 +" = alloca double");
				_("store double " + r + ", double* "+tmp1);
				tmp2 = getTemporary();
				_(tmp2 + " = bitcast double* "+tmp1 + " to i8*");
				_("call void @push(i8 6, i8* " + tmp2 + ")");
				break;
		}
		_("br label %"+resumeLabel);

		os.println(isInt+":");
		switch(ctx.op.getType()) {
			case NobleParser.OP_ADD:
				_("; Addition");
				// Add Integers
				_(";Add Integers");
				String r = getTemporary();
				_(r + " = add i32 " + val1Int + ", " + val2Int);
				String rp = getTemporary();
				_(rp + " = inttoptr i32 " + r + " to i8*");
				_("call void @push(i8 0, i8* " + rp + ")");
				break;
			case NobleParser.OP_SUB:
				_("; Subtraction");
				// Sub Integers
				_(";Sub Ints");
				r = getTemporary();
				_(r + " = sub i32 " + val1Int + ", " + val2Int);
				rp = getTemporary();
				_(rp + " = inttoptr i32 " + r + " to i8*");
				_("call void @push(i8 0, i8* " + rp + ")");
				break;
		}
		_("br label %"+resumeLabel);

		
		os.println(resumeLabel+":");
		return 0;
	}
	public Integer visitMulDiv(NobleParser.MulDivContext ctx) {
		//Visit right
		visit(ctx.expr(1));
		//Visit left
		visit(ctx.expr(0));

		_("call void @pop()");
		String v1 = getTemporary();
		_(v1 + " = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String v2 = getTemporary();
		_(v2 + " = load i8** " + v1);
		//int
		String val1Int = getTemporary();
		_(val1Int + " = ptrtoint i8* " + v2 + " to i32");
		
		//type
		String type1ptr = getTemporary();
		_(type1ptr + " = getelementptr %stackelement* @svalue, i32 0, i32 0");
		String type1val = getTemporary();
		_(type1val + " = load i8* " + type1ptr );

		_("call void @pop()");
		String v4 = getTemporary();
		_(v4 + " = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String v5 = getTemporary();
		_(v5 + " = load i8** " + v4);
		//int
		String val2Int = getTemporary();
		_(val2Int + " = ptrtoint i8* " + v5 + " to i32");
		
		//type
		String type2ptr = getTemporary();
		_(type2ptr + " = getelementptr %stackelement* @svalue, i32 0, i32 0");
		String type2val = getTemporary();
		_(type2val + " = load i8* " + type2ptr);

		String resumeLabel = getLabel();

		String ifSameType = getTemporary();
		_(ifSameType+" = icmp eq i8 "+type1val+", "+type2val);

		String isSameType = getLabel();
		String isNotSameType = getLabel();
		_("br i1 "+ifSameType+", label %"+isSameType+", label %"+isNotSameType);

		os.println(isNotSameType+":");
		String exTmp1 = getTemporary();
		_(exTmp1+" = getelementptr [43 x i8]* @calculationexception, i32 0, i32 0");
		_("call void @throw_exception(i8* "+exTmp1+")");
		_("unreachable");

		os.println(isSameType+":");

		String ifInt = getTemporary();
		_(ifInt+" = icmp eq i8 0, "+type1val);

		String isInt = getLabel();
		String isFloat = getLabel();
		_("br i1 "+ifInt+", label %"+isInt+", label %"+isFloat);
		
		os.println(isFloat+":");

		//float
		String v3 = getTemporary();
		_(v3 + " = bitcast i8* " + v2 + " to double*");
		String val1Float = getTemporary();
		_(val1Float + " = load double* " + v3);

		//float
		String v6 = getTemporary();
		_(v6 + " = bitcast i8* " + v5 + " to double*");
		String val2Float = getTemporary();
		_(val2Float + " = load double* " + v6);
		
		switch(ctx.op.getType()) {
			case NobleParser.OP_MULT:
				_("; Multiplication");
				_(";Multiply Floats");
				String r = getTemporary();
				_(r + " = fmul double " + val1Float + ", " + val2Float);
				String tmp1 = getTemporary();
				_(tmp1 +" = alloca double");
				_("store double " + r + ", double* "+tmp1);
				String tmp2 = getTemporary();
				_(tmp2 + " = bitcast double* "+tmp1 + " to i8*");
				_("call void @push(i8 6, i8* " + tmp2 + ")");
				break;
			case NobleParser.OP_DIV:
				_("; Division");
				_(";Divide Floats");
				r = getTemporary();
				_(r + " = fdiv double " + val1Float + ", " + val2Float);
				tmp1 = getTemporary();
				_(tmp1 +" = alloca double");
				_("store double " + r + ", double* "+tmp1);
				tmp2 = getTemporary();
				_(tmp2 + " = bitcast double* "+tmp1 + " to i8*");
				_("call void @push(i8 6, i8* " + tmp2 + ")");
				break;
		}
		_("br label %"+resumeLabel);

		os.println(isInt+":");
		switch(ctx.op.getType()) {
			case NobleParser.OP_MULT:
				_("; Multiplication");
				_(";Multiply Integers");
				String r = getTemporary();
				_(r + " = mul i32 " + val1Int + ", " + val2Int);
				String rp = getTemporary();
				_(rp + " = inttoptr i32 " + r + " to i8*");
				_("call void @push(i8 0, i8* " + rp + ")");
				break;
			case NobleParser.OP_DIV:
				_("; Division");
				_(";Divide Ints");
				r = getTemporary();
				_(r + " = sdiv i32 " + val1Int + ", " + val2Int);
				rp = getTemporary();
				_(rp + " = inttoptr i32 " + r + " to i8*");
				_("call void @push(i8 0, i8* " + rp + ")");
				break;
		}
		_("br label %"+resumeLabel);

		
		os.println(resumeLabel+":");
		return 0;
	}
	public Integer visitException(NobleParser.ExceptionContext ctx) {
		visit(ctx.exep().expr());

		_("call void @pop()");
		String vloc2 = getTemporary();
		_(vloc2+" = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String valp = getTemporary();
		_(valp+" = load i8** "+vloc2);
		_("call void @throw_exception(i8* "+valp+")");
		return 0;
	}
	public Integer visitIf(NobleParser.IfContext ctx) {
		_("; Starting if");
		visit(ctx.expr());
		
		_(";if");

		_("call void @pop()");
		String vloc = getTemporary();
		_(vloc+" = getelementptr %stackelement* @svalue, i32 0, i32 0");
		String valtype = getTemporary();
		_(valtype+" = load i8* "+vloc);

		String vloc2 = getTemporary();
		_(vloc2+" = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String valp = getTemporary();
		_(valp+" = load i8** "+vloc2);
		String val = getTemporary();
		_(val+" = ptrtoint i8* "+valp+" to i32");

		String iftmp1 = getTemporary();
		_(iftmp1+" = icmp eq i8 3, "+valtype);
		String isValidIf = getLabel();
		String notValidIf = getLabel();
		_("br i1 "+iftmp1+", label %"+isValidIf+", label %"+notValidIf);

		//Is a valid if
		os.println(isValidIf+":");
		_(";if valid");
		String iftmp2 = getTemporary();
		_(iftmp2+" = icmp eq i32 1, "+val);
		String ifTrue = getLabel();
		String resume = getLabel();
		_("br i1 "+iftmp2+", label %"+ifTrue+", label %"+resume);

		os.println(ifTrue+":");
		_(";if true - calling func_block");
		visit(ctx.func_block());
		_(";if true - func_block finished");
		_("br label %"+resume);


		os.println(notValidIf+":");
		String exTmp1 = getTemporary();
		_(exTmp1+" = getelementptr [21 x i8]* @ifexception, i32 0, i32 0");
		_("call void @throw_exception(i8* "+exTmp1+")");
		_("br label %"+resume);

		os.println(resume+":");

		return 0;
	}
	public Integer visitIfElse(NobleParser.IfElseContext ctx) {
		_("; Starting if");
		visit(ctx.expr());
		
		_("; if");

		_("call void @pop()");
		String vloc = getTemporary();
		_(vloc+" = getelementptr %stackelement* @svalue, i32 0, i32 0");
		String valtype = getTemporary();
		_(valtype+" = load i8* "+vloc);

		String vloc2 = getTemporary();
		_(vloc2+" = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String valp = getTemporary();
		_(valp+" = load i8** "+vloc2);
		String val = getTemporary();
		_(val+" = ptrtoint i8* "+valp+" to i32");

		String iftmp1 = getTemporary();
		_(iftmp1+" = icmp eq i8 3, "+valtype);
		String isValidIf = getLabel();
		String notValidIf = getLabel();
		_("br i1 "+iftmp1+", label %"+isValidIf+", label %"+notValidIf);

		//Is a valid if
		os.println(isValidIf+":");
		_(";if comparison valid");
		String iftmp2 = getTemporary();
		_(iftmp2+" = icmp eq i32 1, "+val);
		String ifTrue = getLabel();
		String ifFalse = getLabel();
		String resume = getLabel();
		_("br i1 "+iftmp2+", label %"+ifTrue+", label %"+ifFalse);

		os.println(ifTrue+":");
		_(";if true");
		visit(ctx.func_block(0));
		_("br label %"+resume);

		os.println(ifFalse+":");
		_(";if false");
		visit(ctx.func_block(1));
		_("br label %"+resume);


		os.println(notValidIf+":");
		_(";if comparison invalid - throw exception");
		String exTmp1 = getTemporary();
		_(exTmp1+" = getelementptr [21 x i8]* @ifexception, i32 0, i32 0");
		_("call void @throw_exception(i8* "+exTmp1+")");
		_("br label %"+resume);

		os.println(resume+":");

		return 0;
	}
	public Integer visitRepeat(NobleParser.RepeatContext ctx) {
		
		String entryLabel = getLabel();
		String exitLabel = getLabel();
		currentBreakLabel.push(exitLabel);


		try{
			//Is int...
			Integer count = new Integer(ctx.INT().getText());

			//Start of loop
			_("br label %"+entryLabel);
			os.println(entryLabel+":");

			// generate repeated code
			for(int i=0;i<count;i++){
				visit(ctx.func_block());
			}
			
			_("br label %"+exitLabel);
		}catch(Exception ex){
			//Is ID...

			String literalName = new String(ctx.ID().getText());
			String varType = getVarType(literalName);

			if(!varType.equals("0")){
				throw new RuntimeException("Repeat can only take an integer");
			}

			String varName = getVarName(literalName);
			String loadTmp = getTemporary();
			_(loadTmp + " = load %stackelement* "+varName);
			// Type
			String type = getTemporary();
			_(type + " = extractvalue  %stackelement "+loadTmp+", 0");
			// Value
			String valTmp = getTemporary();
			_(valTmp + " = extractvalue  %stackelement "+loadTmp+", 1");
			String val = getTemporary();
			_(val + " = ptrtoint i8* "+valTmp+" to i32");


			_(";Creating loop counter");
			String loopCountdown = getConstant();
			NblValue<Integer> pslv = new NblValue<Integer>(NblValue.NblValueType.INTEGER, 0);
			vars.put(loopCountdown, pslv);
			_("store i32 "+val+", i32* "+loopCountdown);

			//Start of loop
			_("br label %"+entryLabel);
			os.println(entryLabel+":");

			_(";Check int isn't at 0 or less");
			String temp1 = getTemporary();
			_(temp1+" = getelementptr i32* "+loopCountdown+", i32 0");
			String temp2 = getTemporary();
			_(temp2+" = load i32* "+temp1);
			String ifTmp1 = getTemporary();
			_(ifTmp1+" = icmp ole i32 0, "+temp2);
			String continueLabel = getLabel();
			_("br i1 "+ifTmp1+", label %"+exitLabel+", label %"+continueLabel);

			os.println(continueLabel+":");

			//visit expr
			visit(ctx.func_block());

			_(";Update loop countdown");
			String newVal = getTemporary();
			_(newVal + " = load  i32* "+loopCountdown);
			String tmpVal = getTemporary();
			_(tmpVal + " = sub i32 "+newVal+", 1");
			_("store i32 "+tmpVal+", i32* "+loopCountdown);

			//loop
			_("br label %"+entryLabel);

		}

		//End of loop
		os.println(exitLabel+":");
		currentBreakLabel.pop();

		return 0;
	}
	public Integer visitBreak(NobleParser.BreakContext ctx) {
		_("; breaking!");
		getTemporary();getTemporary(); //Hack - Not sure why
		_("br label %"+currentBreakLabel.peek());
		_("unreachable");
		return 0;
	}
	public Integer visitColour(NobleParser.ColourContext ctx) {
		String news = new String(ctx.HEXCOLOUR().getText());
		_("; " + news);

		if(!news.contains("\\00")){
			news = news+"\\00";
		}
		news = "\""+news+"\"";
		String valName = getConstant();
		NblValue<String> pslv = new NblValue<String>(NblValue.NblValueType.COLOUR, news);
		constants.put(valName, pslv);
		String vp = getTemporary();
		_(vp + " = bitcast " + typeForString(news) + "* " + valName + " to i8*");
		_("call void @push(i8 5, i8* " + vp + ")");

		return 0;
	}
	public Integer visitFloat(NobleParser.FloatContext ctx) {
		Float i = new Float(ctx.getChild(0).getText());
		_("; " + i);

		String tmp1 = getTemporary();
		_(tmp1 +" = alloca double");
		_("store double " + i + ", double* "+tmp1);
		String tmp3 = getTemporary();
		_(tmp3 + " = bitcast double* "+tmp1 + " to i8*");
		_("call void @push(i8 6, i8* " + tmp3 + ")");
		return 0;
	}
	public Integer visitFor(NobleParser.ForContext ctx) {

		if(ctx.for_assign()!=null){
			visit(ctx.for_assign());
		}

		String entryLabel = getLabel();
		String exitLabel = getLabel();
		currentBreakLabel.push(exitLabel);


		//Start of loop
		_("br label %"+entryLabel);
		os.println(entryLabel+":");

		visit(ctx.for_test());

		_("call void @pop()");
		String vloc = getTemporary();
		_(vloc+" = getelementptr %stackelement* @svalue, i32 0, i32 0");
		String valtype = getTemporary();
		_(valtype+" = load i8* "+vloc);

		String vloc2 = getTemporary();
		_(vloc2+" = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String valp = getTemporary();
		_(valp+" = load i8** "+vloc2);
		String val = getTemporary();
		_(val+" = ptrtoint i8* "+valp+" to i1");

		String ifTmp1 = getTemporary();
		_(ifTmp1+" = icmp eq i1 1, "+val);

		String continueLabel = getLabel();
		_("br i1 "+ifTmp1+", label %"+continueLabel+", label %"+exitLabel);

		os.println(continueLabel+":");

		visit(ctx.func_block());

		// Update value if present
		if(ctx.for_update()!=null){
			visit(ctx.for_update());
		}

		//loop
		_("br label %"+entryLabel);


		//End of loop
		os.println(exitLabel+":");
		currentBreakLabel.pop();

		return 0;
	}
	public Integer visitDoWhile(NobleParser.DoWhileContext ctx) {
		_("; DoWhile");

		String start = getLabel();
		_("br label %"+start);
		os.println(start+":");

		_(";Run function block");
		visit(ctx.func_block());

		_(";Check if need to loop again");
		visit(ctx.expr());

		_("call void @pop()");
		String vloc = getTemporary();
		_(vloc+" = getelementptr %stackelement* @svalue, i32 0, i32 0");
		String valtype = getTemporary();
		_(valtype+" = load i8* "+vloc);

		String vloc2 = getTemporary();
		_(vloc2+" = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String valp = getTemporary();
		_(valp+" = load i8** "+vloc2);
		String val = getTemporary();
		_(val+" = ptrtoint i8* "+valp+" to i32");

		String iftmp1 = getTemporary();
		_(iftmp1+" = icmp eq i8 3, "+valtype);
		String isValidIf = getLabel();
		String notValidIf = getLabel();
		_("br i1 "+iftmp1+", label %"+isValidIf+", label %"+notValidIf);

		//Is a valid if
		os.println(isValidIf+":");
		_(";if valid");
		String iftmp2 = getTemporary();
		_(iftmp2+" = icmp eq i32 1, "+val);
		String resume = getLabel();
		_("br i1 "+iftmp2+", label %"+start+", label %"+resume);

		os.println(notValidIf+":");
		String exTmp1 = getTemporary();
		_(exTmp1+" = getelementptr [24 x i8]* @whileexception, i32 0, i32 0");
		_("call void @throw_exception(i8* "+exTmp1+")");
		_("br label %"+resume);

		os.println(resume+":");

		return 0;
	}
	public Integer visitWhile(NobleParser.WhileContext ctx) {
		_("; While");

		String start = getLabel();
		String continueLabel = getLabel();
		_("br label %"+start);
		os.println(start+":");


		_(";Check if need to loop again");
		visit(ctx.expr());

		_("call void @pop()");
		String vloc = getTemporary();
		_(vloc+" = getelementptr %stackelement* @svalue, i32 0, i32 0");
		String valtype = getTemporary();
		_(valtype+" = load i8* "+vloc);

		String vloc2 = getTemporary();
		_(vloc2+" = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String valp = getTemporary();
		_(valp+" = load i8** "+vloc2);
		String val = getTemporary();
		_(val+" = ptrtoint i8* "+valp+" to i32");

		String iftmp1 = getTemporary();
		_(iftmp1+" = icmp eq i8 3, "+valtype);
		String isValidIf = getLabel();
		String notValidIf = getLabel();
		_("br i1 "+iftmp1+", label %"+isValidIf+", label %"+notValidIf);

		//Is a valid if
		os.println(isValidIf+":");
		_(";if valid");
		String iftmp2 = getTemporary();
		_(iftmp2+" = icmp eq i32 1, "+val);
		String resume = getLabel();
		_("br i1 "+iftmp2+", label %"+continueLabel+", label %"+resume);


		os.println(continueLabel+":");
		_(";Run function block");
		visit(ctx.func_block());
		_("br label %"+start);

		os.println(notValidIf+":");
		String exTmp1 = getTemporary();
		_(exTmp1+" = getelementptr [24 x i8]* @whileexception, i32 0, i32 0");
		_("call void @throw_exception(i8* "+exTmp1+")");
		_("br label %"+resume);

		os.println(resume+":");

		return 0;
	}
	public Integer visitNull(NobleParser.NullContext ctx) {
		_("; NULL");

		_("call void @push(i8 7, i8* null)");

		return 0;
	}
	public Integer visitAnd(NobleParser.AndContext ctx) {
		visit(ctx.expr(0));
		visit(ctx.expr(1));

		_("call void @pop()");		
		String def1 = getTemporary();
		_("" + def1 + " = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String def2 = getTemporary();
		_("" + def2 + " = load i8** "+ def1);
		String def3 = getTemporary();
		_("" + def3 + " = ptrtoint i8* " + def2 + " to i1");

		_("call void @pop()");
		String var1 = getTemporary();
		_("" + var1 + " = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String var2 = getTemporary();
		_("" + var2 + " = load i8** " + var1);
		String var3 = getTemporary();
		_("" + var3 + " = ptrtoint i8* " + var2 + " to i1");

		String returnFalse = getLabel();
		String returnTrue = getLabel();
		String areEqual = getLabel();		
		String breakString = getLabel();

		String ifEqual = getTemporary();
		_(ifEqual+" = icmp eq i1 "+def3+", "+var3);	
		_("br i1 "+ifEqual+", label %"+areEqual+", label %"+returnFalse);

		os.println(areEqual+":");
		String isTrue = getTemporary();
		_(isTrue+" = icmp eq i1 1, "+var3);	
		_("br i1 "+isTrue+", label %"+returnTrue+", label %"+returnFalse);

		os.println(returnTrue+":");
		_("; Comparison - True");
		String bt = getTemporary();
		_(bt + " = inttoptr i1 1 to i8*");
		_("call void @push(i8 3, i8* " + bt + ")");
		_("br label %"+breakString);

		os.println(returnFalse+":");
		_("; Comparison - False");
		String bf = getTemporary();
		_(bf + " = inttoptr i1 0 to i8*");
		_("call void @push(i8 3, i8* " + bf + ")");
		_("br label %"+breakString);

		os.println(breakString+":");
		return 0;
	}
	public Integer visitOr(NobleParser.OrContext ctx) {
		visit(ctx.expr(0));
		visit(ctx.expr(1));

		_("call void @pop()");		
		String def1 = getTemporary();
		_("" + def1 + " = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String def2 = getTemporary();
		_("" + def2 + " = load i8** "+ def1);
		String def3 = getTemporary();
		_("" + def3 + " = ptrtoint i8* " + def2 + " to i1");

		_("call void @pop()");
		String var1 = getTemporary();
		_("" + var1 + " = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String var2 = getTemporary();
		_("" + var2 + " = load i8** " + var1);
		String var3 = getTemporary();
		_("" + var3 + " = ptrtoint i8* " + var2 + " to i1");

		String returnFalse = getLabel();
		String returnTrue = getLabel();
		String secondCheck = getLabel();		
		String breakString = getLabel();

		String firstCheck = getTemporary();
		_(firstCheck+" = icmp eq i1 1, "+def3);	
		_("br i1 "+firstCheck+", label %"+returnTrue+", label %"+secondCheck);

		os.println(secondCheck+":");
		String secondIf = getTemporary();
		_(secondIf+" = icmp eq i1 1, "+var3);	
		_("br i1 "+secondIf+", label %"+returnTrue+", label %"+returnFalse);


		os.println(returnTrue+":");
		_("; Comparison - True");
		String bt = getTemporary();
		_(bt + " = inttoptr i1 1 to i8*");
		_("call void @push(i8 3, i8* " + bt + ")");
		_("br label %"+breakString);

		os.println(returnFalse+":");
		_("; Comparison - False");
		String bf = getTemporary();
		_(bf + " = inttoptr i1 0 to i8*");
		_("call void @push(i8 3, i8* " + bf + ")");
		_("br label %"+breakString);

		os.println(breakString+":");
		return 0;
	}
	public Integer visitParens(NobleParser.ParensContext ctx) {
		visit(ctx.expr());
		return 0;
	}
	public Integer visitNot(NobleParser.NotContext ctx) {
		visit(ctx.expr());

		_("call void @pop()");
		String var1 = getTemporary();
		_(var1 + " = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String var2 = getTemporary();
		_(var2 + " = load i8** " + var1);
		String var3 = getTemporary();
		_(var3 + " = ptrtoint i8* " + var2 + " to i1");

		String returnFalse = getLabel();
		String returnTrue = getLabel();	
		String breakString = getLabel();

		String ifEqual = getTemporary();
		_(ifEqual+" = icmp eq i1 0, "+var3);	
		_("br i1 "+ifEqual+", label %"+returnTrue+", label %"+returnFalse);

		os.println(returnTrue+":");
		_("; Comparison - True");
		String bt = getTemporary();
		_(bt + " = inttoptr i1 1 to i8*");
		_("call void @push(i8 3, i8* " + bt + ")");
		_("br label %"+breakString);

		os.println(returnFalse+":");
		_("; Comparison - False");
		String bf = getTemporary();
		_(bf + " = inttoptr i1 0 to i8*");
		_("call void @push(i8 3, i8* " + bf + ")");
		_("br label %"+breakString);

		os.println(breakString+":");

		return 0;
	}
	public Integer visitAssignVar(NobleParser.AssignVarContext ctx) {
		_("; Assign variable");
		if(ctx.expr(0).getClass()==ctx.expr(1).getClass()){
			if(ctx.expr(1) instanceof NobleParser.ArrayCollectiveContext){
				NobleParser.ArrayCollectiveContext arrCtx0 = (NobleParser.ArrayCollectiveContext) ctx.expr(0);
				NobleParser.ArrayCollectiveContext arrCtx1 = (NobleParser.ArrayCollectiveContext) ctx.expr(1);
				if(arrCtx0.array() instanceof NobleParser.ArrayInitContext && arrCtx1.array() instanceof NobleParser.ArrayInitContext){
					NobleParser.ArrayInitContext arr0 = (NobleParser.ArrayInitContext)arrCtx0.array();
					NobleParser.ArrayInitContext arr1 = (NobleParser.ArrayInitContext)arrCtx1.array();

					String literalName0 = new String(arr0.ID().getText());
					String varName0 = getVarName(literalName0);
					Integer arrSize0 = new Integer(arr0.INT().getText());

					String literalName1 = new String(arr1.ID().getText());
					String varName1 = getVarName(literalName1);
					Integer arrSize1 = new Integer(arr1.INT().getText());

					if(!arrSize0.equals(arrSize1)){
						throw new RuntimeException("Invalid number of array objets provided");
					}

					visit(arr0);

					for(int i=0; i<arrSize0;i++){				

						String arrIndexPtr1 = getTemporary();
						_(arrIndexPtr1 + " = getelementptr ["+arrSize1+" x %stackelement]* "+varName1+", i32 0, i32 "+i);


						String loadTmp = getTemporary();
						_(loadTmp + " = load %stackelement* "+arrIndexPtr1);
						
						//Store value
						String arrIndexPtr0 = getTemporary();
						_(arrIndexPtr0 + " = getelementptr ["+arrSize0+" x %stackelement]* "+varName0+", i32 0, i32 "+i);
						
						_("store %stackelement "+loadTmp+", %stackelement* "+arrIndexPtr0);
					}


					return 0;
				}			
			}
		}else if(ctx.expr(0) instanceof NobleParser.ArrayCollectiveContext){
			NobleParser.ArrayCollectiveContext arrCtx = (NobleParser.ArrayCollectiveContext) ctx.expr(0);
			if(arrCtx.array() instanceof NobleParser.ArrayInitContext && ctx.expr(1) instanceof NobleParser.FunctionCallContext){
				NobleParser.ArrayInitContext arr0 = (NobleParser.ArrayInitContext)arrCtx.array();
				String literalName0 = new String(arr0.ID().getText());
				String varName0 = getVarName(literalName0);
				Integer arrSize0 = new Integer(arr0.INT().getText());


				NobleParser.FunctionCallContext funcCallCtx = (NobleParser.FunctionCallContext)ctx.expr(1);

				String functionName = funcCallCtx.functioncall().FUNCNAME().getText();
				Integer funcArrSize = functionReturningArray.get(functionName);

				if(!arrSize0.equals(funcArrSize)){
					throw new RuntimeException("Invalid number of array objets provided");
				}

				visit(arr0);


				Object funcCall = visit(funcCallCtx);


				for(int i=0; i<arrSize0;i++){				
					String arrIndexPtr1 = getTemporary();
					_(arrIndexPtr1 + " = getelementptr ["+arrSize0+" x %stackelement]* "+funcCall+", i32 0, i32 "+i);

					String loadTmp = getTemporary();
					_(loadTmp + " = load %stackelement* "+arrIndexPtr1);
					
					//Store value
					String arrIndexPtr0 = getTemporary();
					_(arrIndexPtr0 + " = getelementptr ["+arrSize0+" x %stackelement]* "+varName0+", i32 0, i32 "+i);
					
					_("store %stackelement "+loadTmp+", %stackelement* "+arrIndexPtr0);
				}


				return 0;


			}
		}

		String literalName = new String(ctx.expr(0).getText());
		_("; " + literalName);

		//Defaults...
		NblValue type = new NblValue<Integer>(NblValue.NblValueType.INTEGER, 0);
		String bitcastType = "i32";
		Boolean visit = true;
		if(ctx.expr(1) instanceof NobleParser.BoolContext){
			_("; Assignment - Bool");
			type = new NblValue<Boolean>(NblValue.NblValueType.BOOL, false);
		}
		if(ctx.expr(1) instanceof NobleParser.FloatContext){
			_("; Assignment - Float");
			type = new NblValue<Double>(NblValue.NblValueType.FLOAT, 0.0);
			bitcastType = "double";
		}
		if(ctx.expr(1) instanceof NobleParser.StringContext){
			_("; Assignment - String");
			String strVal = ctx.expr(1).getText();
			if(!strVal.toLowerCase().contains("\\00")){
				strVal = new StringBuffer(strVal).insert(strVal.length()-1, "\\00").toString();
			}
			type = new NblValue<String>(NblValue.NblValueType.STRING, strVal);
			bitcastType = typeForString(strVal);
		}
		if(ctx.expr(1) instanceof NobleParser.ColourContext){
			_("; Assignment - Colour");
			String strVal = ctx.expr(1).getText();
			strVal = "\"" + strVal + "\\00\"";
			type = new NblValue<String>(NblValue.NblValueType.COLOUR, strVal);
			bitcastType = typeForString(strVal);
		}
		if(ctx.expr(1) instanceof NobleParser.CharContext){
			_("; Assignment - Char");
			String strVal = ctx.expr(1).getText().replace("'","\"");
			if(!strVal.toLowerCase().contains("\\00")){
				strVal = new StringBuffer(strVal).insert(strVal.length()-1, "\\00").toString();
			}
			type = new NblValue<String>(NblValue.NblValueType.CHAR, strVal);
			bitcastType = typeForString(strVal);
		}

		if(visit){
			//Visit value
			visit(ctx.expr(1));
		}


		_("; Assignment");
		_("call void @pop()");		
		
		String varName = getVarName(literalName);

		if(vars.get(varName)==null){
			_(varName + " = alloca %stackelement");
		}else{
			NblValue existingType = vars.get(varName);
			if(existingType.getType()==NblValue.NblValueType.ARRAY){
				throw new RuntimeException("Cannot reassign an array variable");
			}
		}
		
		vars.put(varName, type);
		
		String loadTmp = getTemporary();
		_(loadTmp + " = load %stackelement* @svalue");
		_("store %stackelement "+loadTmp+", %stackelement* "+varName);
		return 0;
	}
	public Integer visitArrayInit(NobleParser.ArrayInitContext ctx) {
		String literalName = new String(ctx.ID().getText());
		String varName = getVarName(literalName);
		NblValue type = new NblValue<Integer>(NblValue.NblValueType.ARRAY, 0);
		vars.put(varName, type);
		Integer arrSize = new Integer(ctx.INT().getText());
		if(arraySizes.get(varName)!=null){
			if(!arraySizes.get(varName).equals(arrSize)){
				throw new RuntimeException("Arrays can only be fixed length. "+arrSize+"|"+arraySizes.get(varName));
			}

			/* Causes problems when returning an array from a function. This gets called when it really shouldnt.
			for(int i=0; i<arrSize;i++){
				_("call void @push(i8 7, i8* null)");
				String arrIndexPtr = getTemporary();
				_(arrIndexPtr + " = getelementptr ["+arrSize+" x %stackelement]* "+varName+", i32 0, i32 "+i);
				_("call void @pop()");
				String tmp = getTemporary();
				_(tmp +"= load %stackelement* @svalue");
				_("store %stackelement "+tmp+", %stackelement* "+arrIndexPtr);
			}
			*/
		}else{
			arraySizes.put(varName, arrSize);
			_(varName + " = alloca ["+arrSize+" x %stackelement]");
		}
		return 0;
	}
	public Integer visitArrayInitAssign(NobleParser.ArrayInitAssignContext ctx) {
		String literalName = new String(ctx.ID().getText());
		String varName = getVarName(literalName);
		NblValue type = new NblValue<Integer>(NblValue.NblValueType.ARRAY, 0);
		vars.put(varName, type);
		Integer arrSize = new Integer(ctx.INT().getText());

		if(arrSize!=ctx.expr().size()){
			throw new RuntimeException("Invalid number of array objets provided");
		}


		if(arraySizes.get(varName)!=null){
			if(!arraySizes.get(varName).equals(arrSize)){
				throw new RuntimeException("Arrays can only be fixed length.");
			}
		}else{
			_(varName + " = alloca ["+arrSize+" x %stackelement]");
			arraySizes.put(varName, arrSize);
		}

		for(int i=0; i<ctx.expr().size();i++){
			visit(ctx.expr(i));

			String arrIndexPtr = getTemporary();
			_(arrIndexPtr + " = getelementptr ["+arrSize+" x %stackelement]* "+varName+", i32 0, i32 "+i);
			_("call void @pop()");
			String tmp = getTemporary();
			_(tmp +"= load %stackelement* @svalue");
			_("store %stackelement "+tmp+", %stackelement* "+arrIndexPtr);
		}

		return 0;
	}
	public Integer visitArrayCall(NobleParser.ArrayCallContext ctx) {
		String literalName = new String(ctx.arr().ID(0).getText());
		String varName = getVarName(literalName);
		Integer arrSize = arraySizes.get(varName);
		

		String arrIndexPtr = "";


		if(ctx.arr().INT()!=null){
			Integer index = new Integer(ctx.arr().INT().getText());

			if(index>=arrSize){
				throw new RuntimeException("Array index is out of bounds");
			}

			arrIndexPtr = getTemporary();
			_(arrIndexPtr + " = getelementptr ["+arrSize+" x %stackelement]* "+varName+", i32 0, i32 "+index);
			
		}else{
			String indexLiteralName = new String(ctx.arr().ID(1).getText());
			String indexVarName = getVarName(indexLiteralName);

			String loadTmp = getTemporary();
			_(loadTmp + " = load %stackelement* "+indexVarName);
			// Type
			String type = getTemporary();
			_(type + " = extractvalue  %stackelement "+loadTmp+", 0");
			// Value
			String val = getTemporary();
			_(val + " = extractvalue  %stackelement "+loadTmp+", 1");
			String index = getTemporary();
			_(index + " = ptrtoint i8* "+val+" to i32");

			arrIndexPtr = getTemporary();
			_(arrIndexPtr + " = getelementptr ["+arrSize+" x %stackelement]* "+varName+", i32 0, i32 "+index);
		}


		String loadTmp = getTemporary();
		_(loadTmp + " = load %stackelement* "+arrIndexPtr);
		// Type
		String type = getTemporary();
		_(type + " = extractvalue  %stackelement "+loadTmp+", 0");
		// Value
		String val = getTemporary();
		_(val + " = extractvalue  %stackelement "+loadTmp+", 1");

		String iftmp1 = getTemporary();
		_(iftmp1+" = icmp sge i8 10, "+type);
		String isValidIf = getLabel();
		String notValidIf = getLabel();
		String resumeLabel = getLabel();
		_("br i1 "+iftmp1+", label %"+isValidIf+", label %"+notValidIf);

		os.println(notValidIf+":");
		_("call void @push(i8 7, i8* null)");
		_("br label %"+resumeLabel);

		os.println(isValidIf+":");
		_("call void @push(i8 "+type+", i8* "+val+")");
		_("br label %"+resumeLabel);

		os.println(resumeLabel+":");
		return 0;
	}
	public Integer visitArrayAssign(NobleParser.ArrayAssignContext ctx) {
		String literalName = new String(ctx.arr().ID(0).getText());
		String varName = getVarName(literalName);
		Integer arrSize = arraySizes.get(varName);

		String arrIndexPtr = "";

		if(ctx.arr().INT()!=null){
			Integer index = new Integer(ctx.arr().INT().getText());

			if(index>=arrSize){
				throw new RuntimeException("Array index is out of bounds");
			}

			arrIndexPtr = getTemporary();
			_(arrIndexPtr + " = getelementptr ["+arrSize+" x %stackelement]* "+varName+", i32 0, i32 "+index);
			
		}else{
			String indexLiteralName = new String(ctx.arr().ID(1).getText());
			String indexVarName = getVarName(indexLiteralName);

			String loadTmp = getTemporary();
			_(loadTmp + " = load %stackelement* "+indexVarName);
			// Type
			String type = getTemporary();
			_(type + " = extractvalue  %stackelement "+loadTmp+", 0");
			// Value
			String val = getTemporary();
			_(val + " = extractvalue  %stackelement "+loadTmp+", 1");
			String index = getTemporary();
			_(index + " = ptrtoint i8* "+val+" to i32");

			arrIndexPtr = getTemporary();
			_(arrIndexPtr + " = getelementptr ["+arrSize+" x %stackelement]* "+varName+", i32 0, i32 "+index);
		}

		visit(ctx.expr());

		_("call void @pop()");
		String tmp = getTemporary();
		_(tmp +"= load %stackelement* @svalue");
		_("store %stackelement "+tmp+", %stackelement* "+arrIndexPtr);


		return 0;
	}
	public Integer visitForeach(NobleParser.ForeachContext ctx) {
		String literalName = new String(ctx.ID(1).getText());
		String varName = getVarName(literalName);
		Integer arrSize = arraySizes.get(varName);

		String varLiteralName = new String(ctx.ID(0).getText());
		String varVarName = getVarName(varLiteralName);
		_(varVarName + " = alloca %stackelement");

		String index = getTemporary();
		_(index + " = alloca i32");
		_("store i32 0, i32* "+index);

		String startLabel = getLabel();
		String exitLabel = getLabel();
		currentBreakLabel.push(exitLabel);

		_("br label %"+startLabel);

		os.println(startLabel+":");

		//Check if array complete
		String currentIndex = getTemporary();
		_(currentIndex +" = load i32* "+index);
		String iftmp1 = getTemporary();
		_(iftmp1+" = icmp eq i32 "+arrSize+", "+currentIndex);
		String continueLabel = getLabel();
		_("br i1 "+iftmp1+", label %"+exitLabel+", label %"+continueLabel);

		os.println(continueLabel+":");

		//load array val into variable
		String arrIndexPtr = getTemporary();
		_(arrIndexPtr + " = getelementptr ["+arrSize+" x %stackelement]* "+varName+", i32 0, i32 "+currentIndex);

		String loadTmp = getTemporary();
		_(loadTmp + " = load %stackelement* "+arrIndexPtr);
		_("store %stackelement "+loadTmp+", %stackelement* "+varVarName);

		visit(ctx.func_block());

		String newIndex = getTemporary();
		_(newIndex +" = add i32 1, "+currentIndex);
		_("store i32 "+newIndex+", i32* "+index);


		_("br label %"+startLabel);


		//End of loop
		os.println(exitLabel+":");
		currentBreakLabel.pop();

		return 0;
	}



	/* UNIMPLEMENTED */
	public Integer visitObjectDef(NobleParser.ObjectDefContext ctx) {
		System.out.println("/!\\ Objects not currently supported! Values follow...");

		if(ctx.object_def().object_props()!=null){
			for(int i=0; i<ctx.object_def().object_props().object_prop().size(); i++){
				Object id = null;
				Object value = null;

				if(ctx.object_def().object_props().object_prop(i).object_id().STRING()!=null){
					id = ctx.object_def().object_props().object_prop(i).object_id().STRING();
				}else{
					id = ctx.object_def().object_props().object_prop(i).object_id().INT();
				}
				switch(ctx.object_def().object_props().object_prop(i).val.getType()){
					case NobleParser.STRING:
						value = ctx.object_def().object_props().object_prop(i).STRING();

						break;
					case NobleParser.INT:
						value = ctx.object_def().object_props().object_prop(i).INT();
						
						break;
				}

				System.out.println("ID: "+id+ " | Val: "+value);
			}
		}
		return 0;
	}
	public Integer visitObjectCall(NobleParser.ObjectCallContext ctx) {
		System.out.println("/!\\ Object calls not currently supported!");
		return 0;
	}
	public Integer visitTryCatch(NobleParser.TryCatchContext ctx) {
		System.out.println("/!\\ Try/Catch not currently supported!");
		return 0;
	}
	/* END UNIMPLEMENTED */






	public void print(Boolean isLog){

		_("; Print - With log? "+ isLog);

		String prepend = "printfstr";
		if(isLog){
			prepend = "printflogstr";
		}
		String prependType = "%"+prepend+"_type";

		_("call void @pop()");
		String vloc = getTemporary();
		_(vloc+" = getelementptr %stackelement* @svalue, i32 0, i32 0");
		String valtype = getTemporary();
		_(valtype+" = load i8* "+vloc);

		String vloc2 = getTemporary();
		_(vloc2+" = getelementptr %stackelement* @svalue, i32 0, i32 1");
		String valp = getTemporary();
		_(valp+" = load i8** "+vloc2);
		String val = getTemporary();
		_(val+" = ptrtoint i8* "+valp+" to i32");

		String printInt = getLabel();
		String printString = getLabel();
		String printBool = getLabel();
		String printFloat = getLabel();
		String printNull = getLabel();
		String breakString = getLabel();

		_("switch i8 "+valtype+", label %"+breakString+" [ i8 0, label %"+printInt+"\n\t\t\ti8 1, label %"+printString+"\n\t\t\ti8 3, label %"+printBool+"\n\t\t\ti8 4, label %"+printString+"\n\t\t\ti8 5, label %"+printString+"\n\t\t\ti8 6, label %"+printFloat+"\n\t\t\ti8 7, label %"+printNull+" ]");	


		os.println(printInt+":");
		String tmp2 = getTemporary();
		if(isLog){
			_(tmp2 + " = call i32 (i8*, ...)* @printf(i8* getelementptr ("+prependType+"* @"+prepend+", i32 0, i32 0), i8 "+valtype+", i32 "+val+")");
		}else{
			_(tmp2 + " = call i32 (i8*, ...)* @printf(i8* getelementptr ("+prependType+"* @"+prepend+", i32 0, i32 0), i32 "+val+")");
		}
		_("br label %"+breakString);

		os.println(printFloat+":");


		String floatVal = getTemporary();
		_(floatVal + " = bitcast i8* "+valp + " to double*");
		String floatVal2 = getTemporary();
		_(floatVal2+" = load double* "+floatVal);
		String tmp20 = getTemporary();
		if(isLog){
			_(tmp20 + " = call i32 (i8*, ...)* @printf(i8* getelementptr ("+prependType+"* @"+prepend+"_f, i32 0, i32 0), i8 "+valtype+", double "+floatVal2+")");
		}else{
			_(tmp20 + " = call i32 (i8*, ...)* @printf(i8* getelementptr ("+prependType+"* @"+prepend+"_f, i32 0, i32 0), double "+floatVal2+")");
		}
		_("br label %"+breakString);

		os.println(printString+":");
		String tmp3 = getTemporary();
		if(isLog){
			_(tmp3 + " = call i32 (i8*, ...)* @printf(i8* getelementptr ("+prependType+"* @"+prepend+"_s, i32 0, i32 0), i8 "+valtype+", i8* "+valp+")");
		}else{
			_(tmp3 + " = call i32 (i8*, ...)* @printf(i8* getelementptr ("+prependType+"* @"+prepend+"_s, i32 0, i32 0), i8* "+valp+")");
		}
		_("br label %"+breakString);

		os.println(printNull+":");
		String nulltmp = getTemporary();
		if(isLog){
			_(nulltmp + " = call i32 (i8*, ...)* @printf(i8* getelementptr ([23 x i8]* @lognull, i32 0, i32 0), i8 "+valtype+")");
		}else{
			_(nulltmp + " = call i32 (i8*, ...)* @printf(i8* getelementptr ([11 x i8]* @printnull, i32 0, i32 0))");
		}
		_("br label %"+breakString);

		os.println(printBool+":");
		String result = getTemporary();
		_(result+" = icmp eq i32 1, "+val);	
		String boolTrue = getLabel();
		String boolFalse = getLabel();
		_("br i1 "+result+", label %"+boolTrue+", label %"+boolFalse);

		os.println(boolTrue+":");
		String tmp4 = getTemporary();
		if(isLog){
			_(tmp4 + " = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ("+prependType+"* @"+prepend+"_s, i32 0, i32 0), i8 "+valtype+", i8* getelementptr inbounds ([5 x i8]* @.true, i32 0, i32 0))");
		}else{
			_(tmp4 + " = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ("+prependType+"* @"+prepend+"_s, i32 0, i32 0), i8* getelementptr inbounds ([5 x i8]* @.true, i32 0, i32 0))");
		}
		_("br label %"+breakString);

		os.println(boolFalse+":");
		String tmp5 = getTemporary();
		if(isLog){
			_(tmp5 + " = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ("+prependType+"* @"+prepend+"_s, i32 0, i32 0), i8 "+valtype+", i8* getelementptr inbounds ([6 x i8]* @.false, i32 0, i32 0))");
		}else{
			_(tmp5 + " = call i32 (i8*, ...)* @printf(i8* getelementptr inbounds ("+prependType+"* @"+prepend+"_s, i32 0, i32 0), i8* getelementptr inbounds ([6 x i8]* @.false, i32 0, i32 0))");
		}
		_("br label %"+breakString);

		os.println(breakString+":");
	}


	public void outputConstants() {
		for (Map.Entry<String, NblValue> entry : constants.entrySet()) {
			os.print(entry.getKey() + " = internal constant ");
			switch(entry.getValue().getType()) {
				case INTEGER:
				case NULL:
					os.println("i32 " + entry.getValue());
					break;
				case FLOAT:
					os.println("double " + entry.getValue());
					break;
				case BOOL:
					Boolean val = (Boolean)entry.getValue().getValue();
					os.println("i32 " + (val?1:0));
					break;
				case STRING:
				case CHAR:
				case COLOUR:
					os.println(typeForString(entry.getValue().toString()) + " c" + entry.getValue());
					break;
			}
		}
	}

	public void outputVars() {
		for (Map.Entry<String, NblValue> entry : vars.entrySet()) {
				if(entry.getValue().getType()!=NblValue.NblValueType.ARRAY){
	                os.print(entry.getKey().replace("%","@") + " = internal global ");
	                switch(entry.getValue().getType()) {
	                        case INTEGER:
	                        case NULL:
	                                os.println("i32 " + entry.getValue());
	                                break;
	                        case FLOAT:
	                                os.println("double " + entry.getValue());
	                                break;
	                        case BOOL:
	                                Boolean val = (Boolean)entry.getValue().getValue();
	                                os.println("i32 " + (val?1:0));
	                                break;
	                        case STRING:
	                        case CHAR:
	                        case COLOUR:
	                                os.println(typeForString(entry.getValue().toString()) + " c" + entry.getValue());
	                                break;
	                }
	            }
        }
	}

	String getVarType(String literalName){
		NblValue varType = vars.get(getVarName(literalName));
		if(varType==null){
			return "0";
		}
		return Integer.toString(varType.getType().ordinal());
	}

	String makeVar(String name, NblValue type){
		String value = type.toString();
		switch(type.getType()) {
			case STRING:
			case CHAR:
			case COLOUR:
				varTypes.put(getVarName(name), typeForString(type.toString()));
				break;
			case INTEGER:
			case BOOL:
				case NULL:
				varTypes.put(getVarName(name), "i32");
				break;
			case FLOAT:
				varTypes.put(getVarName(name), "double");
				break;
		}
		vars.put(getVarName(name), type);

		return getVarName(name);
	}


	String typeForString(String val) {
		val = val.replace("\\00", " ");
		return "[" + (val.length()-2) + " x i8]";
	}


	String getVarName(String literalName) {
		literalName = literalName.substring(1);
		if(varIterations.get("!"+literalName)!=null){
			literalName = literalName + varIterations.get("!"+literalName).toString();
		}
		return "%" + currentFunction + "_" + VAR_PREFIX + literalName;
	}
	String getArrName(String literalName) {
		literalName = literalName.substring(1).replace("[", "$").replace("]", "$");
		if(varIterations.get("!"+literalName)!=null){
			literalName = literalName + varIterations.get("!"+literalName).toString();
		}
		return "%" + currentFunction + "_" + ARR_PREFIX + literalName;
	}
	String getLabel() {
		return LABEL_PREFIX + labelCounter++;
	}
	String getConstant() {
		return "@" + CONSTANT_PREFIX + constantCounter++;
	}
	String getFunction() {
		return "@fun" + funcCounter++;
	}
	String getTemporary() {
		return "%" + tmpCounter++;
	}


	void _(String value){
		os.println("\t"+value);
	}

	
}
