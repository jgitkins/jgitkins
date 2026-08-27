package io.jgitkins.server.persistence.placement;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Task 2.67's namesake risk, made checkable: a mapper XML whose namespace no longer names a real
 * interface.
 *
 * <p>The namespace is a fully-qualified class name written as a string. Moving the interface without
 * editing it produces a MyBatis binding failure at context startup or first query — never a compile
 * error. That is the reason the plan says {@code compileJava} passing proves nothing about this move, and
 * it is the single most likely way a 53-file relocation ships broken.
 *
 * <p>Two checks, because a resolvable namespace is not sufficient. The class must exist, and every
 * statement id in the XML must have a method of that name on the interface. A statement whose id no
 * longer matches a method is dead SQL, and a method with no statement fails on invocation, which for a
 * mapper means the first request that reaches it.
 *
 * <p>This runs with no Spring context and no database on purpose: it is the fastest possible place for
 * this failure to surface, and it surfaces with the class and statement named.
 */
class MapperNamespaceBindingTest {

    private static final Path MAPPER_RESOURCES = Path.of("src/main/resources/mapper");
    private static final Set<String> STATEMENT_TAGS = Set.of("select", "insert", "update", "delete");

    @Test
    void everyMapperXmlNamespaceResolvesToAnExistingMapperInterface() throws Exception {
        List<Path> mappers = mapperFiles();
        assertThat(mappers)
                .as("no mapper XML was found under %s; the scan is broken, not the code", MAPPER_RESOURCES)
                .isNotEmpty();

        List<String> unresolvable = new ArrayList<>();
        Map<String, Set<String>> statementsWithoutMethods = new LinkedHashMap<>();

        for (Path mapper : mappers) {
            Document document = parse(mapper);
            String namespace = document.getDocumentElement().getAttribute("namespace");

            assertThat(namespace)
                    .as("%s declares no namespace; MyBatis cannot bind it to anything", mapper)
                    .isNotBlank();

            Class<?> mapperInterface;
            try {
                mapperInterface = Class.forName(namespace);
            } catch (ClassNotFoundException e) {
                unresolvable.add(mapper + " -> " + namespace);
                continue;
            }

            Set<String> methodNames = Arrays.stream(mapperInterface.getMethods())
                    .map(Method::getName)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

            Set<String> orphans = new LinkedHashSet<>();
            for (String statementId : statementIds(document)) {
                if (!methodNames.contains(statementId)) {
                    orphans.add(statementId);
                }
            }
            if (!orphans.isEmpty()) {
                statementsWithoutMethods.put(mapper.toString(), orphans);
            }
        }

        assertThat(unresolvable)
                .as("these mapper namespaces do not name an existing class. MyBatis fails on this at "
                        + "context startup or first query, never at compile time, so a passing build says "
                        + "nothing about it — which is exactly how a package move ships broken.")
                .isEmpty();

        assertThat(statementsWithoutMethods)
                .as("these statement ids have no method of the same name on their mapper interface. The "
                        + "SQL is unreachable, and if the intent was the other direction — a renamed "
                        + "method — the failure arrives on the first request that calls it.")
                .isEmpty();
    }

    @Test
    void everyMapperInterfaceUnderTheNewLocationIsBackedByAnXmlOrIsAnnotationDriven() throws Exception {
        // The inverse direction. A mapper interface whose XML was not moved with it, or was deleted,
        // still compiles and still gets a bean; every call on it fails at runtime.
        Set<String> namespaces = new LinkedHashSet<>();
        for (Path mapper : mapperFiles()) {
            namespaces.add(parse(mapper).getDocumentElement().getAttribute("namespace"));
        }

        List<String> withoutBacking = new ArrayList<>();
        Path root = Path.of("src/main/java/io/jgitkins/server");
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith("MbgMapper.java")
                            || p.toString().endsWith("QueryMapper.java"))
                    .sorted()
                    .toList()) {
                String fqcn = "io.jgitkins.server."
                        + root.relativize(file).toString().replace(".java", "").replace('/', '.');
                if (namespaces.contains(fqcn)) {
                    continue;
                }
                // An annotation-driven mapper needs no XML; @Select and friends carry the SQL.
                String source = Files.readString(file);
                boolean annotationDriven = source.contains("@Select")
                        || source.contains("@Insert")
                        || source.contains("@Update")
                        || source.contains("@Delete");
                if (!annotationDriven) {
                    withoutBacking.add(fqcn);
                }
            }
        }

        assertThat(withoutBacking)
                .as("these mapper interfaces have neither an XML namespace pointing at them nor inline "
                        + "SQL annotations. They compile, they get a bean, and every call on them fails "
                        + "at runtime.")
                .isEmpty();
    }

    private static List<Path> mapperFiles() throws Exception {
        try (Stream<Path> files = Files.walk(MAPPER_RESOURCES)) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".xml"))
                    .sorted()
                    .toList();
        }
    }

    private static Document parse(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // The MyBatis DTD is declared but must not be fetched: a test that needs the network fails for
        // reasons that have nothing to do with what it asserts.
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        try (InputStream in = Files.newInputStream(path)) {
            return factory.newDocumentBuilder().parse(in);
        }
    }

    private static Set<String> statementIds(Document document) {
        Set<String> ids = new LinkedHashSet<>();
        NodeList children = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && STATEMENT_TAGS.contains(node.getNodeName())) {
                String id = ((Element) node).getAttribute("id");
                if (!id.isBlank()) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }
}
