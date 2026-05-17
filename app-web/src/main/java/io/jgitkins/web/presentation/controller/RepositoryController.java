package io.jgitkins.web.presentation.controller;

import io.jgitkins.web.application.dto.RepositoryBranchCreateResult;
import io.jgitkins.web.application.dto.RepositoryCreateContext;
import io.jgitkins.web.application.dto.RepositoryCreateResult;
import io.jgitkins.web.application.dto.RepositoryDetailData;
import io.jgitkins.web.application.dto.RepositoryFileIndexEntry;
import io.jgitkins.web.application.port.in.facade.RepositoryFacadeUseCase;
import io.jgitkins.web.presentation.dto.RepositoryCreateForm;
import io.jgitkins.web.presentation.support.RepositoryTreePathSupport;
import io.jgitkins.web.presentation.support.RepositoryUserProfile;
import io.jgitkins.web.presentation.support.RepositoryUserProfileResolver;
import io.jgitkins.web.presentation.support.RepositoryViewSupport;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class RepositoryController {

	private final RepositoryFacadeUseCase repositoryFacadeUseCase;
	private final RepositoryUserProfileResolver userProfileResolver;
	private final RepositoryViewSupport repositoryViewSupport;
	private final RepositoryTreePathSupport treePathSupport;

	@GetMapping("/repositories/new")
	public String newRepository(Authentication authentication, Model model) {
		RepositoryCreateForm form = new RepositoryCreateForm();
		RepositoryUserProfile profile = userProfileResolver.resolve(authentication);
		RepositoryCreateContext context = repositoryFacadeUseCase.getInitData(profile, form.getOwnerType(),
				form.getOrganizeId());
		repositoryViewSupport.populateCreateModel(model, context, form, null);
		return "repositories/new";
	}

	@PostMapping("/repositories")
	public String createRepository(@Valid @ModelAttribute("form") RepositoryCreateForm form,
			BindingResult bindingResult,
			Authentication authentication,
			Model model) {
		RepositoryUserProfile profile = userProfileResolver.resolve(authentication);
		RepositoryCreateContext context = repositoryFacadeUseCase.getInitData(profile, form.getOwnerType(),
				form.getOrganizeId());

		String validationError = resolveValidationError(bindingResult);
		if (validationError == null) {
			validationError = repositoryViewSupport.validateForm(form);
		}
		if (validationError != null) {
			repositoryViewSupport.populateCreateModel(model, context, form, validationError);
			return "repositories/new";
		}

		RepositoryCreateResult result = repositoryFacadeUseCase
				.createRepository(repositoryViewSupport.toRequest(form, profile));
		if (result.errorMessage() != null) {
			repositoryViewSupport.populateCreateModel(model, context, form, result.errorMessage());
			return "repositories/new";
		}

		return "redirect:/";
	}

	@GetMapping("/{namespace}/{repoName}")
	public String repositoryDetailPage(@PathVariable("namespace") String namespace,
			@PathVariable("repoName") String repoName,
			@RequestParam(name = "branch", required = false) String branch,
			Authentication authentication,
			Model model) {
		RepositoryDetailData detail = repositoryFacadeUseCase.getRepositoryDetail(namespace, repoName, branch, "",
				userProfileResolver.isAuthenticated(authentication));
		if (detail == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		repositoryViewSupport.populateDetailModel(model, namespace, repoName, "", detail);
		return "repositories/detail";
	}

	@GetMapping({ "/{namespace}/{repoName}/tree", "/{namespace}/{repoName}/tree/**" })
	public String repositoryTreePage(@PathVariable("namespace") String namespace,
			@PathVariable("repoName") String repoName,
			@RequestParam(name = "branch", required = false) String branch,
			Authentication authentication,
			HttpServletRequest request,
			Model model) {
		String directory = treePathSupport.resolveTreeDirectory(request);
		RepositoryDetailData detail = repositoryFacadeUseCase.getRepositoryDetail(namespace, repoName, branch,
				directory, userProfileResolver.isAuthenticated(authentication));
		if (detail == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		repositoryViewSupport.populateDetailModel(model, namespace, repoName, directory, detail);
		return "repositories/detail";
	}

	@GetMapping("/{namespace}/{repoName}/find-files/index")
	@ResponseBody
	public ResponseEntity<List<RepositoryFileIndexEntry>> findFileIndex(@PathVariable("namespace") String namespace,
			@PathVariable("repoName") String repoName,
			@RequestParam(name = "branch", required = false) String branch) {
		List<RepositoryFileIndexEntry> files = repositoryFacadeUseCase.getFileIndex(namespace, repoName, branch);
		return ResponseEntity.ok(files);
	}

	@PostMapping("/{namespace}/{repoName}/branches")
	public String createBranch(@PathVariable("namespace") String namespace,
			@PathVariable("repoName") String repoName,
			@RequestParam("branchName") String branchName,
			@RequestParam(name = "sourceBranch", required = false) String sourceBranch,
			@RequestParam(name = "currentPath", required = false) String currentPath,
			@RequestParam(name = "currentBranch", required = false) String currentBranch,
			RedirectAttributes redirectAttributes) {
		RepositoryBranchCreateResult result = repositoryFacadeUseCase.createBranch(namespace, repoName, branchName,
				sourceBranch, currentBranch);
		if (result.errorMessage() != null) {
			redirectAttributes.addFlashAttribute("branchError", result.errorMessage());
			return "redirect:" + buildRepositoryRedirect(namespace, repoName, currentPath, currentBranch);
		}

		String selectedBranch = result.branch() != null
				&& org.springframework.util.StringUtils.hasText(result.branch().name())
						? result.branch().name()
						: branchName;
		redirectAttributes.addFlashAttribute("branchSuccess",
				repositoryViewSupport.getMessage("success.branch.created"));
		return "redirect:" + buildRepositoryRedirect(namespace, repoName, currentPath, selectedBranch);
	}

	@PostMapping("/{namespace}/{repoName}/files")
	public String uploadFile(@PathVariable("namespace") String namespace,
			@PathVariable("repoName") String repoName,
			@RequestParam("branch") String branch,
			@RequestParam("path") String path,
			@RequestParam("message") String message,
			@RequestParam("file") MultipartFile file,
			@RequestParam(name = "currentPath", required = false) String currentPath,
			RedirectAttributes redirectAttributes) {
		var result = repositoryFacadeUseCase.uploadFile(namespace, repoName, branch, path, message, file);
		if (result.errorMessage() != null) {
			redirectAttributes.addFlashAttribute("fileError", result.errorMessage());
			return "redirect:" + buildRepositoryRedirect(namespace, repoName, currentPath, branch);
		}

		redirectAttributes.addFlashAttribute("fileSuccess", repositoryViewSupport.getMessage("success.file.uploaded"));
		return "redirect:" + buildRepositoryRedirect(namespace, repoName, currentPath, branch);
	}

	@PostMapping("/{namespace}/{repoName}/directories")
	public String createDirectory(@PathVariable("namespace") String namespace,
			@PathVariable("repoName") String repoName,
			@RequestParam("branch") String branch,
			@RequestParam("directoryPath") String directoryPath,
			@RequestParam("message") String message,
			@RequestParam(name = "currentPath", required = false) String currentPath,
			RedirectAttributes redirectAttributes) {
		var result = repositoryFacadeUseCase.createDirectory(namespace, repoName, branch, directoryPath, message);
		if (result.errorMessage() != null) {
			redirectAttributes.addFlashAttribute("directoryError", result.errorMessage());
			return "redirect:" + buildRepositoryRedirect(namespace, repoName, currentPath, branch);
		}

		redirectAttributes.addFlashAttribute("directorySuccess",
				repositoryViewSupport.getMessage("success.directory.created"));
		return "redirect:" + buildRepositoryRedirect(namespace, repoName, currentPath, branch);
	}

	private String resolveValidationError(BindingResult bindingResult) {
		if (bindingResult == null || !bindingResult.hasErrors()) {
			return null;
		}
		if (bindingResult.getFieldError() != null) {
			return bindingResult.getFieldError().getDefaultMessage();
		}
		return repositoryViewSupport.getMessage("error.request.invalid");
	}

	private String buildRepositoryRedirect(String namespace, String repoName, String currentPath, String branch) {
		StringBuilder builder = new StringBuilder();
		if (org.springframework.util.StringUtils.hasText(currentPath)) {
			builder.append("/").append(namespace).append("/").append(repoName).append("/tree/")
					.append(currentPath.trim());
		} else {
			builder.append("/").append(namespace).append("/").append(repoName);
		}
		if (org.springframework.util.StringUtils.hasText(branch)) {
			builder.append("?branch=").append(branch.trim());
		}
		return builder.toString();
	}
}

