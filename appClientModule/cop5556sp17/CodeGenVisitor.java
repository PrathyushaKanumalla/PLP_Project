package cop5556sp17;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.Token;
import cop5556sp17.AST.ASTVisitor;
import cop5556sp17.AST.AssignmentStatement;
import cop5556sp17.AST.BinaryChain;
import cop5556sp17.AST.BinaryExpression;
import cop5556sp17.AST.Block;
import cop5556sp17.AST.BooleanLitExpression;
import cop5556sp17.AST.Chain;
import cop5556sp17.AST.ChainElem;
import cop5556sp17.AST.ConstantExpression;
import cop5556sp17.AST.Dec;
import cop5556sp17.AST.Expression;
import cop5556sp17.AST.FilterOpChain;
import cop5556sp17.AST.FrameOpChain;
import cop5556sp17.AST.IdentChain;
import cop5556sp17.AST.IdentExpression;
import cop5556sp17.AST.IdentLValue;
import cop5556sp17.AST.IfStatement;
import cop5556sp17.AST.ImageOpChain;
import cop5556sp17.AST.IntLitExpression;
import cop5556sp17.AST.ParamDec;
import cop5556sp17.AST.Program;
import cop5556sp17.AST.SleepStatement;
import cop5556sp17.AST.Statement;
import cop5556sp17.AST.Tuple;
import cop5556sp17.AST.Type;
import cop5556sp17.AST.Type.TypeName;
import cop5556sp17.AST.WhileStatement;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	/**
	 * @param DEVEL
	 *            used as parameter to genPrint and genPrintTOS
	 * @param GRADE
	 *            used as parameter to genPrint and genPrintTOS
	 * @param sourceFileName
	 *            name of source file, may be null.
	 */
	public CodeGenVisitor(boolean DEVEL, boolean GRADE, String sourceFileName) {
		super();
		this.DEVEL = DEVEL;
		this.GRADE = GRADE;
		this.sourceFileName = sourceFileName;
	}

	ClassWriter cw;
	String className;
	String classDesc;
	String sourceFileName;

	MethodVisitor mv; // visitor of method currently under construction

	/** Indicates whether genPrint and genPrintTOS should generate code. */
	final boolean DEVEL;
	final boolean GRADE;

	FieldVisitor fv;
	int slotCount = 0;
	int arg_index = 0;

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		// TODO Implement this
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		className = program.getName();
		classDesc = "L" + className + ";";
		String sourceFileName = (String) arg;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object",
				new String[] { "java/lang/Runnable" });
		cw.visitSource(sourceFileName, null);

		// generate constructor code
		// get a MethodVisitor
		mv = cw.visitMethod(ACC_PUBLIC, "<init>", "([Ljava/lang/String;)V", null, null);
		mv.visitCode();
		// Create label at start of code
		Label constructorStart = new Label();
		mv.visitLabel(constructorStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering <init>");
		// generate code to call superclass constructor
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		// visit parameter decs to add each as field to the class
		// pass in mv so decs can add their initialization code to the
		// constructor.
		ArrayList<ParamDec> params = program.getParams();
		arg_index = 0;
		for (ParamDec dec : params)
			dec.visit(this, mv);
		arg_index = 0;
		mv.visitInsn(RETURN);
		
		// create label at end of code
		Label constructorEnd = new Label();
		mv.visitLabel(constructorEnd);
		// finish up by visiting local vars of constructor
		// the fourth and fifth arguments are the region of code where the local
		// variable is defined as represented by the labels we inserted.
		mv.visitLocalVariable("this", classDesc, null, constructorStart, constructorEnd, 0);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, constructorStart, constructorEnd, 1);
		// indicates the max stack size for the method.
		// because we used the COMPUTE_FRAMES parameter in the classwriter
		// constructor, asm
		// will do this for us. The parameters to visitMaxs don't matter, but
		// the method must
		// be called.
		mv.visitMaxs(1, 1);
		// finish up code generation for this method.
		mv.visitEnd();
		// end of constructor

		// create main method which does the following
		// 1. instantiate an instance of the class being generated, passing the
		// String[] with command line arguments
		// 2. invoke the run method.
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		mv.visitCode();
		Label mainStart = new Label();
		mv.visitLabel(mainStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering main");
		mv.visitTypeInsn(NEW, className);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "([Ljava/lang/String;)V", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, className, "run", "()V", false);
		mv.visitInsn(RETURN);
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);
		mv.visitLocalVariable("instance", classDesc, null, mainStart, mainEnd, 1);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		// create run method
		mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
		mv.visitCode();
		Label startRun = new Label();
		mv.visitLabel(startRun);
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering run");
		program.getB().visit(this, null);
		mv.visitInsn(RETURN);
		Label endRun = new Label();
		mv.visitLabel(endRun);
		mv.visitLocalVariable("this", classDesc, null, startRun, endRun, 0);
		// TODO visit the local variables
		for (Dec dec : program.getB().getDecs()) {
			if (dec.getTypeName() == TypeName.INTEGER) {
				mv.visitLocalVariable(dec.getIdent().getText(), "I", null, startRun, endRun, dec.getSlotNum());
			} else if (dec.getTypeName() == TypeName.BOOLEAN) {
				mv.visitLocalVariable(dec.getIdent().getText(), "Z", null, startRun, endRun, dec.getSlotNum());
			} else if (dec.getTypeName() == TypeName.FRAME) {
				mv.visitLocalVariable(dec.getIdent().getText(), PLPRuntimeFrame.JVMDesc, null, startRun, endRun,
						dec.getSlotNum());
			} else if (dec.getTypeName() == TypeName.IMAGE) {
				mv.visitLocalVariable(dec.getIdent().getText(), "Ljava/awt/image/BufferedImage;", null, startRun,
						endRun, dec.getSlotNum());
			}
		}

		mv.visitMaxs(1, 1);
		mv.visitEnd(); // end of run method

		cw.visitEnd();// end of class

		// generate classfile and return it
		return cw.toByteArray();
	}

	@Override
	public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception {
		assignStatement.getE().visit(this, arg);
		CodeGenUtils.genPrint(DEVEL, mv, "\nassignment: " + assignStatement.var.getText() + "=");
		CodeGenUtils.genPrintTOS(GRADE, mv, assignStatement.getE().getType());
		assignStatement.getVar().visit(this, arg);
		return null;
	}

	@Override
	public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception {
		
		Chain chain = binaryChain.getE0();
        ChainElem chainElem = binaryChain.getE1();
        chain.visit(this, "LEFT");
        if(chain.getTypeName().isType(TypeName.URL)) {
            mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "readFromURL", PLPRuntimeImageIO.readFromURLSig, false);
        }
        else if(chain.getTypeName().isType(TypeName.FILE)) {
            mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "readFromFile", PLPRuntimeImageIO.readFromFileDesc, false);
        }
        else if(chain.getTypeName().isType(TypeName.NONE)) {
            mv.visitInsn(POP);
        }
        if(chainElem instanceof FilterOpChain) {
            if(binaryChain.getArrow().isKind(Kind.ARROW)) {
                mv.visitInsn(ACONST_NULL);
            }
            else {
                mv.visitInsn(DUP);
            }
        }
        chainElem.visit(this, "RIGHT");
		return null;
	}

	@Override
	public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception {
		// TODO Implement this
		Label conditRet = new Label();
		binaryExpression.getE0().visit(this, arg);
		binaryExpression.getE1().visit(this, arg);
		switch (binaryExpression.getOp().kind) {
		case TIMES: {
			if(binaryExpression.getE0().getTypeName().isType(TypeName.IMAGE) && binaryExpression.getE1().getTypeName().isType(TypeName.INTEGER)) {
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "mul", PLPRuntimeImageOps.mulSig,false);
			}
			else if(binaryExpression.getE0().getTypeName().isType(TypeName.INTEGER) && binaryExpression.getE1().getTypeName().isType(TypeName.IMAGE)) {
				mv.visitInsn(SWAP);
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "mul", PLPRuntimeImageOps.mulSig,false);
			}
			else {
				mv.visitInsn(IMUL);
			}
			break;
		}
		case DIV: {
			if(binaryExpression.getE0().getTypeName().isType(TypeName.IMAGE) && binaryExpression.getE1().getTypeName().isType(TypeName.INTEGER)) {
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "div", PLPRuntimeImageOps.divSig,false);
			}
			else 
				mv.visitInsn(IDIV);
			break;
		}
		case AND: 
			mv.visitInsn(IAND);
			break;
		case MOD: {
			if(binaryExpression.getE0().getTypeName().isType(TypeName.IMAGE) && binaryExpression.getE1().getTypeName().isType(TypeName.INTEGER)) {
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "mod", PLPRuntimeImageOps.modSig,false);
			}
			else
				mv.visitInsn(IREM);
			break;
		}
		case PLUS:{
			if(binaryExpression.getE0().getTypeName().isType(TypeName.IMAGE) && binaryExpression.getE1().getTypeName().isType(TypeName.IMAGE)) {
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "add", PLPRuntimeImageOps.addSig,false);
			}
			else {
				mv.visitInsn(IADD);
			}
			break;
		}
		case MINUS: {
			if(binaryExpression.getE0().getTypeName().isType(TypeName.IMAGE) && binaryExpression.getE1().getTypeName().isType(TypeName.IMAGE)) {
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "sub", PLPRuntimeImageOps.subSig,false);
			}
			else {
				mv.visitInsn(ISUB);
			}
			break;
		}
		case OR:
			mv.visitInsn(IOR);
			break;
		case LT: {
			Label end = new Label();
			mv.visitJumpInsn(IF_ICMPGE, end);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(GOTO, conditRet);
			mv.visitLabel(end);
			mv.visitInsn(ICONST_0);
			mv.visitJumpInsn(GOTO, conditRet);
			break;
		}
		case LE: {
			Label end = new Label();
			mv.visitJumpInsn(IF_ICMPGT, end);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(GOTO, conditRet);
			mv.visitLabel(end);
			mv.visitInsn(ICONST_0);
			mv.visitJumpInsn(GOTO, conditRet);
			break;
		}
		case GT: {
			Label end = new Label();
			mv.visitJumpInsn(IF_ICMPLE, end);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(GOTO, conditRet);
			mv.visitLabel(end);
			mv.visitInsn(ICONST_0);
			mv.visitJumpInsn(GOTO, conditRet);
			break;
		}
		case GE: {
			Label end = new Label();
			mv.visitJumpInsn(IF_ICMPLT, end);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(GOTO, conditRet);
			mv.visitLabel(end);
			mv.visitInsn(ICONST_0);
			mv.visitJumpInsn(GOTO, conditRet);
			break;
		}
		case EQUAL: {
			Label end = new Label();
			mv.visitJumpInsn(IF_ICMPNE, end);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(GOTO, conditRet);
			mv.visitLabel(end);
			mv.visitInsn(ICONST_0);
			mv.visitJumpInsn(GOTO, conditRet);
			break;
		}
		case NOTEQUAL: {
			Label end = new Label();
			mv.visitJumpInsn(IF_ICMPEQ, end);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(GOTO, conditRet);
			mv.visitLabel(end);
			mv.visitInsn(ICONST_0);
			mv.visitJumpInsn(GOTO, conditRet);
			break;
		}
		}
		mv.visitLabel(conditRet);
		return null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		// TODO Implement this
		List<Dec> decList = block.getDecs();
		for (Dec dec : decList) {
			dec.visit(this, arg);
		}
		List<Statement> statementList = block.getStatements();
		for (Statement statement : statementList) {
			statement.visit(this, arg);
            if(statement instanceof BinaryChain)
                mv.visitInsn(POP);
		}
		return null;
	}

	@Override
	public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception {
		// TODO Implement this
		if (booleanLitExpression.getFirstToken().getText().equals("true"))
			mv.visitInsn(ICONST_1);
		else
			mv.visitInsn(ICONST_0);
		return null;
	}

	@Override
	public Object visitConstantExpression(ConstantExpression constantExpression, Object arg) {
		if(constantExpression.getFirstToken().isKind(Kind.KW_SCREENWIDTH)) {
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "getScreenWidth", PLPRuntimeFrame.getScreenWidthSig, false);
		}
		else if(constantExpression.getFirstToken().isKind(Kind.KW_SCREENHEIGHT)) {
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "getScreenHeight", PLPRuntimeFrame.getScreenHeightSig, false);
		}
		return null;
	}

	@Override
	public Object visitDec(Dec declaration, Object arg) throws Exception {
		// TODO Implement this
		declaration.setSlotNum(++slotCount);
		if(declaration.getTypeName().isType(TypeName.IMAGE) || declaration.getTypeName().isType(TypeName.FRAME) ) {
			mv.visitInsn(ACONST_NULL);
			mv.visitVarInsn(ASTORE, declaration.getSlotNum());
		}
		else{
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ISTORE, declaration.getSlotNum());
		}
		return null;
	}

	@Override
	public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception {
		Token filterToken = filterOpChain.getFirstToken();
        filterOpChain.getArg().visit(this, null);
        String op = (String) arg;
        if(filterToken.isKind(Kind.OP_BLUR)) {
            mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "blurOp", PLPRuntimeFilterOps.opSig, false);
        }
        else if(filterToken.isKind(Kind.OP_GRAY)) {
            mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "grayOp", PLPRuntimeFilterOps.opSig, false);
        }
        else if(filterToken.isKind(Kind.OP_CONVOLVE)){
        	 mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "convolveOp", PLPRuntimeFilterOps.opSig, false);
        }
		return null;
	}

	@Override
	public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception {
		Token frameToken = frameOpChain.getFirstToken();
        frameOpChain.getArg().visit(this, null);
        if(frameToken.isKind(Kind.KW_SHOW)) {
            mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "showImage", PLPRuntimeFrame.showImageDesc, false);
        }
        else if(frameToken.isKind(Kind.KW_HIDE)){
            mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "hideImage", PLPRuntimeFrame.hideImageDesc, false);
        }
        else if(frameToken.isKind(Kind.KW_MOVE)) {
            mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "moveFrame", PLPRuntimeFrame.moveFrameDesc, false);
        }
        else if(frameToken.isKind(Kind.KW_XLOC)) {
            mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "getXVal", PLPRuntimeFrame.getXValDesc, false);
        }
        else if(frameToken.isKind(Kind.KW_YLOC)) {
            mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "getYVal", PLPRuntimeFrame.getYValDesc, false);
        }
		
		return null;
	}

	@Override
	public Object visitIdentChain(IdentChain identChain, Object args) throws Exception {
		String arg = (String) args;
		Dec dec = identChain.getDec();
		if(arg.equals("LEFT")) {
			if (dec instanceof ParamDec) {
				mv.visitVarInsn(ALOAD, 0);
				if (dec.getTypeName().equals(TypeName.INTEGER))
					mv.visitFieldInsn(GETFIELD, className, dec.getIdent().getText(), "I");
				else if (dec.getTypeName().equals(TypeName.BOOLEAN))
					mv.visitFieldInsn(GETFIELD, className, dec.getIdent().getText(), "Z");
				else if (dec.getTypeName().equals(TypeName.FILE)) {
					mv.visitFieldInsn(GETFIELD, className, dec.getIdent().getText(), "Ljava/io/File;");
				}
				else if (dec.getTypeName().equals(TypeName.URL)) {
					mv.visitFieldInsn(GETFIELD, className, dec.getIdent().getText(), "Ljava/net/URL;");
				}
			} else {
				if (dec.getTypeName().equals(TypeName.FRAME) || dec.getTypeName().equals(TypeName.IMAGE)) {
					mv.visitVarInsn(ALOAD, dec.getSlotNum());
				}
				else{
					mv.visitVarInsn(ILOAD, dec.getSlotNum());
				}
			}
		}
		else {
			if (dec instanceof ParamDec) {
				mv.visitVarInsn(ALOAD, 0);
				if (dec.getTypeName().equals(TypeName.INTEGER)) {
					mv.visitInsn(SWAP);
					mv.visitFieldInsn(PUTFIELD, className, dec.getIdent().getText(), "I");
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, className, dec.getIdent().getText(), "I");
					
					
				} /*else if (dec.getTypeName().equals(TypeName.BOOLEAN)) {
					mv.visitInsn(SWAP);
					mv.visitFieldInsn(PUTFIELD, className, dec.getIdent().getText(), "Z");
				}*/
				
				else if (dec.getTypeName().equals(TypeName.FILE)) {
					mv.visitFieldInsn(GETFIELD, className, dec.getIdent().getText(), "Ljava/io/File;");
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "write", PLPRuntimeImageIO.writeImageDesc,false);
					mv.visitInsn(POP);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, className, dec.getIdent().getText(), "Ljava/io/File;");
				}
			} 
			else {
				if (dec.getTypeName().equals(TypeName.FRAME)) {
					mv.visitVarInsn(ALOAD, dec.getSlotNum());
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "createOrSetFrame", PLPRuntimeFrame.createOrSetFrameSig,false);
					mv.visitVarInsn(ASTORE,dec.getSlotNum());
					mv.visitVarInsn(ALOAD, dec.getSlotNum());
				}
				else if(dec.getTypeName().equals(TypeName.IMAGE)) {
					mv.visitVarInsn(ASTORE, dec.getSlotNum());
					mv.visitVarInsn(ALOAD, dec.getSlotNum());
				}
				else{
					mv.visitVarInsn(ISTORE, dec.getSlotNum());
					mv.visitVarInsn(ILOAD, dec.getSlotNum());
				}
			}
		}
		return null;
	}

	@Override
	public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception {
		// TODO Implement this
		Dec dec = identExpression.getDec();
		// added
		if (dec instanceof ParamDec) {
			mv.visitVarInsn(ALOAD, 0);
			if (dec.getTypeName().equals(TypeName.INTEGER))
				mv.visitFieldInsn(GETFIELD, className, dec.getIdent().getText(), "I");
			else if (dec.getTypeName().equals(TypeName.BOOLEAN))
				mv.visitFieldInsn(GETFIELD, className, dec.getIdent().getText(), "Z");
			else if (dec.getTypeName().equals(TypeName.FILE)) {
				mv.visitFieldInsn(GETFIELD, className, dec.getIdent().getText(), "Ljava/io/File;");
			}
			else if (dec.getTypeName().equals(TypeName.URL)) {
				mv.visitFieldInsn(GETFIELD, className, dec.getIdent().getText(), "Ljava/net/URL;");
			}
		} else {
			if (dec.getTypeName().equals(TypeName.FRAME) || dec.getTypeName().equals(TypeName.IMAGE)) {
				mv.visitVarInsn(ALOAD, dec.getSlotNum());
			}
			else{
				mv.visitVarInsn(ILOAD, dec.getSlotNum());
			}
		}
		// added
		return null;
	}

	@Override
	public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception {
		// TODO Implement this
		Dec dec = identX.getDec();
		if (dec instanceof ParamDec) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(SWAP);
			if (dec.getTypeName().equals(TypeName.INTEGER)) {
				mv.visitFieldInsn(PUTFIELD, className, dec.getIdent().getText(), "I");
			} else if (dec.getTypeName().equals(TypeName.BOOLEAN)) {
				mv.visitFieldInsn(PUTFIELD, className, dec.getIdent().getText(), "Z");
			}
			else if (dec.getTypeName().equals(TypeName.FILE)) {
				mv.visitFieldInsn(PUTFIELD, className, dec.getIdent().getText(),TypeName.FILE.toString());
			}
			else if (dec.getTypeName().equals(TypeName.URL)) {
				mv.visitFieldInsn(PUTFIELD, className, dec.getIdent().getText(), TypeName.URL.toString());
			}
		} else {
			if (dec.getTypeName().equals(TypeName.IMAGE)) {
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "copyImage", PLPRuntimeImageOps.copyImageSig,false);
				mv.visitVarInsn(ASTORE, dec.getSlotNum());
			}
			else if (dec.getTypeName().equals(TypeName.FRAME)) {
				mv.visitVarInsn(ASTORE, dec.getSlotNum());
			}
			else{
				mv.visitVarInsn(ISTORE, dec.getSlotNum());
			}
		}
		// added
		return null;

	}

	@Override
	public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception {
		// TODO Implement this
		Label blockStart = new Label();
		Label blockEnd = new Label();
		mv.visitLabel(blockStart);
		ifStatement.getE().visit(this, null);
		mv.visitJumpInsn(IFEQ, blockEnd);
		ifStatement.getB().visit(this, arg);
		mv.visitLabel(blockEnd);
		// local variables
		for (Dec dec : ifStatement.getB().getDecs()) {
			if (dec.getTypeName() == TypeName.INTEGER) {
				mv.visitLocalVariable(dec.getIdent().getText(), "I", null, blockStart, blockEnd, dec.getSlotNum());
			} else if (dec.getTypeName() == TypeName.BOOLEAN) {
				mv.visitLocalVariable(dec.getIdent().getText(), "Z", null, blockStart, blockEnd, dec.getSlotNum());
			}
			else if (dec.getTypeName() == TypeName.FRAME) {
				mv.visitLocalVariable(dec.getIdent().getText(), PLPRuntimeFrame.JVMDesc, null, blockStart, blockEnd, dec.getSlotNum());
			}
			else if (dec.getTypeName() == TypeName.IMAGE) {
				mv.visitLocalVariable(dec.getIdent().getText(), "Ljava/awt/image/BufferedImage;", null, blockStart, blockEnd, dec.getSlotNum());
			}
		}
		return null;
	}

	@Override
	public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception {
		Token imageToken = imageOpChain.getFirstToken();
        imageOpChain.getArg().visit(this, null);
        if(imageToken.isKind(Kind.OP_WIDTH)) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/awt/image/BufferedImage", "getWidth", PLPRuntimeImageOps.getWidthSig, false);
        }
        else if(imageToken.isKind(Kind.OP_HEIGHT)) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/awt/image/BufferedImage", "getHeight", PLPRuntimeImageOps.getHeightSig, false);
        }
        else if(imageToken.isKind(Kind.KW_SCALE)) {
            mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "scale", PLPRuntimeImageOps.scaleSig, false);
        }
		return null;
	}

	@Override
	public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception {
		// TODO Implement this
		mv.visitIntInsn(SIPUSH, intLitExpression.getFirstToken().intVal());
		// added
		return null;
	}

	@Override
	public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception {
		// TODO Implement this
		String varName = paramDec.getIdent().getText();
		String typeIdentifier = null;
		if (paramDec.getTypeName() == TypeName.INTEGER) {
			typeIdentifier = "I";
			fv = cw.visitField(ACC_PUBLIC, varName, typeIdentifier, null, new Integer(0));
		} else if (paramDec.getTypeName() == TypeName.BOOLEAN) {
			typeIdentifier = "Z";
			fv = cw.visitField(ACC_PUBLIC, varName, typeIdentifier, null, new Boolean(false));
		} else if (paramDec.getTypeName() == TypeName.FILE) {
			typeIdentifier = "Ljava/io/File;";
			fv = cw.visitField(ACC_PUBLIC, varName, typeIdentifier, null, null);
		} else if (paramDec.getTypeName() == TypeName.URL) {
			typeIdentifier = "Ljava/net/URL;";
			fv = cw.visitField(ACC_PUBLIC, varName, typeIdentifier, null, null);
		}
		fv.visitEnd();
		// For assignment 5, only needs to handle integers and booleans
		/*
		 * fv = cw.visitField(0, "a", "I", null, null); fv.visitEnd();
		 * mv.visitVarInsn(ALOAD, 0); mv.visitInsn(ICONST_5);
		 * mv.visitFieldInsn(PUTFIELD, "cop5556sp17/Test", "a", "I");
		 */
		if (typeIdentifier.equals("I")) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitIntInsn(SIPUSH, arg_index++);
			mv.visitInsn(AALOAD);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
		} else if (typeIdentifier.equals("Z")) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitIntInsn(SIPUSH, arg_index++);
			mv.visitInsn(AALOAD);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
		} else if (typeIdentifier.equals("Ljava/io/File;")) {
			mv.visitVarInsn(ALOAD, 0);			
			
			mv.visitTypeInsn(NEW, "java/io/File");
			mv.visitInsn(DUP);

			mv.visitVarInsn(ALOAD, 1);
			mv.visitIntInsn(SIPUSH, arg_index++);
			mv.visitInsn(AALOAD);

			mv.visitMethodInsn(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V", false);
		} else if(typeIdentifier.equals("Ljava/net/URL;")) {
		    mv.visitVarInsn(ALOAD, 1);
		    mv.visitIntInsn(SIPUSH, arg_index++);

		    mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "getURL", PLPRuntimeImageIO.getURLSig, false);
            mv.visitVarInsn(ALOAD, 0);

            mv.visitInsn(SWAP);
		}
		mv.visitFieldInsn(PUTFIELD, className, varName, typeIdentifier);
		return null;

	}

	@Override
	public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception {
		sleepStatement.getE().visit(this, arg);
		mv.visitInsn(I2L);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);
		return null;
	}

	@Override
	public Object visitTuple(Tuple tuple, Object arg) throws Exception {
		for (Expression exprn : tuple.getExprList()) {
			exprn.visit(this, arg);
		}  
		return null;
	}

	@Override
	public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception {
		// TODO Implement this
		Label blockStart = new Label();
		Label blockEnd = new Label();
		mv.visitLabel(blockStart);
		whileStatement.getE().visit(this, null);
		mv.visitJumpInsn(IFEQ, blockEnd);
		whileStatement.getB().visit(this, arg);
		mv.visitJumpInsn(GOTO, blockStart);
		mv.visitLabel(blockEnd);
		// local variables
		for (Dec dec : whileStatement.getB().getDecs()) {
			if (dec.getTypeName() == TypeName.INTEGER) {
				mv.visitLocalVariable(dec.getIdent().getText(), "I", null, blockStart, blockEnd, dec.getSlotNum());
			} else if (dec.getTypeName() == TypeName.BOOLEAN) {
				mv.visitLocalVariable(dec.getIdent().getText(), "Z", null, blockStart, blockEnd, dec.getSlotNum());
			} else if(dec.getTypeName() == TypeName.FRAME) {
                mv.visitLocalVariable(dec.getIdent().getText(), PLPRuntimeFrame.JVMDesc, null, blockStart, blockEnd, dec.getSlotNum());
            }
            else if(dec.getTypeName() == TypeName.IMAGE) {
                mv.visitLocalVariable(dec.getIdent().getText(), "Ljava/awt/image/BufferedImage;", null, blockStart, blockEnd, dec.getSlotNum());
            }
			
		}
		return null;
	}

}
