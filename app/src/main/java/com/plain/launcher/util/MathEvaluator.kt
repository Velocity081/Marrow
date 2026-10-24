package com.plain.launcher.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.*

data class MathResult(
    val caption: String,
    val result: String
)

object MathEvaluator {

    private val ALLOWED_WORDS = setOf(
        "sqrt", "abs", "sin", "cos", "tan", "log", "ln", "pi", "e", "of"
    )

    fun evaluate(rawQuery: String): MathResult? {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return null

        // Remove trailing '=' if user typed e.g. "2+2="
        val cleanedQuery = if (trimmed.endsWith("=")) {
            trimmed.dropLast(1).trim()
        } else {
            trimmed
        }

        if (cleanedQuery.isEmpty()) return null

        // Must not be a single number (e.g. "42", "3.14")
        if (cleanedQuery.toDoubleOrNull() != null) return null

        // Convert query to lower case for token analysis
        val lower = cleanedQuery.lowercase()

        // Check for disallowed letters/words (to avoid matching app names like "chrome", "7zip", "formula 1")
        val wordRegex = Regex("[a-zA-Z]+")
        val words = wordRegex.findAll(lower).map { it.value }.toList()
        for (w in words) {
            // 'x' is allowed as multiplication
            if (w == "x") continue
            if (!ALLOWED_WORDS.contains(w)) {
                return null
            }
        }

        // Must contain at least one math operator or recognized math function
        val hasOperator = lower.contains('+') ||
                lower.contains('-') ||
                lower.contains('*') ||
                lower.contains('×') ||
                lower.contains('/') ||
                lower.contains('÷') ||
                lower.contains('^') ||
                lower.contains('%') ||
                lower.contains("sqrt") ||
                lower.contains("abs") ||
                lower.contains("sin") ||
                lower.contains("cos") ||
                lower.contains("tan") ||
                lower.contains("log") ||
                lower.contains("ln") ||
                lower.contains("of") ||
                Regex("""\d\s*[xX]\s*\d""").containsMatchIn(cleanedQuery)

        if (!hasOperator) return null

        // Prepare normalized expression for parser
        var expr = lower
            .replace('×', '*')
            .replace('÷', '/')
            .replace(Regex("""\bof\b"""), "*")

        // Replace 'x' between numbers or parens with '*'
        expr = expr.replace(Regex("""(?<=\d|\))\s*[xX]\s*(?=\d|\(|sqrt|abs|sin|cos|tan|log|ln|pi|e)"""), "*")

        return try {
            val parser = Parser(expr)
            val value = parser.parse()
            if (value.isNaN() || value.isInfinite()) {
                null
            } else {
                val formatted = formatResult(value)
                MathResult(
                    caption = cleanedQuery,
                    result = formatted
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatResult(value: Double): String {
        // Round very close values to eliminate floating point precision artifacts (e.g. 0.1 + 0.2 -> 0.3)
        val roundedTo10 = (value * 1e10).roundToLong() / 1e10

        val roundedLong = Math.round(roundedTo10)
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }

        return if (abs(roundedTo10 - roundedLong) < 1e-9) {
            val df = DecimalFormat("#,###", symbols)
            df.format(roundedLong)
        } else {
            val df = DecimalFormat("#,##0.########", symbols)
            df.format(roundedTo10)
        }
    }

    private class Parser(private val input: String) {
        private var pos = 0
        private val len = input.length

        fun parse(): Double {
            skipWhitespace()
            val result = parseExpression()
            skipWhitespace()
            if (pos < len) {
                // Unconsumed characters left => syntax error
                throw IllegalArgumentException("Unexpected character at $pos: ${input[pos]}")
            }
            return result
        }

        // Expression = Term (('+' | '-') Term)*
        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipWhitespace()
                if (pos >= len) break
                when (input[pos]) {
                    '+' -> {
                        pos++
                        value += parseTerm()
                    }
                    '-' -> {
                        pos++
                        value -= parseTerm()
                    }
                    else -> break
                }
            }
            return value
        }

        // Term = Factor (('*' | '/') Factor)*
        private fun parseTerm(): Double {
            var value = parseFactor()
            while (true) {
                skipWhitespace()
                if (pos >= len) break
                when (input[pos]) {
                    '*' -> {
                        pos++
                        value *= parseFactor()
                    }
                    '/' -> {
                        pos++
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        value /= divisor
                    }
                    else -> break
                }
            }
            return value
        }

        // Factor = Unary ('^' Factor)?
        private fun parseFactor(): Double {
            val value = parseUnary()
            skipWhitespace()
            if (pos < len && input[pos] == '^') {
                pos++
                val exponent = parseFactor()
                return value.pow(exponent)
            }
            return value
        }

        // Unary = ('+' | '-')? Primary ('%')*
        private fun parseUnary(): Double {
            skipWhitespace()
            if (pos >= len) throw IllegalArgumentException("Unexpected end of input")

            if (input[pos] == '+') {
                pos++
                return parseUnary()
            }
            if (input[pos] == '-') {
                pos++
                return -parseUnary()
            }

            var value = parsePrimary()
            skipWhitespace()
            while (pos < len && input[pos] == '%') {
                pos++
                value /= 100.0
                skipWhitespace()
            }
            return value
        }

        // Primary = Number | '(' Expression ')' | Function '(' Expression ')' | Function Primary | Constant
        private fun parsePrimary(): Double {
            skipWhitespace()
            if (pos >= len) throw IllegalArgumentException("Unexpected end of input")

            val ch = input[pos]

            if (ch == '(') {
                pos++
                val value = parseExpression()
                skipWhitespace()
                if (pos >= len || input[pos] != ')') {
                    throw IllegalArgumentException("Missing closing parenthesis")
                }
                pos++ // consume ')'
                return value
            }

            if (ch.isDigit() || ch == '.') {
                return parseNumber()
            }

            if (ch.isLetter()) {
                val start = pos
                while (pos < len && input[pos].isLetter()) {
                    pos++
                }
                val word = input.substring(start, pos)
                return when (word) {
                    "pi" -> Math.PI
                    "e" -> Math.E
                    "sqrt" -> {
                        val arg = parseFunctionArg()
                        if (arg < 0.0) throw ArithmeticException("Negative square root")
                        sqrt(arg)
                    }
                    "abs" -> abs(parseFunctionArg())
                    "sin" -> sin(Math.toRadians(parseFunctionArg()))
                    "cos" -> cos(Math.toRadians(parseFunctionArg()))
                    "tan" -> tan(Math.toRadians(parseFunctionArg()))
                    "log" -> {
                        val arg = parseFunctionArg()
                        if (arg <= 0.0) throw ArithmeticException("Non-positive logarithm")
                        log10(arg)
                    }
                    "ln" -> {
                        val arg = parseFunctionArg()
                        if (arg <= 0.0) throw ArithmeticException("Non-positive natural log")
                        ln(arg)
                    }
                    else -> throw IllegalArgumentException("Unknown identifier: $word")
                }
            }

            throw IllegalArgumentException("Unexpected character at $pos: $ch")
        }

        private fun parseFunctionArg(): Double {
            skipWhitespace()
            return if (pos < len && input[pos] == '(') {
                pos++
                val value = parseExpression()
                skipWhitespace()
                if (pos >= len || input[pos] != ')') {
                    throw IllegalArgumentException("Missing closing parenthesis for function")
                }
                pos++
                value
            } else {
                parseUnary()
            }
        }

        private fun parseNumber(): Double {
            val start = pos
            var hasDot = false
            while (pos < len) {
                val c = input[pos]
                if (c == '.') {
                    if (hasDot) break
                    hasDot = true
                    pos++
                } else if (c.isDigit()) {
                    pos++
                } else {
                    break
                }
            }
            val numStr = input.substring(start, pos)
            return numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $numStr")
        }

        private fun skipWhitespace() {
            while (pos < len && input[pos].isWhitespace()) {
                pos++
            }
        }
    }
}
