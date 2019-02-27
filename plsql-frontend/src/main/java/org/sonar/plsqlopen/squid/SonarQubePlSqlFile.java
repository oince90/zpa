/*
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
package org.sonar.plsqlopen.squid;

import java.io.IOException;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.plsqlopen.api.PlSqlFile;

public class SonarQubePlSqlFile implements PlSqlFile {

    private final InputFile inputFile;

    private SonarQubePlSqlFile(InputFile inputFile) {
      this.inputFile = inputFile;
    }

    public static PlSqlFile create(InputFile inputFile) {
        return new SonarQubePlSqlFile(inputFile);
    }

    @Override
    public String fileName() {
        return inputFile.filename();
    }

    @Override
    @Deprecated
    public InputFile inputFile() {
        return inputFile;
    }
    
    @Override
    public String contents() {
        try {
            return inputFile.contents();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read contents of input file " + inputFile(), e);
        }
    }

}
