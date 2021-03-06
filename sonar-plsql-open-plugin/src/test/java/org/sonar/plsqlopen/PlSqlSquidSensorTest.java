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
package org.sonar.plsqlopen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.plsqlopen.checks.CheckList;

import com.google.common.io.Files;

public class PlSqlSquidSensorTest {

    private PlSqlSquidSensor sensor;
    private SensorContextTester context;
    private FileLinesContext fileLinesContext;
    
    @Before
    public void setUp() {
        ActiveRules activeRules = (new ActiveRulesBuilder())
                .create(RuleKey.of(CheckList.REPOSITORY_KEY, "EmptyBlock"))
                .setName("Print Statement Usage")
                .activate()
                .build();
        CheckFactory checkFactory = new CheckFactory(activeRules);
        context = SensorContextTester.create(new File("."));
        
        FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
        fileLinesContext = mock(FileLinesContext.class);
        when(fileLinesContextFactory.createFor(Mockito.any(InputFile.class))).thenReturn(fileLinesContext);
    
        sensor = new PlSqlSquidSensor(checkFactory, new MapSettings().asConfig(), new NoSonarFilter(), fileLinesContextFactory, null);
    }
    
    @Test
    public void testDescriptor() {
        DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
        sensor.describe(descriptor);
        assertThat(descriptor.name()).isEqualTo("Z PL/SQL Analyzer");
        assertThat(descriptor.languages()).containsOnly(PlSql.KEY);
    }
    
    @Test
    public void shouldAnalyse() throws IOException {
      String relativePath = "src/test/resources/org/sonar/plsqlopen/code.sql";
      DefaultInputFile inputFile = new TestInputFileBuilder("key", relativePath)
              .setLanguage(PlSql.KEY)
              .setCharset(StandardCharsets.UTF_8)
              .initMetadata(Files.toString(new File(relativePath), StandardCharsets.UTF_8))
              .setModuleBaseDir(Paths.get(""))
              .build();
      
      context.fileSystem().add(inputFile);
      
      sensor.execute(context);
      
      String key = inputFile.key();

      //assertThat(context.measure(key, CoreMetrics.FILES).value()).isEqualTo(1);
      assertThat(context.measure(key, CoreMetrics.NCLOC).value()).isEqualTo(18);
      assertThat(context.measure(key, CoreMetrics.COMMENT_LINES).value()).isEqualTo(2);
      assertThat(context.measure(key, CoreMetrics.COMPLEXITY).value()).isEqualTo(6);
      assertThat(context.measure(key, CoreMetrics.FUNCTIONS).value()).isEqualTo(2);
      assertThat(context.measure(key, CoreMetrics.STATEMENTS).value()).isEqualTo(8);
      verify(fileLinesContext, times(8)).setIntValue(Mockito.eq(CoreMetrics.EXECUTABLE_LINES_DATA_KEY), Mockito.anyInt(), Mockito.eq(1));
      verify(fileLinesContext).save();
    }

    @Test
    public void shouldAnalyseTestFile() throws IOException {
        String relativePath = "src/test/resources/org/sonar/plsqlopen/test.sql";
        DefaultInputFile inputFile = new TestInputFileBuilder("key", relativePath)
            .setLanguage(PlSql.KEY)
            .setType(InputFile.Type.TEST)
            .setCharset(StandardCharsets.UTF_8)
            .initMetadata(Files.toString(new File(relativePath), StandardCharsets.UTF_8))
            .setModuleBaseDir(Paths.get(""))
            .build();

        context.fileSystem().add(inputFile);

        sensor.execute(context);

        String key = inputFile.key();

        // shouldn't save metrics for test files
        assertThat(context.measure(key, CoreMetrics.NCLOC)).isNull();
        assertThat(context.measure(key, CoreMetrics.COMMENT_LINES)).isNull();
        assertThat(context.measure(key, CoreMetrics.COMPLEXITY)).isNull();
        assertThat(context.measure(key, CoreMetrics.FUNCTIONS)).isNull();
        assertThat(context.measure(key, CoreMetrics.STATEMENTS)).isNull();
        verifyZeroInteractions(fileLinesContext);

        // but should save highlighting data
        assertThat(context.highlightingTypeAt(key, 1, lineOffset(1))).containsExactly(TypeOfText.KEYWORD);
        assertThat(context.highlightingTypeAt(key, 2, lineOffset(3))).containsExactly(TypeOfText.COMMENT);
        assertThat(context.highlightingTypeAt(key, 3, lineOffset(3))).containsExactly(TypeOfText.STRUCTURED_COMMENT);
        assertThat(context.highlightingTypeAt(key, 6, lineOffset(8))).containsExactly(TypeOfText.STRING);
        assertThat(context.highlightingTypeAt(key, 7, lineOffset(1))).containsExactly(TypeOfText.KEYWORD);
    }
    
    private int lineOffset(int offset) {
        return offset - 1;
    }
    
}
