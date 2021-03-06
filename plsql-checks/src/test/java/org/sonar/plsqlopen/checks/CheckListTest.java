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
package org.sonar.plsqlopen.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.Test;

public class CheckListTest {

    /**
     * Enforces that each check declared in list.
     */
    @Test
    public void count() {
        int count = 0;
        String[] files = new File("src/main/java/org/sonar/plsqlopen/checks/").list((file, name) -> name.endsWith("java"));
        for (String file : files) {
            if (file.endsWith("Check.java") && !file.startsWith("Abstract")) {
                count++;
            }
        }
        assertThat(CheckList.getChecks().size()).isEqualTo(count);
    }
    
}
