package io.jgitkins.server.architecture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The import scanner the task 2.66 guardrails share, plus the categories they forbid.
 *
 * <p>One scanner, one category list. Four guardrails each rolling their own regexes would drift, and the
 * drift would be silent: a category that stopped matching would simply report no violations, which is
 * indistinguishable from a clean tree.
 *
 * <p><strong>Comments are stripped before matching.</strong> Several guarded classes carry a javadoc line
 * naming the technology they deliberately do not use. That is documentation of a boundary, not a breach
 * of it, and a scanner that flagged it would make deleting the explanation the cheapest way to go green.
 */
final class ArchitectureScanner {

    /** A forbidden category: a stable name, the pattern, and why it is forbidden. */
    record Category(String name, Pattern pattern, String reason) {
    }

    /** A single finding: which file, which category, which line. */
    record Violation(Path file, String category, String line) {
        @Override
        public String toString() {
            return file.getFileName() + " -> " + category + ": " + line.trim();
        }
    }

    static final Category FORBIDDEN_SPRING = new Category("FORBIDDEN_SPRING",
            Pattern.compile("^import\\s+org\\.springframework\\.(?!security\\.|data\\.)"),
            "Spring types make a domain class depend on the container that hosts it");
    static final Category FORBIDDEN_JPA = new Category("FORBIDDEN_JPA",
            Pattern.compile("^import\\s+jakarta\\.persistence\\."),
            "a JPA type in the domain ties the model to one persistence provider");
    static final Category FORBIDDEN_SERVLET = new Category("FORBIDDEN_SERVLET",
            Pattern.compile("^import\\s+jakarta\\.servlet\\."),
            "servlet types are transport; past the inbound adapter they are a leak");
    static final Category FORBIDDEN_MYBATIS = new Category("FORBIDDEN_MYBATIS",
            Pattern.compile("^import\\s+(org\\.mybatis\\.|org\\.apache\\.ibatis\\.)"),
            "MyBatis types belong to the outbound adapter");
    /**
     * The security <em>context</em>, not the whole security library.
     *
     * <p>Narrowed after the first run flagged {@code org.springframework.security.crypto.password
     * .PasswordEncoder} in {@code UserCredentialService}. Hashing a password is not an ambient actor —
     * it is a computation on a value the caller supplied — and this task guards against decisions that
     * discover who the caller is, not against every type in the security artifact. Matching the whole
     * package would have made the category's name a lie and invited an allowlist entry to paper over it.
     */
    static final Category FORBIDDEN_SECURITY_CONTEXT = new Category("FORBIDDEN_SECURITY_CONTEXT",
            Pattern.compile("^import\\s+org\\.springframework\\.security\\.(core|web|authentication)\\."
                    + "|SecurityContextHolder"),
            "reading the security context is how a decision silently acquires an ambient actor");
    static final Category FORBIDDEN_SPRING_DATA = new Category("FORBIDDEN_SPRING_DATA",
            Pattern.compile("^import\\s+org\\.springframework\\.data\\."),
            "Spring Data types belong to the outbound adapter");
    static final Category FORBIDDEN_JGIT = new Category("FORBIDDEN_JGIT",
            Pattern.compile("^import\\s+org\\.eclipse\\.jgit\\."),
            "JGit is an outbound technology, not a domain concept");
    static final Category FORBIDDEN_JWT = new Category("FORBIDDEN_JWT",
            Pattern.compile("^import\\s+io\\.jsonwebtoken\\."),
            "JWT types are transport credentials, not business inputs");
    static final Category FORBIDDEN_PERSISTENCE_ADAPTER = new Category("FORBIDDEN_PERSISTENCE_ADAPTER",
            Pattern.compile("^import\\s+io\\.jgitkins\\.server\\..*\\.adapter\\.out\\.persistence\\."),
            "the domain must not name the adapter that stores it");
    static final Category FORBIDDEN_CURRENT_USER = new Category("FORBIDDEN_CURRENT_USER",
            Pattern.compile("CurrentUserPort"),
            "an ambient current-user port; tasks 2.63-2.65 made the requester an argument");
    static final Category FORBIDDEN_REPOSITORY_ACTOR = new Category("FORBIDDEN_REPOSITORY_ACTOR",
            Pattern.compile("RepositoryActorPort"),
            "an ambient actor port; tasks 2.64-2.65 made the requester an argument");
    static final Category FORBIDDEN_PRINCIPAL = new Category("FORBIDDEN_PRINCIPAL",
            Pattern.compile("^import\\s+java\\.security\\.Principal"),
            "the transport principal stops at the inbound adapter");
    static final Category FORBIDDEN_MULTIPART = new Category("FORBIDDEN_MULTIPART",
            Pattern.compile("MultipartFile"),
            "a multipart upload is a transport shape; only the allowlisted inbound port may name it");
    /**
     * A foreign <em>bounded context's</em> aggregate.
     *
     * <p>{@code io.jgitkins.server.shared} is excluded: it is the shared kernel, and every aggregate in
     * every context extends {@code AbstractAggregateRoot} from it. Without the exclusion this category
     * would forbid the base class the rule depends on — which the first run duly reported for five
     * aggregates at once, a shape that reads as "the rule is wrong" rather than "the code is wrong".
     */
    static final Category FORBIDDEN_FOREIGN_AGGREGATE = new Category("FORBIDDEN_FOREIGN_AGGREGATE",
            Pattern.compile("^import\\s+io\\.jgitkins\\.server\\.(?!shared\\.)"
                    + "\\w+(\\.\\w+)*\\.domain\\.(aggregate|entity)\\."),
            "a foreign aggregate crossing a context boundary makes one context own another's lifecycle");

