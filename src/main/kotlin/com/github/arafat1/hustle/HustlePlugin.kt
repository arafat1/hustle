package com.github.arafat1.hustle

import org.gradle.api.InvalidUserDataException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.*

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.resources.MissingResourceException
import org.gradle.internal.impldep.org.eclipse.jgit.errors.NotSupportedException
import java.util.*

class HustlePlugin: Plugin<Project> {
    lateinit var hustleConfig: HustlePluginExtension
    lateinit var dbKlassName: String
    lateinit var database: Database

    override fun apply(project: Project) {
        hustleConfig = project.extensions.create("hustle", HustlePluginExtension::class.java)

        with(project.task("generateScaffold")) {
            doLast {
                if (hustleConfig.dbUrl == null) throw MissingResourceException("Please provide databse Url")
                val urlFragments = hustleConfig.dbUrl!!.split(":")
                if (urlFragments.size < 4) throw InvalidUserDataException("Url provided is not correct")

                if (hustleConfig.user == null || hustleConfig.password == null)
                    throw InvalidUserDataException("Username or Password missing")

                when(urlFragments[1]) {
                    "postgresql" -> database = PostgreSql()
                    else -> throw NotSupportedException("Database not supported")
                }

                val basePackage = ("${project.group}.${project.name}")
                    .replace(HustleConst.N_BASE_PACKAGE.toRegex(), "")
                val entityPackage = "$basePackage.entity"
                val repoPackage = "$basePackage.repository"
                val baseDir: String = project.projectDir.path
                val entityPath: Path = Paths.get(baseDir, HustleConst.SOURCE_SET, entityPackage.replace('.', '/'))
                val repoPath: Path = Paths.get(baseDir, HustleConst.SOURCE_SET, repoPackage.replace('.', '/'))

                entityPath.toFile().mkdir()
                repoPath.toFile().mkdir()

                generateScaffolds(entityPackage, repoPackage, entityPath.toString(), repoPath.toString())
            }
        }
    }

    private fun generateScaffolds(entityPackage: String, repoPackage: String, entityPath: String, repoPath: String) {
        Class.forName("org.postgresql.Driver")
        val conn: Connection = DriverManager.getConnection(hustleConfig.dbUrl, hustleConfig.user, hustleConfig.password)
        val md: DatabaseMetaData = conn.metaData

        // tables
        val types = arrayOf("TABLE")
        val rs: ResultSet = md.getTables("dvdrental", null, "%", types)

        // columns
        while (rs.next()) {
            val tableName: String = rs.getString(3)
            val pk: ResultSet = md.getPrimaryKeys(null, null, tableName)
            // primary key
            var primaryKeyColumn = ""
            while (pk.next()) {
                primaryKeyColumn = pk.getString("COLUMN_NAME")
            }
            val tmd =
                TableMetaData(tableName, primaryKeyColumn)

            val columns: ResultSet = md.getColumns(null, null, tableName, null)
            while (columns.next()) {
                val columnName: String = columns.getString("COLUMN_NAME")
                val datatype: String = columns.getString("TYPE_NAME")
                val isNullable: String = columns.getString("IS_NULLABLE")
                tmd.columns.add(
                    Column(
                        columnName,
                        datatype,
                        isNullable
                    )
                )
                //println("ColumnName: $columnName, DataType: $datatype, IsNullable: $isNullable")
            }
            generateEntityCode(tmd, entityPackage, entityPath)
            generateRepositoryCode(tmd, repoPackage, repoPath, entityPackage)
        }
    }

    @Throws(SQLException::class, IOException::class)
    private fun generateEntityCode(tmd: TableMetaData, entityPackage: String, entityPath: String) {
        val className = convertToJavaName(tmd.tableName, true)
        println("Generating Entity: $className")
        val file: Path = Paths.get("$entityPath/$className.java")
        val lines: MutableList<String> = ArrayList()

        lines.add("package $entityPackage;\n")

        lines.add("import javax.persistence.Column;")
        lines.add("import javax.persistence.Entity;")
        lines.add("import javax.persistence.Id;")
        lines.add("import javax.persistence.Table;\n")

        lines.add("import java.sql.Timestamp;")
        lines.add("import java.sql.Date;")
        lines.add("import java.math.BigDecimal;\n")

        lines.add("import lombok.Data;\n")

        lines.add("@Entity")
        lines.add("@Table(name = \"${tmd.tableName.toUpperCase()}\")")
        lines.add("@Data")
        lines.add("public class ${convertToJavaName(tmd.tableName, true)} {\n")

        for (col in tmd.columns) {
            if (col.columnName == tmd.primaryKeyColumn)
                lines.add("  @Id")
            lines.add("  @Column(name = \"${col.columnName}\")")
            lines.add("  private ${database.getJavaType(col.dataType)} ${convertToJavaName(col.columnName, false)};\n")
        }

        lines.add("}")

        Files.write(file, lines, StandardCharsets.UTF_8)
    }

    @Throws(IOException::class)
    private fun generateRepositoryCode(tmd: TableMetaData, repoPackage: String, repoPath: String, entityPackage: String) {
        val className = convertToJavaName(tmd.tableName, true)
        println ("Generating Repository: ${className}Repository")
        val file: Path = Paths.get("$repoPath/${className}Repository.java")
        val lines: MutableList<String> = ArrayList()
        var primaryKeyType = ""
        for (col in tmd.columns) {
            if (col.columnName == tmd.primaryKeyColumn) primaryKeyType = database.getJavaType(col.dataType)
        }
        lines.add("package $repoPackage;\n")

        lines.add("import $entityPackage.$className;")
        lines.add("import org.springframework.data.repository.PagingAndSortingRepository;")
        lines.add("import org.springframework.data.rest.core.annotation.RepositoryRestResource;\n")

        lines.add("@RepositoryRestResource(path = \"${className}s\", collectionResourceRel = \"${className}s\")")
        lines.add("public interface ${className}Repository extends PagingAndSortingRepository<$className, $primaryKeyType> {\n}")
        Files.write(file, lines, StandardCharsets.UTF_8)
    }

    private fun convertToJavaName(tableName: String, isFirstCharCamel: Boolean): String {
        val sb: java.lang.StringBuilder = java.lang.StringBuilder()
        var isUnderscore = isFirstCharCamel
        for (element in tableName) {
            if ('_' == element) {
                isUnderscore = true
                continue
            }
            if (isUnderscore) {
                sb.append(Character.toUpperCase(element))
                isUnderscore = false
            } else {
                sb.append(element)
            }
        }
        return sb.toString().replace(HustleConst.N_VARIABLE.toRegex(), "")
    }

    private class TableMetaData(var tableName: String, var primaryKeyColumn: String) {
        var columns: MutableList<Column> = ArrayList()
    }

    private class Column(var columnName: String, var dataType: String, var isNullable: String)
}