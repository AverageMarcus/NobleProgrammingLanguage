import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.*;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.HashMap;

public class NC {
	static final String PROLOGUE_NAME = "noble_llvm_prologue.ll";
	static final String EPILOGUE_NAME = "noble_llvm_epilogue.ll";

    public static void main(String[] args) throws Exception {
        String inputFile = null;
        String outputFile = null;
        if(args.length == 2) {
			outputFile = args[0];
			inputFile = args[1];
		}

		if (inputFile == null || outputFile == null){
			throw new Exception();
		}

		InputStream is = new FileInputStream(inputFile);
		FileOutputStream os = new FileOutputStream(outputFile);

		ANTLRInputStream input = new ANTLRInputStream(is);
		NobleLexer lexer = new NobleLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		NobleParser parser = new NobleParser(tokens);

		ParseTree tree = parser.r();
		//System.out.println(tree.toStringTree(parser));

		// Generate LLVM prologue
		FileInputStream ps = new FileInputStream(PROLOGUE_NAME);
		os.getChannel().transferFrom(ps.getChannel(), 0, ps.getChannel().size());
		ps.close();
		os.getChannel().position(os.getChannel().size());

		// Visitor generates the IR
		NblVisitor l = new NblVisitor(os);
		l.visit(tree);

		// Generate LLVM epilogue
		ps = new FileInputStream(EPILOGUE_NAME);
		os.getChannel().transferFrom(ps.getChannel(), os.getChannel().size(), ps.getChannel().size());
		ps.close();
		os.getChannel().position(os.getChannel().size());
		l.outputConstants();
		l.outputVars();
	}
}
