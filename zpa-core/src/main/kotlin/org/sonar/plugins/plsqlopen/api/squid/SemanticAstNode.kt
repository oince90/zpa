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
package org.sonar.plugins.plsqlopen.api.squid

import com.sonar.sslr.api.AstNode

import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType
import org.sonar.plugins.plsqlopen.api.symbols.Symbol

class SemanticAstNode(astNode: AstNode) : AstNode(astNode.type, astNode.name, astNode.token) {

    var symbol: Symbol? = null
        set(symbol) {
            field = symbol

            for (node in children) {
                (node as SemanticAstNode).symbol = symbol
            }
        }

    var plSqlType: PlSqlType? = PlSqlType.UNKNOWN
        get() = this.symbol?.type() ?:  field

    init {
        super.setFromIndex(astNode.fromIndex)
        super.setToIndex(astNode.toIndex)
    }
}
