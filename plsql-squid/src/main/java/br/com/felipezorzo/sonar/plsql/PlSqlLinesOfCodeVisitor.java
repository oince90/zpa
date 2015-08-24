/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015 Felipe Zorzo
 * felipe.b.zorzo@gmail.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package br.com.felipezorzo.sonar.plsql;

import static com.sonar.sslr.api.GenericTokenType.EOF;

import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.measures.MetricDef;

import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Token;

public class PlSqlLinesOfCodeVisitor extends SquidAstVisitor<Grammar> implements AstAndTokenVisitor {

    private final MetricDef metric;
    private int lastTokenLine;

    public PlSqlLinesOfCodeVisitor(MetricDef metric) {
        this.metric = metric;
    }

    @Override
    public void visitFile(AstNode node) {
        lastTokenLine = -1;
    }

    @Override
    public void visitToken(Token token) {
        if (!token.getType().equals(EOF)) {
            /* Handle all the lines of the token */
            String[] tokenLines = token.getValue().split("\n", -1);

            int firstLineAlreadyCounted = lastTokenLine == token.getLine() ? 1 : 0;
            getContext().peekSourceCode().add(metric, (double) tokenLines.length - firstLineAlreadyCounted);

            lastTokenLine = token.getLine() + tokenLines.length - 1;
        }
    }

}