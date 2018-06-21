// Dvir Schnaps 208299453
// Yishai Jaffe 207612920
// Targil 2

//QUESTIONS:
//WHEN JUMPING TO "LABEL" DO I NEED TO REALLY JUMP TO "FILENAME.LABEL"?
//DO I NEED SEPERATE LABEL COUNTERS FOR DIFFERENT KINDS OF LABELS?
//IS THE INIT CORRECT?

import java.io.File

val map = hashMapOf("local" to "LCL", "argument" to "ARG", "this" to "THIS", "that" to "THAT")
var fileName = ""
var lc = 1
val initSP = """
	|@256
	|D=A
	|@SP
	|M=D""".trimMargin()

fun main(args: Array<String>) {
	val input = readLine()
	var vm = File(input)
	var asm = File(input + ".asm")
	asm.delete()
	asm.appendText(initASM())
	vm.listFiles().forEach{it ->
		if (it.name.endsWith(".vm")){
			fileName = it.name.split(".")[0]// for static push/pop
			it.forEachLine{it1 ->
				if(!it1.startsWith("/") && !it1.isEmpty())
					asm.appendText(VMtoASM(it1))}
		}
	}
	println("All done, man :)")
}

fun labelCounter(increment: Boolean): Int{
	val temp = lc
	if (increment) lc += 1
	return temp
}

fun initASM(): String =
		initSP + "\n\n" + VMtoASM("call Sys.init 0")

fun VMtoASM(command: String): String = run {
	val splitCommand = command.replace(Regex("\\s+"), " ").split(" ")//split to words
	when(splitCommand[0]){
		"add" -> binaryAction("D+M")
		"sub" -> binaryAction("M-D")
		"neg" -> unaryAction("-M")
		"eq" -> compare("JEQ")
		"gt" -> compare("JGT")
		"lt" -> compare("JLT")
		"and" -> binaryAction("D&M")
		"or" -> binaryAction("D|M")
		"not" -> unaryAction("!M")
		"pop" -> pop(splitCommand[1], splitCommand[2].toInt())
		"push" -> push(splitCommand[1], splitCommand[2].toInt())
		"label" -> label(splitCommand[1])
		"goto" -> goto(splitCommand[1])
		"if-goto" -> if_goto(splitCommand[1])
		"function" -> function(splitCommand[1],splitCommand[2])
		"call" -> call(splitCommand[1],splitCommand[2])
		"return" -> ret()
		else -> "error <- <- <- <- <- <-"
	}.trimMargin() + "\n\n"
}

fun binaryAction(action: String): String =
		"""
	|@SP
	|AM=M-1
	|D=M
	|A=A-1
	|M=$action"""

fun unaryAction(action: String): String =
		"""
	|@SP
	|A=M-1
	|M=$action"""

fun compare(cmp: String): String =
		"""
	|@SP
	|AM=M-1
	|D=M
	|A=A-1
	|D=M-D
	|@${cmp}_${labelCounter(false)}
	|D;$cmp
	|@SP
	|A=M-1
	|M=0
	|@END_${labelCounter(false)}
	|0;JMP
	|(${cmp}_${labelCounter(false)})
	|@SP
	|A=M-1
	|M=-1
	|(END_${labelCounter(true)})"""

fun pop(segment: String, index: Int): String =
		when (segment) {
			"local", "argument", "this", "that" -> """
			|@${map[segment]}
			|A=M
			|D=A
			|@${index}
			|D=D+A
			|@R15
			|M=D
			|@SP
			|AM=M-1
			|D=M
			|@R15
			|A=M
			|M=D"""
			"temp" -> """
			|@5
			|D=A
			|@${index}
			|D=D+A
			|@R15
			|M=D
			|@SP
			|AM=M-1
			|D=M
			|@R15
			|A=M
			|M=D"""
			"static" -> """
			|@SP
			|AM=M-1
			|D=M
			|@${fileName}.$index
			|M=D"""
			"pointer" -> {
				val pointer = if (index == 0) "this" else "that"
				"""
			|@SP
			|AM=M-1
			|D=M
			|@${map[pointer]}
			|M=D"""
			}
			else -> "error pop <- <- <- <- <- <-"
		}

fun push(segment: String, index: Int): String =
		when (segment) {
			"local", "argument", "this", "that" -> """
			|@${map[segment]}
			|A=M
			|D=A
			|@${index}
			|D=D+A
			|A=D
			|D=M
			|@SP
			|M=M+1
			|A=M-1
			|M=D"""
			"temp" -> """
			|@5
			|D=A
			|@${index}
			|D=D+A
			|A=D
			|D=M
			|@SP
			|M=M+1
			|A=M-1
			|M=D"""
			"static" -> """
			|@${fileName}.$index
			|D=M
			|@SP
			|M=M+1
			|A=M-1
			|M=D"""
			"pointer" -> {
				val pointer = if (index == 0) "this" else "that"
				"""
			|@${map[pointer]}
			|D=M
			|@SP
			|M=M+1
			|A=M-1
			|M=D"""
			}
			"constant" -> """
			|@$index
			|D=A
			|@SP
			|M=M+1
			|A=M-1
			|M=D
			"""
			else -> "error push <- <- <- <- <- <-"
		}

fun label(label: String): String = "($label)"

fun goto(label: String): String =
		"""
	|@$label
	|0;JMP"""

fun  if_goto(label: String): String =
		"""
	|@SP
	|AM=M-1
	|D=M
	|@$label
	|D;JNE"""

fun function(name: String, locals: String) =
		"""
	|($name)
	|@$locals
	|D=A
	|(INIT_$name)
	|@END_INIT_$name
	|D;JEQ
	|@SP
	|M=M+1
	|A=M-1
	|M=0
	|D=D-1
	|@INIT_$name
	|0;JMP
	|(END_INIT_$name)"""

fun call(name: String, arguments: String): String =
		"""
	|@RETURN_FROM_${name}_${labelCounter(false)}
	|D=A
	|@SP
	|M=M+1
	|A=M-1
	|M=D
	|@LCL
	|D=M
	|@SP
	|M=M+1
	|A=M-1
	|M=D
	|@ARG
	|D=M
	|@SP
	|M=M+1
	|A=M-1
	|M=D
	|@THIS
	|D=M
	|@SP
	|M=M+1
	|A=M-1
	|M=D
	|@THAT
	|D=M
	|@SP
	|M=M+1
	|A=M-1
	|M=D
	|@SP
	|D=M
	|@$arguments
	|D=D-A
	|@5
	|D=D-A
	|@ARG
	|M=D
	|@SP
	|D=M
	|@LCL
	|M=D
	|@$name
	|0;JMP
	|(RETURN_FROM_${name}_${labelCounter(true)})"""

fun ret(): String =
		"""
	|@LCL
	|D=M
	|@5
	|A=D-A
	|D=M
	|@R15
	|M=D
	|@SP
	|A=M-1
	|D=M
	|@ARG
	|A=M
	|M=D
	|D=A+1
	|@SP
	|M=D
	|@LCL
	|AM=M-1
	|D=M
	|@THAT
	|M=D
	|@LCL
	|AM=M-1
	|D=M
	|@THIS
	|M=D
	|@LCL
	|AM=M-1
	|D=M
	|@ARG
	|M=D
	|@LCL
	|A=M-1
	|D=M
	|@LCL
	|M=D
	|@R15
	|A=M
	|0;JMP"""