    /**
     * A foreign bounded context's <em>persistence</em> package.
     *
     * <p>The sibling rule {@link #FORBIDDEN_FOREIGN_AGGREGATE} forbids holding another context's
     * aggregate, on the grounds that it makes one context own invariants it does not enforce. Reaching
     * into another context's persistence adapter is the same failure with less protection: a generated
     * MBG mapper or a JPA entity carries no invariants at all, so the coupling is to the other
     * context's <em>table shape</em>, and that context can no longer change its own storage without
     * breaking this one.
     *
     * <p>It also entangles the persistence selector. An adapter that names
     * {@code OrganizeJpaRepository} keeps reading collaboration's data through JPA even after
     * collaboration is switched to MyBatis, so the two contexts can be configured to disagree about
     * which provider owns a table.
     *
     * <p>Not excluded for {@code adapter/out/acl} the way the aggregate rule is. An ACL translates
     * through the other context's application port -- that is what the four adapters in
     * {@code repository/adapter/out/acl} already do -- and none of them needs a foreign mapper. There
     * is no layer where this import is the right answer, so there is no directory to exempt.
     *
     * <p>Own-context imports match this pattern too, so ownership is decided at the call site, where
     * the owner is known. Same shape as the aggregate rule.
     */
    static final Category FORBIDDEN_FOREIGN_PERSISTENCE = new Category("FORBIDDEN_FOREIGN_PERSISTENCE",
            Pattern.compile("^import\\s+io\\.jgitkins\\.server\\.(?!shared\\.)"
                    + "\\w+(\\.\\w+)*\\.adapter\\.out\\.persistence\\."),
            "reaching into another context's persistence couples this one to that one's table shape");

    private ArchitectureScanner() {
    }

    /** Scans one file and returns every (category, line) hit, comments removed first. */
    static List<Violation> scan(Path file, List<Category> categories) throws IOException {
        List<Violation> violations = new ArrayList<>();
        for (String line : stripComments(Files.readString(file)).lines().toList()) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            for (Category category : categories) {
                if (category.pattern().matcher(trimmed).find()) {
                    violations.add(new Violation(file, category.name(), trimmed));
                }
            }
        }
        return violations;
    }

    /** Scans every {@code .java} file under the given roots. */
    static List<Violation> scanTree(List<Path> roots, List<Category> categories) throws IOException {
        List<Violation> violations = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .sorted()
                        .toList()) {
                    violations.addAll(scan(file, categories));
                }
            }
        }
        return violations;
    }

    /** Groups findings by category, for assertions that count per category. */
    static Map<String, List<Violation>> byCategory(List<Violation> violations) {
        Map<String, List<Violation>> grouped = new LinkedHashMap<>();
        for (Violation violation : violations) {
            grouped.computeIfAbsent(violation.category(), key -> new ArrayList<>()).add(violation);
        }
        return grouped;
    }

    static Path mainRoot() {
        Path local = Path.of("src/main/java/io/jgitkins/server");
        return Files.isDirectory(local) ? local : Path.of("app-server/src/main/java/io/jgitkins/server");
    }

    static Path negativeFixtures() {
        Path local = Path.of("src/test/resources/architecture/negative");
        return Files.isDirectory(local)
                ? local
                : Path.of("app-server/src/test/resources/architecture/negative");
    }

    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }
}
