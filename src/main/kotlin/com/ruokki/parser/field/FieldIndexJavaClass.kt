package com.ruokki.parser.field

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.ruokki.parser.CLASS
import com.ruokki.parser.FIELDNAME
import com.ruokki.parser.FIELDTYPE
import com.ruokki.parser.METHODNAME
import com.ruokki.parser.log.Loggable
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.TextField
import java.io.File

class FieldIndexJavaClass : FieldIndexer, Loggable {
    override fun addIndex(file: File, document: Document) {
        logger().debug { "Indexing class file ${file.name}" }
        val compilationUnit = StaticJavaParser.parse(file)
        compilationUnit.findAll(ClassOrInterfaceDeclaration::class.java).forEach { classOrInterfaceDeclaration ->
            this.indexJavaClassToDocument(
                classOrInterfaceDeclaration,
                document
            )
        }
    }

    private fun indexJavaClassToDocument(clazz: ClassOrInterfaceDeclaration, document: Document) {
        logger().debug { "Indexing class ${clazz.nameAsString}" }
        document.add(TextField(CLASS, clazz.nameAsString, Field.Store.YES))
        clazz.findAll(FieldDeclaration::class.java)
            .forEach { fieldDeclaration: FieldDeclaration -> this.indexJavaFieldToDocument(fieldDeclaration, document) }
        clazz.findAll(MethodDeclaration::class.java)
            .forEach { methodDeclaration: MethodDeclaration ->
                this.indexJavaMethodToDocument(
                    methodDeclaration,
                    document
                )
            }
    }

    private fun indexJavaFieldToDocument(fieldDeclaration: FieldDeclaration, document: Document) {
        val variable = fieldDeclaration.findFirst(VariableDeclarator::class.java).get().name.asString()
        logger().debug { "Indexing Field ${variable}" }
        document.add(TextField(FIELDNAME, variable, Field.Store.YES))
        document.add(TextField(FIELDTYPE, fieldDeclaration.commonType.asString(), Field.Store.YES))
    }

    private fun indexJavaMethodToDocument(methodDeclaration: MethodDeclaration, document: Document) {
        val variable = methodDeclaration.nameAsString
        logger().debug { "Indexing method ${variable}" }
        document.add(TextField(METHODNAME, variable, Field.Store.YES))
    }
}