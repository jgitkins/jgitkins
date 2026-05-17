package io.jgitkins.web.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.web.application.dto.RepositoryBranchCreateResult;
import io.jgitkins.web.application.dto.RepositoryCreateResult;
import io.jgitkins.web.application.dto.RepositoryFileIndexEntry;
import io.jgitkins.web.application.dto.RepositoryCreateContext;
import io.jgitkins.web.application.port.in.facade.RepositoryFacadeUseCase;
import io.jgitkins.web.presentation.dto.RepositoryCreateForm;
import io.jgitkins.web.presentation.support.RepositoryTreePathSupport;
import io.jgitkins.web.presentation.support.RepositoryUserProfile;
import io.jgitkins.web.presentation.support.RepositoryUserProfileResolver;
import io.jgitkins.web.presentation.support.RepositoryViewSupport;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@ExtendWith(MockitoExtension.class)
class RepositoryControllerTest {

    @Mock
    private RepositoryFacadeUseCase repositoryFacadeUseCase;
    @Mock
    private RepositoryUserProfileResolver userProfileResolver;
    @Mock
    private RepositoryViewSupport repositoryViewSupport;
    @Mock
    private RepositoryTreePathSupport treePathSupport;

    private RepositoryController controller;

    @BeforeEach
    void setUp() {
        controller = new RepositoryController(
                repositoryFacadeUseCase,
                userProfileResolver,
                repositoryViewSupport,
                treePathSupport);
        RepositoryUserProfile profile = new RepositoryUserProfile("alzar", "a@b.c");
        Mockito.lenient().when(userProfileResolver.resolve(any()))
                .thenReturn(profile);
        Mockito.lenient().when(repositoryFacadeUseCase.getInitData(any(), any(), any()))
                .thenReturn(new RepositoryCreateContext(List.of(), null, profile, "alzar", "alzar"));
    }

    @Test
    void createRepository_returnsFormViewWhenBindingHasErrors() {
        RepositoryCreateForm form = new RepositoryCreateForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("repoName", "NotBlank", "Repository name is required.");
        Model model = new ConcurrentModel();

        String view = controller.createRepository(form, bindingResult, null, model);

        assertEquals("repositories/new", view);
        verify(repositoryViewSupport).populateCreateModel(any(), any(), any(), any());
        verify(repositoryFacadeUseCase, never()).createRepository(any());
    }

    @Test
    void createRepository_createsWhenValidationPasses() {
        RepositoryCreateForm form = new RepositoryCreateForm();
        form.setRepoName("demo");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        Model model = new ConcurrentModel();
        when(repositoryViewSupport.validateForm(form)).thenReturn(null);
        when(repositoryViewSupport.toRequest(any(), any())).thenReturn(null);
        when(repositoryFacadeUseCase.createRepository(any())).thenReturn(new RepositoryCreateResult(null, null));

        String view = controller.createRepository(form, bindingResult, null, model);

        assertEquals("redirect:/", view);
        verify(repositoryFacadeUseCase).createRepository(any());
    }

    @Test
    void createBranch_redirectsToNewBranchOnSuccess() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(repositoryFacadeUseCase.createBranch(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new RepositoryBranchCreateResult(
                        new io.jgitkins.web.application.dto.BranchSummary(1L, "feature/x", false, false, false), null));

        String view = controller.createBranch("team", "demo", "feature/x", "main", "src", "main", redirect);

        assertEquals("redirect:/team/demo/tree/src?branch=feature/x", view);
    }

    @Test
    void uploadFile_redirectsWithErrorWhenUploadFails() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        MockMultipartFile file = new MockMultipartFile("file", "README.md", "text/markdown", "# hi".getBytes());
        when(repositoryFacadeUseCase.uploadFile(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(new io.jgitkins.web.application.dto.RepositoryCreateResult(null, "failed"));

        String view = controller.uploadFile("team", "demo", "main", "README.md", "add", file, "", redirect);

        assertEquals("redirect:/team/demo?branch=main", view);
        assertEquals("failed", redirect.getFlashAttributes().get("fileError"));
    }

    @Test
    void findFileIndex_returnsIndexList() {
        when(repositoryFacadeUseCase.getFileIndex("team", "demo", "main"))
                .thenReturn(List.of(new RepositoryFileIndexEntry("README.md", "README.md", "blob")));

        var response = controller.findFileIndex("team", "demo", "main");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("README.md", response.getBody().get(0).name());
    }
}
