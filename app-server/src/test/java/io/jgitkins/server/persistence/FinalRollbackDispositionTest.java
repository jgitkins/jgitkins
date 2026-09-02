package io.jgitkins.server.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 * Task 2.77 rollback disposition: the assets every migrated capability rolls back to are intact.
 *
 * <p>"Retained" is a claim about the future — that a rollback will work — and the only way to keep it
 * honest is to check the retained thing is still the thing the rollback expects. A mapper XML that is
 * present but has lost a statement id, or lost its {@code FOR UPDATE}, satisfies a file-exists check and
 * fails the rollback.
 *
 * <p>The lock contract is the part that matters most. Three statements across two mappers carry
 * {@code FOR UPDATE}, and each is the MyBatis half of a guarantee whose JPA half is a
 * {@code @Lock(PESSIMISTIC_WRITE)} proven by its own test: the organization membership mutation lock, the
 * job-history compare-and-append, and the user row lock. If a rollback restored a mapper whose
 * {@code FOR UPDATE} had been dropped, the capability would come back functional and unsafe — reads would
 * succeed, and two concurrent writers would both win. That failure has no error message.
 */
class FinalRollbackDispositionTest {

    private static final Path REPOSITORY_ROOT = Paths.get("..").toAbsolutePath().normalize();

    /** Statement ids that must still emit {@code FOR UPDATE}, per mapper. */
    private static final Map<String, List<String>> LOCK_CONTRACT = Map.of(
            "app-server/src/main/resources/mapper/mbg/OrganizeEntityMbgMapper.xml",
            List.of("selectByOrganizeIdForUpdate"),
            "app-server/src/main/resources/mapper/mbg/JobHistoryEntityMbgMapper.xml",
            List.of("selectLatestHistoryForUpdate"),
            "app-server/src/main/resources/mapper/mbg/UserEntityMbgMapper.xml",
            List.of("selectByPrimaryKeyForUpdate"));

    /** Mapper namespace must still match the interface the MyBatis adapter injects. */
    private static final Map<String, String> NAMESPACE_CONTRACT = Map.of(
            "app-server/src/main/resources/mapper/mbg/OrganizeEntityMbgMapper.xml",
            "io.jgitkins.server.collaboration.adapter.out.persistence.translator.OrganizeEntityMbgMapper",
            "app-server/src/main/resources/mapper/mbg/JobHistoryEntityMbgMapper.xml",
            "io.jgitkins.server.execution.adapter.out.persistence.translator.JobHistoryEntityMbgMapper",
            "app-server/src/main/resources/mapper/mbg/UserEntityMbgMapper.xml",
            "io.jgitkins.server.identity.access.adapter.out.persistence.translator.UserEntityMbgMapper",
            "app-server/src/main/resources/mapper/custom/JobDispatchQueryMapper.xml",
            "io.jgitkins.server.execution.adapter.out.persistence.translator.JobDispatchQueryMapper");

    private static final Set<String> STATEMENT_TAGS = Set.of("select", "insert", "update", "delete");

    @Test
    void restoresAllRetainedAssets() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String asset : NAMESPACE_CONTRACT.keySet()) {
            if (!Files.exists(REPOSITORY_ROOT.resolve(asset))) {
                missing.add(asset);
            }
        }
        assertThat(missing)
                .as("a retained asset that is not on disk makes its capability's documented rollback "
                        + "impossible")
                .isEmpty();

        Map<String, String> wrongNamespace = new LinkedHashMap<>();
        for (Map.Entry<String, String> expected : NAMESPACE_CONTRACT.entrySet()) {
            String actual = documentOf(expected.getKey()).getDocumentElement().getAttribute("namespace");
            if (!expected.getValue().equals(actual)) {
                wrongNamespace.put(expected.getKey(), actual);
            }
        }
        assertThat(wrongNamespace)
                .as("a mapper whose namespace no longer names the interface the adapter injects fails at "
                        + "context startup, not at build time — so the rollback would take the "
                        + "application down rather than restore it")
                .isEmpty();
    }

    @Test
    void everyRetainedLockStatementStillEmitsForUpdate() throws Exception {
        Map<String, List<String>> unlocked = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> contract : LOCK_CONTRACT.entrySet()) {
            Document document = documentOf(contract.getKey());
            List<String> lost = new ArrayList<>();

            for (String statementId : contract.getValue()) {
                Element statement = statementById(document, statementId);
                assertThat(statement)
                        .as("statement '%s' is gone from %s; the rollback target no longer has the "
                                + "operation its adapter calls", statementId, contract.getKey())
                        .isNotNull();
                if (!textOf(statement).toUpperCase(java.util.Locale.ROOT).contains("FOR UPDATE")) {
                    lost.add(statementId);
                }
            }
            if (!lost.isEmpty()) {
                unlocked.put(contract.getKey(), lost);
            }
        }

        assertThat(unlocked)
                .as("these statements are the MyBatis half of a row-lock guarantee whose JPA half is a "
                        + "@Lock(PESSIMISTIC_WRITE). Restoring a mapper without its FOR UPDATE brings the "
                        + "capability back functional and unsafe: reads succeed, and two concurrent "
                        + "writers both win. There is no error message for that.")
                .isEmpty();
    }

    @Test
    void theLockContractCoversEveryForUpdateStatementInTheTree() throws Exception {
        // The inverse direction: a FOR UPDATE that appears in some mapper this test does not know about
        // is an unaudited lock. It would survive a rollback by luck rather than by contract.
        Map<String, List<String>> found = new LinkedHashMap<>();
        Path resources = REPOSITORY_ROOT.resolve("app-server/src/main/resources/mapper");

        try (Stream<Path> files = Files.walk(resources)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".xml")).sorted().toList()) {
                String relative = REPOSITORY_ROOT.relativize(file).toString();
                Document document = documentOf(relative);
                List<String> locking = new ArrayList<>();
                NodeList children = document.getDocumentElement().getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    if (node.getNodeType() != Node.ELEMENT_NODE
                            || !STATEMENT_TAGS.contains(node.getNodeName())) {
                        continue;
                    }
                    Element element = (Element) node;
                    if (textOf(element).toUpperCase(java.util.Locale.ROOT).contains("FOR UPDATE")) {
                        locking.add(element.getAttribute("id"));
                    }
                }
                if (!locking.isEmpty()) {
                    found.put(relative, locking);
                }
            }
        }

        assertThat(found)
                .as("every FOR UPDATE in app-server's mappers must be named in this test's LOCK_CONTRACT. "
                        + "An unlisted one is an unaudited lock: it would survive a rollback by luck "
                        + "rather than by contract, and nothing would notice if it were dropped.")
                .isEqualTo(LOCK_CONTRACT);
    }

    private static Document documentOf(String relativePath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // MyBatis mappers declare a DTD; resolving it would reach the network, and a test that needs the
        // network is a test that fails for reasons unrelated to the thing it asserts.
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        try (InputStream in = Files.newInputStream(REPOSITORY_ROOT.resolve(relativePath))) {
            return factory.newDocumentBuilder().parse(in);
        }
    }

    private static Element statementById(Document document, String id) {
        NodeList children = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE
                    && STATEMENT_TAGS.contains(node.getNodeName())
                    && id.equals(((Element) node).getAttribute("id"))) {
                return (Element) node;
            }
        }
        return null;
    }

    private static String textOf(Element element) {
        StringBuilder text = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                text.append(node.getNodeValue());
            } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                text.append(textOf((Element) node));
            }
        }
        return text.toString();
    }
}
