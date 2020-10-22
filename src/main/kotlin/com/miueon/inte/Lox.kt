package com.miueon.inte

import com.miueon.inte.Lox.Companion.runFile
import com.miueon.inte.Lox.Companion.runPrompt
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.jvm.Throws


class Lox {
    companion object{
        var hadError:Boolean = false
        var hadRuntimeError:Boolean = false
        val interpreter = Interpreter()
        val resolver = Resolver(interpreter)
        fun runFile(path: String) {
            val bytes = Files.readAllBytes(Paths.get(path))
            run(String(bytes, Charset.defaultCharset()))

            // Indicate an error by the exit code
            if (hadError) {
                System.exit(65)
            }
            if (hadRuntimeError) {
                System.exit(70)
            }
        }

        fun runPrompt() {
            val input = InputStreamReader(System.`in`)
            val reader = BufferedReader(input)

            while (true) {
                print("> ")
                // ctrl-D will signals an "end-of-file" condition
                // this will make readLine get null
                var line: String? = reader.readLine() ?: break
                run(line!!)
                hadError = false
            }
        }

        fun run(source: String) {
            val scanner = Scanner(source)
            val tokens:List<Token> = scanner.scanTokens()
            val parser = Parser(tokens)
            val statements = parser.parse()

            if (hadError) return
           // println(AstPrinter().print(expression!!))


            resolver.resolve(statements)

            if (hadError) {
                return
            }
            interpreter.interpret(statements)
        }

        fun error(line: Int, msg: String) {
            report(line, "", msg)
        }
        fun runtimeError(error: RuntimeError) {
            System.err.println("${error.message}\n[line ${error.token.line}]")
            hadRuntimeError = true
        }
        private fun report(line: Int, where: String, msg: String) {
            System.err.println(
                "[line ${line} ] Error ${where} : ${msg}"
            )
            hadError = true
        }
        fun error(token: Token, msg: String) {
            if (token.type == TokenType.EOF) {
                report(token.line, " at end", msg)
            } else {
                report(token.line, " at '${token.lexeme}'", msg)
            }
        }
    }
}

fun main(args:Array<String>) {
    if (args.size > 1) {
        println("Usage: jlox [srcipt]")
        System.exit(64)
    } else if (args.size == 1) {
        runFile(args[0])
    } else {
        runPrompt()
    }
}