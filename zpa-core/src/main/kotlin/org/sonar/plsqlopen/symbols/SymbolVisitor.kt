/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plsqlopen.symbols

import com.sonar.sslr.api.AstNode
import com.sonar.sslr.api.AstNodeType
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.PlSqlPunctuator
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck
import org.sonar.plugins.plsqlopen.api.squid.SemanticAstNode
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType
import org.sonar.plugins.plsqlopen.api.symbols.Scope
import org.sonar.plugins.plsqlopen.api.symbols.Symbol

class SymbolVisitor(private val typeSolver: DefaultTypeSolver?) : PlSqlCheck() {

    private val scopeHolders = arrayOf<AstNodeType>(
        PlSqlGrammar.CREATE_PROCEDURE,
        PlSqlGrammar.PROCEDURE_DECLARATION,
        PlSqlGrammar.CREATE_FUNCTION,
        PlSqlGrammar.FUNCTION_DECLARATION,
        PlSqlGrammar.CREATE_PACKAGE,
        PlSqlGrammar.CREATE_PACKAGE_BODY,
        PlSqlGrammar.SIMPLE_DML_TRIGGER,
        PlSqlGrammar.INSTEAD_OF_DML_TRIGGER,
        PlSqlGrammar.COMPOUND_DML_TRIGGER,
        PlSqlGrammar.SYSTEM_TRIGGER,
        PlSqlGrammar.TYPE_CONSTRUCTOR,
        PlSqlGrammar.CREATE_TYPE,
        PlSqlGrammar.CREATE_TYPE_BODY,
        PlSqlGrammar.BLOCK_STATEMENT,
        PlSqlGrammar.FOR_STATEMENT,
        PlSqlGrammar.CURSOR_DECLARATION)

    private lateinit var symbolTable: SymbolTableImpl
    private var currentScope: Scope? = null

    val symbols: List<Symbol>
        get() = if (::symbolTable.isInitialized) symbolTable.symbols else emptyList()

    override fun init() {
        subscribeTo(*scopeHolders)
    }

    override fun visitFile(node: AstNode) {
        symbolTable = SymbolTableImpl()

        visit(node)

        context.symbolTable = symbolTable
    }

    override fun visitNode(node: AstNode) {
        if (node.`is`(*scopeHolders)) {
            context.currentScope = symbolTable.getScopeFor(node)
        }
    }

    override fun leaveNode(node: AstNode) {
        if (node.`is`(*scopeHolders)) {
            context.currentScope = context.currentScope?.outer()
        }
    }

    override fun leaveFile(node: AstNode) {
        currentScope = null
    }

    private fun visit(ast: AstNode) {
        visitNodeInternal(ast)
        visitChildren(ast)

        if (ast.`is`(*scopeHolders)) {
            leaveScope()
        }
    }

    private fun visitChildren(ast: AstNode) {
        for (child in ast.children) {
            visit(child)
        }
    }

    private fun visitNodeInternal(node: AstNode) {
        if (node.type === PlSqlGrammar.VARIABLE_DECLARATION) {
            visitVariableDeclaration(node)
        } else if (node.type === PlSqlGrammar.VARIABLE_NAME) {
            visitVariableName(node)
        } else if (node.type === PlSqlGrammar.CURSOR_DECLARATION) {
            visitCursor(node)
        } else if (node.type === PlSqlGrammar.BLOCK_STATEMENT) {
            visitBlock(node)
        } else if (node.type === PlSqlGrammar.FOR_STATEMENT) {
            visitFor(node)
        } else if (node.type === PlSqlGrammar.PARAMETER_DECLARATION || node.type === PlSqlGrammar.CURSOR_PARAMETER_DECLARATION) {
            visitParameterDeclaration(node)
        } else if (node.type === PlSqlGrammar.CREATE_PROCEDURE ||
                node.type === PlSqlGrammar.PROCEDURE_DECLARATION ||
                node.type === PlSqlGrammar.CREATE_FUNCTION ||
                node.type === PlSqlGrammar.FUNCTION_DECLARATION ||
                node.type === PlSqlGrammar.SIMPLE_DML_TRIGGER ||
                node.type === PlSqlGrammar.INSTEAD_OF_DML_TRIGGER ||
                node.type === PlSqlGrammar.COMPOUND_DML_TRIGGER ||
                node.type === PlSqlGrammar.SYSTEM_TRIGGER ||
                node.type === PlSqlGrammar.CREATE_TYPE ||
                node.type === PlSqlGrammar.CREATE_TYPE_BODY ||
                node.type === PlSqlGrammar.TYPE_CONSTRUCTOR) {
            visitUnit(node)
        } else if (node.type === PlSqlGrammar.CREATE_PACKAGE || node.type === PlSqlGrammar.CREATE_PACKAGE_BODY) {
            visitPackage(node)
        } else if (node.type === PlSqlGrammar.LITERAL) {
            visitLiteral(node)
        }
    }

