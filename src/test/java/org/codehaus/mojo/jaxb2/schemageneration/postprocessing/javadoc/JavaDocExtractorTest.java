package org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc;

import org.codehaus.mojo.jaxb2.BufferingLog;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.location.ClassLocation;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.location.MethodLocation;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.location.PackageLocation;
import org.codehaus.mojo.jaxb2.shared.FileSystemUtilities;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;

/**
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 */
public class JavaDocExtractorTest {

    // Shared state
    private File javaDocBasicDir;
    private BufferingLog log;

    @Before
    public void setupSharedState() {

        log = new BufferingLog(BufferingLog.LogLevel.DEBUG);

        // Find the desired directory
        final URL dirURL = getClass().getClassLoader().getResource("testdata/schemageneration/javadoc/basic");
        this.javaDocBasicDir = new File(dirURL.getPath());
        Assert.assertTrue(javaDocBasicDir.exists() && javaDocBasicDir.isDirectory());
    }

    @Test
    public void validateLogStatementsDuringProcessing() {

        // Assemble
        final JavaDocExtractor unitUnderTest = new JavaDocExtractor(log);
        final List<File> sourceDirs = Arrays.<File>asList(javaDocBasicDir);
        final List<File> sourceFiles = FileSystemUtilities.resolveRecursively(sourceDirs, null, log);

        // Act
        unitUnderTest.addSourceFiles(sourceFiles);
        final SearchableDocumentation ignoredResult = unitUnderTest.process();

        // Assert
        final SortedMap<String, Throwable> logBuffer = log.getLogBuffer();
        final List<String> keys = new ArrayList<String>(logBuffer.keySet());

        /*
         * 000: (DEBUG) Accepted file [/Users/lj/Development/Projects/Codehaus/github_jaxb2_plugin/target/test-classes/testdata/schemageneration/javadoc/basic/NodeProcessor.java],
         * 001: (INFO) Processing [1] java sources.,
         * 002: (DEBUG) Added package-level JavaDoc for [basic],
         * 003: (DEBUG) Added class-level JavaDoc for [basic.NodeProcessor],
         * 004: (DEBUG) Added method-level JavaDoc for [basic.NodeProcessor#accept(org.w3c.dom.Node)],
         * 005: (DEBUG) Added method-level JavaDoc for [basic.NodeProcessor#process(org.w3c.dom.Node)]]
         */
        Assert.assertEquals(6, keys.size());
        Assert.assertEquals("001: (INFO) Processing [1] java sources.", keys.get(1));
        Assert.assertEquals("002: (DEBUG) Added package-level JavaDoc for [basic]", keys.get(2));
        Assert.assertEquals("003: (DEBUG) Added class-level JavaDoc for [basic.NodeProcessor]", keys.get(3));
        Assert.assertEquals("004: (DEBUG) Added method-level JavaDoc for [basic.NodeProcessor#accept(org.w3c.dom.Node)]",
                keys.get(4));
        Assert.assertEquals("005: (DEBUG) Added method-level JavaDoc for [basic.NodeProcessor#process(org.w3c.dom.Node)]",
                keys.get(5));
    }

    @Test
    public void validatePathsFromProcessing() {

        // Assemble
        final JavaDocExtractor unitUnderTest = new JavaDocExtractor(log);
        final List<File> sourceDirs = Arrays.asList(javaDocBasicDir);
        final List<File> sourceFiles = FileSystemUtilities.resolveRecursively(sourceDirs, null, log);

        // Act
        unitUnderTest.addSourceFiles(sourceFiles);
        final SearchableDocumentation result = unitUnderTest.process();

        // Assert
        final List<String> paths = new ArrayList<String>(result.getPaths());
        Assert.assertEquals(4, paths.size());
        Assert.assertEquals("basic", paths.get(0));
        Assert.assertEquals("basic.NodeProcessor", paths.get(1));
        Assert.assertEquals("basic.NodeProcessor#accept(org.w3c.dom.Node)", paths.get(2));
        Assert.assertEquals("basic.NodeProcessor#process(org.w3c.dom.Node)", paths.get(3));
    }

