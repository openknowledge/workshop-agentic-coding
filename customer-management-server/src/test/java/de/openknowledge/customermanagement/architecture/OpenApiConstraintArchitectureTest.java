/*
 * Copyright (C) open knowledge GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.openknowledge.customermanagement.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class OpenApiConstraintArchitectureTest {

    private static final String ROOT_PACKAGE = "de.openknowledge.customermanagement";

    @Test
    void everyStringColumnLengthHasMatchingSizeConstraint() {
        var classes = new ClassFileImporter().importPackages(ROOT_PACKAGE);
        List<String> violations = new ArrayList<>();

        for (var javaClass : classes) {
            if (!javaClass.isAnnotatedWith(Entity.class)) {
                continue;
            }
            inspectEntity(javaClass.reflect(), violations);
        }

        assertThat(violations)
                .as("Each @Column(length) on a String path must have a matching @Size(max=length)")
                .isEmpty();
    }

    @Test
    void everyEntityColumnLengthMatchesFlywayMigration() throws Exception {
        Map<String, Integer> dbLengths = buildDbColumnLengthMap();
        var classes = new ClassFileImporter().importPackages(ROOT_PACKAGE);
        List<String> violations = new ArrayList<>();

        for (var javaClass : classes) {
            if (!javaClass.isAnnotatedWith(Entity.class)) {
                continue;
            }
            checkEntityVsDb(javaClass.reflect(), dbLengths, violations);
        }

        assertThat(violations)
                .as("Each @Column(length) must match the VARCHAR(N) in Flyway migrations")
                .isEmpty();
    }

    private void inspectEntity(Class<?> entityClass, List<String> violations) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.getAnnotation(Embedded.class) == null) {
                continue;
            }
            AttributeOverride[] overrides = field.getAnnotationsByType(AttributeOverride.class);
            for (AttributeOverride override : overrides) {
                checkOverride(entityClass, field, override, violations);
            }
        }
    }

    private void checkOverride(
            Class<?> entityClass,
            Field field,
            AttributeOverride override,
            List<String> violations) {
        String[] parts = override.name().split("\\.");
        if (parts.length != 2) {
            return;
        }
        Class<?> intermediateType = resolveComponentType(field.getType(), parts[0]);
        if (intermediateType == null || !intermediateType.isRecord()) {
            return;
        }
        RecordComponent leaf = findRecordComponent(intermediateType, parts[1]);
        if (leaf == null || leaf.getType() != String.class) {
            return;
        }
        Lob lob = leaf.getAnnotation(Lob.class);
        if (lob == null) {
            lob = leaf.getAccessor().getAnnotation(Lob.class);
        }
        if (lob != null) {
            return;
        }
        Column column = override.column();
        int length = column.length();
        Size size = leaf.getAnnotation(Size.class);
        if (size == null) {
            size = leaf.getAccessor().getAnnotation(Size.class);
        }
        if (size == null || size.max() != length) {
            int foundMax = size != null ? size.max() : -1;
            violations.add(
                    String.format(
                            "%s#%s '%s': column length=%d but @Size(max=%d)",
                            entityClass.getSimpleName(),
                            field.getName(),
                            override.name(),
                            length,
                            foundMax));
        }
    }

    private Class<?> resolveComponentType(Class<?> type, String componentName) {
        if (!type.isRecord()) {
            return null;
        }
        for (RecordComponent rc : type.getRecordComponents()) {
            if (rc.getName().equals(componentName)) {
                return rc.getType();
            }
        }
        return null;
    }

    private RecordComponent findRecordComponent(Class<?> type, String name) {
        for (RecordComponent rc : type.getRecordComponents()) {
            if (rc.getName().equals(name)) {
                return rc;
            }
        }
        return null;
    }

    private Map<String, Integer> buildDbColumnLengthMap() throws Exception {
        Map<String, Integer> result = new HashMap<>();
        URL migrationDir = getClass().getClassLoader().getResource("db/migration");
        if (migrationDir == null) {
            return result;
        }
        File[] sqlFiles =
                new File(migrationDir.toURI()).listFiles((dir, name) -> name.endsWith(".sql"));
        if (sqlFiles == null) {
            return result;
        }
        Arrays.sort(sqlFiles, Comparator.comparing(File::getName));
        for (File sqlFile : sqlFiles) {
            String sql = Files.readString(sqlFile.toPath()).toUpperCase();
            parseCreateTable(sql, result);
            parseAlterTable(sql, result);
        }
        return result;
    }

    private void parseCreateTable(String sql, Map<String, Integer> result) {
        Pattern tablePattern = Pattern.compile("CREATE\\s+TABLE\\s+(\\w+)");
        Pattern colPattern =
                Pattern.compile("^\\s+(\\w+)\\s+VARCHAR\\s*\\((\\d+)\\)", Pattern.MULTILINE);
        String[] statements = sql.split(";");
        for (String stmt : statements) {
            Matcher tableMatcher = tablePattern.matcher(stmt);
            if (!tableMatcher.find()) {
                continue;
            }
            String tableName = tableMatcher.group(1);
            Matcher colMatcher = colPattern.matcher(stmt);
            while (colMatcher.find()) {
                result.put(
                        tableName + "." + colMatcher.group(1),
                        Integer.parseInt(colMatcher.group(2)));
            }
        }
    }

    private void parseAlterTable(String sql, Map<String, Integer> result) {
        Pattern alterPattern =
                Pattern.compile(
                        "ALTER TABLE\\s+(\\w+)\\s+ALTER\\s+COLUMN\\s+(\\w+)"
                                + "\\s+TYPE\\s+VARCHAR\\s*\\((\\d+)\\)");
        Matcher matcher = alterPattern.matcher(sql);
        while (matcher.find()) {
            result.put(
                    matcher.group(1) + "." + matcher.group(2), Integer.parseInt(matcher.group(3)));
        }
    }

    private void checkEntityVsDb(
            Class<?> entityClass, Map<String, Integer> dbLengths, List<String> violations) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table == null) {
            return;
        }
        String tableName = table.name().toUpperCase();
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.getAnnotation(Embedded.class) == null) {
                continue;
            }
            AttributeOverride[] overrides = field.getAnnotationsByType(AttributeOverride.class);
            for (AttributeOverride override : overrides) {
                checkColumnVsDb(entityClass, tableName, override, dbLengths, violations);
            }
        }
    }

    private void checkColumnVsDb(
            Class<?> entityClass,
            String tableName,
            AttributeOverride override,
            Map<String, Integer> dbLengths,
            List<String> violations) {
        Column column = override.column();
        String colName = column.name().toUpperCase();
        Integer dbLength = dbLengths.get(tableName + "." + colName);
        if (dbLength == null) {
            return;
        }
        int jpaLength = column.length();
        if (jpaLength != dbLength) {
            violations.add(
                    String.format(
                            "%s.%s: @Column(length=%d) but Flyway defines VARCHAR(%d)",
                            entityClass.getSimpleName(), colName, jpaLength, dbLength));
        }
    }
}
