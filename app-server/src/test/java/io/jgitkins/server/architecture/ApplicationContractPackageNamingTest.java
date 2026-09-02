package io.jgitkins.server.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Every bounded context names its use-case input/output package {@code contract}, and none names it
 * {@code dto}.
 *
 * <p>Before this, three contexts used {@code dto}, two used {@code contract}, and three carried both
 * at once. The question "where does a command go" had a different answer per context, so every new
 * file re-decided it and every review re-argued it.
 *
 * <p>{@code contract} won because the layer holds the input and output of a use case, not something
 * to serialise. The name is now the rule everywhere rather than a rule for this layer only: the
 * adapter side was {@code adapter/in/rest/dto} when this guard was written and is
 * {@code adapter/in/rest/contract} today, so {@code dto} names nothing in the tree. An earlier
 * revision of this javadoc argued the opposite -- that the adapter keeping {@code dto} was what let
 * a reader tell the two apart at the import. It does not hold up against the import lines
 * themselves: {@code application.contract.command.OrganizeCreationCommand} and
 * {@code adapter.in.rest.contract.request.OrganizeCreationRequest} are separated by the layer
 * segment and by the leaf, in all twenty files that import both.
 *
 * <p><strong>This does not assert the contexts have identical package sets.</strong> They do not, and
 * should not: {@code policy}, {@code support} and {@code validate} appear only where a context has
 * that responsibility, and requiring all five to match would force empty packages into existence to
 * satisfy a test. The invariant is about the one package whose name was inconsistent.
 */
class ApplicationContractPackageNamingTest {

    private static final List<String> CONTEXTS =
            List.of("collaboration", "change/review", "identity/access", "repository", "execution");

    /**
     * What the rule decides, over a set of context-relative package paths.
     *
     * <p>Pure on purpose. The real check feeds it the directories on disk; the negative control feeds
     * it a context that kept {@code dto}. Both run the same code, so the control proves the rule
     * fires rather than proving a copy of it does.
     */
    private static Survey survey(List<String> applicationSubpackagePaths) {
        TreeSet<String> withContract = new TreeSet<>();
        TreeSet<String> withDto = new TreeSet<>();
        TreeSet<String> contexts = new TreeSet<>();
        for (String path : applicationSubpackagePaths) {
            int marker = path.indexOf("/application/");
            if (marker < 0) {
                continue;
            }
            String context = path.substring(0, marker);
            String subpackage = path.substring(marker + "/application/".length());
            contexts.add(context);
            if (subpackage.equals("contract")) {
                withContract.add(context);
            } else if (subpackage.equals("dto")) {
                withDto.add(context);
            }
        }
        TreeSet<String> missingContract = new TreeSet<>(contexts);
        missingContract.removeAll(withContract);
        return new Survey(contexts, missingContract, withDto);
    }

    private record Survey(TreeSet<String> contexts, TreeSet<String> missingContract,
                          TreeSet<String> stillCallingItDto) {
    }

    @Test
    void everyContextNamesItsUseCaseContractPackageContract() {
        Survey survey = survey(applicationSubpackagesOnDisk());

        assertThat(survey.contexts())
                .as("no context application directory was found, which means the source root did not "
                        + "resolve -- every assertion below would then pass on an empty survey")
                .hasSize(CONTEXTS.size());

        assertThat(survey.stillCallingItDto())
                .as("application/dto is the old name for this layer. dto now names nothing in the "
                        + "tree -- the adapter side is adapter/in/rest/contract. Use "
                        + "application/contract.")
                .isEmpty();

        assertThat(survey.missingContract())
                .as("a context whose application layer has no contract package either has no use "
                        + "cases -- unlikely -- or is holding its commands and results somewhere the "
                        + "convention does not name")
                .isEmpty();
    }

    @Test
    void theRuleFiresOnAContextThatKeptDto() {
        Survey survey = survey(List.of(
                "collaboration/application/dto",
                "collaboration/application/service",
                "repository/application/contract"));

        assertThat(survey.stillCallingItDto())
                .as("if the rule cannot spot a dto package here, its emptiness on the real tree "
                        + "proves nothing")
                .containsExactly("collaboration");
        assertThat(survey.missingContract())
                .as("collaboration has no contract package in this input, so the second half of the "
                        + "rule must report it too")
                .containsExactly("collaboration");
    }

    private static List<String> applicationSubpackagesOnDisk() {
        Path root = ArchitectureScanner.mainRoot();
        return CONTEXTS.stream()
                .map(context -> root.resolve(context).resolve("application"))
                .filter(Files::isDirectory)
                .flatMap(application -> {
                    Path relative = root.relativize(application);
                    try (var entries = Files.list(application)) {
                        return entries.filter(Files::isDirectory)
                                .map(directory -> relative + "/" + directory.getFileName())
                                .toList()
                                .stream();
                    } catch (java.io.IOException unreadable) {
                        throw new IllegalStateException("unreadable application package: " + application,
                                unreadable);
                    }
                })
                .toList();
    }
}
