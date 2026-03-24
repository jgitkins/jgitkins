package io.jgitkins.web.application.mapper;

import io.jgitkins.web.application.dto.BranchSummary;
import io.jgitkins.web.application.dto.RepositoryDetailData;
import io.jgitkins.web.application.dto.RepositoryFileEntry;
import io.jgitkins.web.application.dto.RepositorySummary;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-24T23:00:08+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 18.0.2 (Amazon.com Inc.)"
)
@Component
public class RepositoryDetailDataMapperImpl implements RepositoryDetailDataMapper {

    @Override
    public RepositoryDetailData toDetail(RepositorySummary repository, List<BranchSummary> branches, List<RepositoryFileEntry> files, String namespace, String ownerSlug, String repoName, String selectedBranch, String role, boolean writable) {
        if ( repository == null && branches == null && files == null && namespace == null && ownerSlug == null && repoName == null && selectedBranch == null && role == null ) {
            return null;
        }

        RepositorySummary repository1 = null;
        repository1 = repository;
        List<BranchSummary> branches1 = null;
        List<BranchSummary> list = branches;
        if ( list != null ) {
            branches1 = new ArrayList<BranchSummary>( list );
        }
        List<RepositoryFileEntry> files1 = null;
        List<RepositoryFileEntry> list1 = files;
        if ( list1 != null ) {
            files1 = new ArrayList<RepositoryFileEntry>( list1 );
        }
        String namespace1 = null;
        namespace1 = resolveSelectedBranch( namespace );
        String ownerSlug1 = null;
        ownerSlug1 = resolveSelectedBranch( ownerSlug );
        String repoName1 = null;
        repoName1 = resolveSelectedBranch( repoName );
        String role1 = null;
        role1 = resolveSelectedBranch( role );
        boolean writable1 = false;
        writable1 = writable;

        String selectedBranch1 = resolveSelectedBranch(selectedBranch);
        String errorMessage = (String) null;

        RepositoryDetailData repositoryDetailData = new RepositoryDetailData( repository1, branches1, files1, namespace1, ownerSlug1, repoName1, selectedBranch1, role1, writable1, errorMessage );

        return repositoryDetailData;
    }
}