    private fun visitUnit(node: AstNode) {
        val autonomousTransaction = node.select()
                .children(PlSqlGrammar.DECLARE_SECTION)
                .children(PlSqlGrammar.PRAGMA_DECLARATION)
                .children(PlSqlGrammar.AUTONOMOUS_TRANSACTION_PRAGMA).isNotEmpty
        val exceptionHandler = node.select()
                .children(PlSqlGrammar.STATEMENTS_SECTION)
                .children(PlSqlGrammar.EXCEPTION_HANDLER).isNotEmpty
        enterScope(node, autonomousTransaction, exceptionHandler)
    }

    private fun visitPackage(node: AstNode) {
        enterScope(node, false, false)
    }

    private fun visitCursor(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        createSymbol(identifier, Symbol.Kind.CURSOR, PlSqlType.UNKNOWN)
        enterScope(node, null, null)
    }

    private fun visitBlock(node: AstNode) {
        val exceptionHandler = node.select()
                .children(PlSqlGrammar.STATEMENTS_SECTION)
                .children(PlSqlGrammar.EXCEPTION_HANDLER).isNotEmpty
        enterScope(node, null, exceptionHandler)
    }

    private fun visitFor(node: AstNode) {
        enterScope(node, null, null)
        val identifier = node.getFirstChild(PlSqlKeyword.FOR).nextSibling

        val type = if (node.hasDirectChildren(PlSqlPunctuator.RANGE)) {
            PlSqlType.NUMERIC
        } else {
            PlSqlType.ROWTYPE
        }

        createSymbol(identifier, Symbol.Kind.VARIABLE, type)
    }

    private fun visitVariableDeclaration(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val datatype = node.getFirstChild(PlSqlGrammar.DATATYPE)

        val type = solveType(datatype)
        createSymbol(identifier, Symbol.Kind.VARIABLE, type)
    }

    private fun visitParameterDeclaration(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        val datatype = node.getFirstChild(PlSqlGrammar.DATATYPE)

        val type = solveType(datatype)
        createSymbol(identifier, Symbol.Kind.PARAMETER, type).addModifiers(node.getChildren(PlSqlKeyword.IN, PlSqlKeyword.OUT))
    }

    private fun visitVariableName(node: AstNode) {
        val identifier = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        if (identifier != null && currentScope != null) {
            val symbol = currentScope?.getSymbol(identifier.tokenOriginalValue)
            if (symbol != null) {
                symbol.addUsage(identifier)
                semantic(node).symbol = symbol
            }
        }
    }

    private fun visitLiteral(node: AstNode) {
        (node as SemanticAstNode).plSqlType = solveType(node)
    }

    private fun createSymbol(identifier: AstNode, kind: Symbol.Kind, type: PlSqlType): Symbol {
        val symbol = symbolTable.declareSymbol(identifier, kind, currentScope!!, type)
        semantic(identifier).symbol = symbol
        return symbol
    }

    private fun enterScope(node: AstNode, autonomousTransaction: Boolean?, exceptionHandler: Boolean?) {
        var autonomous = false
        var exception = false

        with(currentScope) {
            if (autonomousTransaction != null) {
                autonomous = autonomousTransaction
            } else if (this != null) {
                autonomous = this.isAutonomousTransaction
            }

            if (this != null) {
                exception = this.hasExceptionHandler() || java.lang.Boolean.TRUE == exceptionHandler
            } else if (exceptionHandler != null) {
                exception = exceptionHandler
            }
        }

        val scope = ScopeImpl(currentScope, node, autonomous, exception)
        symbolTable.addScope(scope)
        currentScope = scope
    }

    private fun leaveScope() {
        val scope = currentScope
        requireNotNull(scope) { "Current scope should never be null when calling method \"leaveScope\"" }
        currentScope = scope.outer()
    }

    private fun solveType(node: AstNode?): PlSqlType {
        var type = PlSqlType.UNKNOWN
        if (typeSolver != null) {
            type = typeSolver.solve(node)
        }
        return type
    }

}
