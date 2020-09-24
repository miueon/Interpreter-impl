package com.miueon.tool

import java.io.PrintWriter

class GenerateAst {
}

fun main(args:Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output directory>");
        System.exit(64);
    }
    val outputDir = args[0];
    defineAst(outputDir, "Expr", listOf(
        "Binary   : Expr left, Token operator, Expr right",
        "Grouping : Expr expression",
        "Literal  : Object value",
        "Unary    : Token operator, Expr right"
    ))
}

fun defineAst(outputDir: String, baseName: String, types: List<String>) {
    val path = "${outputDir}/${baseName}.java"
    PrintWriter(path, "UTF-8").use {
        it.println("package com.miueon.inte;")
        it.println()
        it.println("import java.util.List;")
        it.println()
        it.println("abstract class ${baseName} {")
        defineVisitor(it, baseName, types)
        //
        for (type in types) {
            val className = type.split(":")[0].trim()
            val fields = type.split(":")[1].trim()
            defineType(it, baseName, className, fields)
        }
        it.println()
        it.println("   abstract <R> R accept(Visitor<R> visitor);")
        it.println("}")
    }
}
fun defineVisitor(w: PrintWriter, baseName: String, types: List<String>) {
    w.println("  interface Visitor<R> {")
    for (type in types) {
        val typeName = type.split(":")[0].trim()
        w.println("   R visit${typeName}${baseName}(${typeName} ${baseName.toLowerCase()});")

    }
    w.println("  }")
}
fun defineType(w: PrintWriter, baseName: String, className: String, fieldList: String) {
    w.println("  static class ${className} extends ${baseName} {")

    // constructor
    w.println("    ${className} ( ${fieldList} ) {" )
    val fields = fieldList.split(", ")
    for (field in fields) {
        val name = field.split(" ")[1]
        w.println("    this.${name} = ${name};")
    }
    w.println("     }")
    // Visitor
    w.println()
    w.println("   @Override")
    w.println("   <R> R accept(Visitor<R> visitor) {")
    w.println("     return visitor.visit${className}${baseName}(this);")
    w.println("    }")
    // fields
    for (field in fields) {
        w.println("   final ${field};")
    }
    w.println("  }")
}