    @Test
    public void validateJavaDocDataFromProcessing() {

        // Assemble
        final String basicPackagePath = "basic";
        final String nodeProcessorClassPath = "basic.NodeProcessor";
        final String acceptMethodPath = "basic.NodeProcessor#accept(org.w3c.dom.Node)";
        final String processMethodPath = "basic.NodeProcessor#process(org.w3c.dom.Node)";

        final JavaDocExtractor unitUnderTest = new JavaDocExtractor(log);
        final List<File> sourceDirs = Arrays.<File>asList(javaDocBasicDir);
        final List<File> sourceFiles = FileSystemUtilities.resolveRecursively(sourceDirs, null, log);

        // Act
        unitUnderTest.addSourceFiles(sourceFiles);
        final SearchableDocumentation result = unitUnderTest.process();

        // Assert
        /*
         +=================
         | Comment:
         | No JavaDoc tags.
         +=================
         */
        final SortableLocation packageLocation = result.getLocation(basicPackagePath);
        final JavaDocData basicPackageJavaDoc = result.getJavaDoc(basicPackagePath);
        Assert.assertTrue(packageLocation instanceof PackageLocation);

        final PackageLocation castPackageLocation = (PackageLocation) packageLocation;
        Assert.assertEquals("basic", castPackageLocation.getPackageName());
        Assert.assertEquals(JavaDocData.NO_COMMENT, basicPackageJavaDoc.getComment());
        Assert.assertEquals(0, basicPackageJavaDoc.getTag2ValueMap().size());

        /*
         +=================
         | Comment: Processor/visitor pattern specification for DOM Nodes.
         | 2 JavaDoc tags ...
         | author: <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, Mr. Foo
         | see: org.w3c.dom.Node
         +=================
         */
        final SortableLocation classLocation = result.getLocation(nodeProcessorClassPath);
        final JavaDocData nodeProcessorClassJavaDoc = result.getJavaDoc(nodeProcessorClassPath);
        Assert.assertTrue(classLocation instanceof ClassLocation);

        final ClassLocation castClassLocation = (ClassLocation) classLocation;
        Assert.assertEquals("basic", castClassLocation.getPackageName());
        Assert.assertEquals("NodeProcessor", castClassLocation.getClassName());
        Assert.assertEquals("Processor/visitor pattern specification for DOM Nodes.",
                nodeProcessorClassJavaDoc.getComment());


        final SortedMap<String, String> classTag2ValueMap = nodeProcessorClassJavaDoc.getTag2ValueMap();
        Assert.assertEquals(2, classTag2ValueMap.size());
        Assert.assertEquals("org.w3c.dom.Node", classTag2ValueMap.get("see"));
        Assert.assertEquals("<a href=\"mailto:lj@jguru.se\">Lennart J&ouml;relid</a>, Mr. Foo",
                classTag2ValueMap.get("author"));

        /*
         +=================
         | Comment: Defines if this visitor should process the provided node.
         | 2 JavaDoc tags ...
         | param: aNode The DOM node to process.
         | return: <code>true</code> if the provided Node should be processed by this NodeProcessor.
         +=================
         */
        final SortableLocation acceptMethodLocation = result.getLocation(acceptMethodPath);
        final JavaDocData acceptMethodClassJavaDoc = result.getJavaDoc(acceptMethodPath);
        Assert.assertTrue(acceptMethodLocation instanceof MethodLocation);

        final MethodLocation castMethodLocation = (MethodLocation) acceptMethodLocation;
        Assert.assertEquals("basic", castMethodLocation.getPackageName());
        Assert.assertEquals("NodeProcessor", castMethodLocation.getClassName());
        Assert.assertEquals("(org.w3c.dom.Node)", castMethodLocation.getParametersAsString());
        Assert.assertEquals("Defines if this visitor should process the provided node.",
                acceptMethodClassJavaDoc.getComment());

        final SortedMap<String, String> methodTag2ValueMap = acceptMethodClassJavaDoc.getTag2ValueMap();
        Assert.assertEquals(2, methodTag2ValueMap.size());
        Assert.assertEquals("aNode The DOM node to process.", methodTag2ValueMap.get("param"));
        Assert.assertEquals("<code>true</code> if the provided Node should be processed by this NodeProcessor.",
                methodTag2ValueMap.get("return"));
    }
}
