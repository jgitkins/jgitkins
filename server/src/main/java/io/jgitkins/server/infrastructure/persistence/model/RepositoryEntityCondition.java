package io.jgitkins.server.infrastructure.persistence.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RepositoryEntityCondition {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public RepositoryEntityCondition() {
        oredCriteria = new ArrayList<>();
    }

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    public void clear() {
        oredCriteria.clear();
        orderByClause = null;
        distinct = false;
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() {
            super();
            criteria = new ArrayList<>();
        }

        public boolean isValid() {
            return criteria.size() > 0;
        }

        public List<Criterion> getAllCriteria() {
            return criteria;
        }

        public List<Criterion> getCriteria() {
            return criteria;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria andIdIsNull() {
            addCriterion("ID is null");
            return (Criteria) this;
        }

        public Criteria andIdIsNotNull() {
            addCriterion("ID is not null");
            return (Criteria) this;
        }

        public Criteria andIdEqualTo(Long value) {
            addCriterion("ID =", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotEqualTo(Long value) {
            addCriterion("ID <>", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThan(Long value) {
            addCriterion("ID >", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThanOrEqualTo(Long value) {
            addCriterion("ID >=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThan(Long value) {
            addCriterion("ID <", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThanOrEqualTo(Long value) {
            addCriterion("ID <=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdIn(List<Long> values) {
            addCriterion("ID in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotIn(List<Long> values) {
            addCriterion("ID not in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdBetween(Long value1, Long value2) {
            addCriterion("ID between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotBetween(Long value1, Long value2) {
            addCriterion("ID not between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andNameIsNull() {
            addCriterion("NAME is null");
            return (Criteria) this;
        }

        public Criteria andNameIsNotNull() {
            addCriterion("NAME is not null");
            return (Criteria) this;
        }

        public Criteria andNameEqualTo(String value) {
            addCriterion("NAME =", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotEqualTo(String value) {
            addCriterion("NAME <>", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameGreaterThan(String value) {
            addCriterion("NAME >", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameGreaterThanOrEqualTo(String value) {
            addCriterion("NAME >=", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLessThan(String value) {
            addCriterion("NAME <", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLessThanOrEqualTo(String value) {
            addCriterion("NAME <=", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLike(String value) {
            addCriterion("NAME like", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotLike(String value) {
            addCriterion("NAME not like", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameIn(List<String> values) {
            addCriterion("NAME in", values, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotIn(List<String> values) {
            addCriterion("NAME not in", values, "name");
            return (Criteria) this;
        }

        public Criteria andNameBetween(String value1, String value2) {
            addCriterion("NAME between", value1, value2, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotBetween(String value1, String value2) {
            addCriterion("NAME not between", value1, value2, "name");
            return (Criteria) this;
        }

        public Criteria andPathIsNull() {
            addCriterion("PATH is null");
            return (Criteria) this;
        }

        public Criteria andPathIsNotNull() {
            addCriterion("PATH is not null");
            return (Criteria) this;
        }

        public Criteria andPathEqualTo(String value) {
            addCriterion("PATH =", value, "path");
            return (Criteria) this;
        }

        public Criteria andPathNotEqualTo(String value) {
            addCriterion("PATH <>", value, "path");
            return (Criteria) this;
        }

        public Criteria andPathGreaterThan(String value) {
            addCriterion("PATH >", value, "path");
            return (Criteria) this;
        }

        public Criteria andPathGreaterThanOrEqualTo(String value) {
            addCriterion("PATH >=", value, "path");
            return (Criteria) this;
        }

        public Criteria andPathLessThan(String value) {
            addCriterion("PATH <", value, "path");
            return (Criteria) this;
        }

        public Criteria andPathLessThanOrEqualTo(String value) {
            addCriterion("PATH <=", value, "path");
            return (Criteria) this;
        }

        public Criteria andPathLike(String value) {
            addCriterion("PATH like", value, "path");
            return (Criteria) this;
        }

        public Criteria andPathNotLike(String value) {
            addCriterion("PATH not like", value, "path");
            return (Criteria) this;
        }

        public Criteria andPathIn(List<String> values) {
            addCriterion("PATH in", values, "path");
            return (Criteria) this;
        }

        public Criteria andPathNotIn(List<String> values) {
            addCriterion("PATH not in", values, "path");
            return (Criteria) this;
        }

        public Criteria andPathBetween(String value1, String value2) {
            addCriterion("PATH between", value1, value2, "path");
            return (Criteria) this;
        }

        public Criteria andPathNotBetween(String value1, String value2) {
            addCriterion("PATH not between", value1, value2, "path");
            return (Criteria) this;
        }

        public Criteria andRepositoryTypeIsNull() {
            addCriterion("REPOSITORY_TYPE is null");
            return (Criteria) this;
        }

        public Criteria andRepositoryTypeIsNotNull() {
            addCriterion("REPOSITORY_TYPE is not null");
            return (Criteria) this;
        }

        public Criteria andRepositoryTypeEqualTo(String value) {
            addCriterion("REPOSITORY_TYPE =", value, "repositoryType");
            return (Criteria) this;
        }

        public Criteria andRepositoryTypeNotEqualTo(String value) {
            addCriterion("REPOSITORY_TYPE <>", value, "repositoryType");
            return (Criteria) this;
        }

        public Criteria andRepositoryTypeGreaterThan(String value) {
            addCriterion("REPOSITORY_TYPE >", value, "repositoryType");
            return (Criteria) this;
        }

        public Criteria andRepositoryTypeGreaterThanOrEqualTo(String value) {
            addCriterion("REPOSITORY_TYPE >=", value, "repositoryType");
            return (Criteria) this;
        }

        public Criteria andRepositoryTypeLessThan(String value) {
            addCriterion("REPOSITORY_TYPE <", value, "repositoryType");
            return (Criteria) this;
        }

        public Criteria andRepositoryTypeLessThanOrEqualTo(String value) {
            addCriterion("REPOSITORY_TYPE <=", value, "repositoryType");
            return (Criteria) this;
        }

        public Criteria andRepositoryTypeLike(String value) {
            addCriterion("REPOSITORY_TYPE like", value, "repositoryType");
            return (Criteria) this;
        }

        public Criteria andRepositoryTypeNotLike(String value) {
            addCriterion("REPOSITORY_TYPE not like", value, "repositoryType");
            return (Criteria) this;
        }

        public Criteria andRepositoryTypeIn(List<String> values) {
            addCriterion("REPOSITORY_TYPE in", values, "repositoryType");
            return (Criteria) this;
        }

        public Criteria andRepositoryTypeNotIn(List<String> values) {
            addCriterion("REPOSITORY_TYPE not in", values, "repositoryType");
            return (Criteria) this;
        }

        public Criteria andRepositoryTypeBetween(String value1, String value2) {
            addCriterion("REPOSITORY_TYPE between", value1, value2, "repositoryType");
            return (Criteria) this;
        }

        public Criteria andRepositoryTypeNotBetween(String value1, String value2) {
            addCriterion("REPOSITORY_TYPE not between", value1, value2, "repositoryType");
            return (Criteria) this;
        }

        public Criteria andOwnerTypeIsNull() {
            addCriterion("OWNER_TYPE is null");
            return (Criteria) this;
        }

        public Criteria andOwnerTypeIsNotNull() {
            addCriterion("OWNER_TYPE is not null");
            return (Criteria) this;
        }

        public Criteria andOwnerTypeEqualTo(String value) {
            addCriterion("OWNER_TYPE =", value, "ownerType");
            return (Criteria) this;
        }

        public Criteria andOwnerTypeNotEqualTo(String value) {
            addCriterion("OWNER_TYPE <>", value, "ownerType");
            return (Criteria) this;
        }

        public Criteria andOwnerTypeGreaterThan(String value) {
            addCriterion("OWNER_TYPE >", value, "ownerType");
            return (Criteria) this;
        }

        public Criteria andOwnerTypeGreaterThanOrEqualTo(String value) {
            addCriterion("OWNER_TYPE >=", value, "ownerType");
            return (Criteria) this;
        }

        public Criteria andOwnerTypeLessThan(String value) {
            addCriterion("OWNER_TYPE <", value, "ownerType");
            return (Criteria) this;
        }

        public Criteria andOwnerTypeLessThanOrEqualTo(String value) {
            addCriterion("OWNER_TYPE <=", value, "ownerType");
            return (Criteria) this;
        }

        public Criteria andOwnerTypeLike(String value) {
            addCriterion("OWNER_TYPE like", value, "ownerType");
            return (Criteria) this;
        }

        public Criteria andOwnerTypeNotLike(String value) {
            addCriterion("OWNER_TYPE not like", value, "ownerType");
            return (Criteria) this;
        }

        public Criteria andOwnerTypeIn(List<String> values) {
            addCriterion("OWNER_TYPE in", values, "ownerType");
            return (Criteria) this;
        }

        public Criteria andOwnerTypeNotIn(List<String> values) {
            addCriterion("OWNER_TYPE not in", values, "ownerType");
            return (Criteria) this;
        }

        public Criteria andOwnerTypeBetween(String value1, String value2) {
            addCriterion("OWNER_TYPE between", value1, value2, "ownerType");
            return (Criteria) this;
        }

        public Criteria andOwnerTypeNotBetween(String value1, String value2) {
            addCriterion("OWNER_TYPE not between", value1, value2, "ownerType");
            return (Criteria) this;
        }

        public Criteria andOwnerIdIsNull() {
            addCriterion("OWNER_ID is null");
            return (Criteria) this;
        }

        public Criteria andOwnerIdIsNotNull() {
            addCriterion("OWNER_ID is not null");
            return (Criteria) this;
        }

        public Criteria andOwnerIdEqualTo(Long value) {
            addCriterion("OWNER_ID =", value, "ownerId");
            return (Criteria) this;
        }

        public Criteria andOwnerIdNotEqualTo(Long value) {
            addCriterion("OWNER_ID <>", value, "ownerId");
            return (Criteria) this;
        }

        public Criteria andOwnerIdGreaterThan(Long value) {
            addCriterion("OWNER_ID >", value, "ownerId");
            return (Criteria) this;
        }

        public Criteria andOwnerIdGreaterThanOrEqualTo(Long value) {
            addCriterion("OWNER_ID >=", value, "ownerId");
            return (Criteria) this;
        }

        public Criteria andOwnerIdLessThan(Long value) {
            addCriterion("OWNER_ID <", value, "ownerId");
            return (Criteria) this;
        }

        public Criteria andOwnerIdLessThanOrEqualTo(Long value) {
            addCriterion("OWNER_ID <=", value, "ownerId");
            return (Criteria) this;
        }

        public Criteria andOwnerIdIn(List<Long> values) {
            addCriterion("OWNER_ID in", values, "ownerId");
            return (Criteria) this;
        }

        public Criteria andOwnerIdNotIn(List<Long> values) {
            addCriterion("OWNER_ID not in", values, "ownerId");
            return (Criteria) this;
        }

        public Criteria andOwnerIdBetween(Long value1, Long value2) {
            addCriterion("OWNER_ID between", value1, value2, "ownerId");
            return (Criteria) this;
        }

        public Criteria andOwnerIdNotBetween(Long value1, Long value2) {
            addCriterion("OWNER_ID not between", value1, value2, "ownerId");
            return (Criteria) this;
        }

        public Criteria andCredentialIdIsNull() {
            addCriterion("CREDENTIAL_ID is null");
            return (Criteria) this;
        }

        public Criteria andCredentialIdIsNotNull() {
            addCriterion("CREDENTIAL_ID is not null");
            return (Criteria) this;
        }

        public Criteria andCredentialIdEqualTo(String value) {
            addCriterion("CREDENTIAL_ID =", value, "credentialId");
            return (Criteria) this;
        }

        public Criteria andCredentialIdNotEqualTo(String value) {
            addCriterion("CREDENTIAL_ID <>", value, "credentialId");
            return (Criteria) this;
        }

        public Criteria andCredentialIdGreaterThan(String value) {
            addCriterion("CREDENTIAL_ID >", value, "credentialId");
            return (Criteria) this;
        }

        public Criteria andCredentialIdGreaterThanOrEqualTo(String value) {
            addCriterion("CREDENTIAL_ID >=", value, "credentialId");
            return (Criteria) this;
        }

        public Criteria andCredentialIdLessThan(String value) {
            addCriterion("CREDENTIAL_ID <", value, "credentialId");
            return (Criteria) this;
        }

        public Criteria andCredentialIdLessThanOrEqualTo(String value) {
            addCriterion("CREDENTIAL_ID <=", value, "credentialId");
            return (Criteria) this;
        }

        public Criteria andCredentialIdLike(String value) {
            addCriterion("CREDENTIAL_ID like", value, "credentialId");
            return (Criteria) this;
        }

        public Criteria andCredentialIdNotLike(String value) {
            addCriterion("CREDENTIAL_ID not like", value, "credentialId");
            return (Criteria) this;
        }

        public Criteria andCredentialIdIn(List<String> values) {
            addCriterion("CREDENTIAL_ID in", values, "credentialId");
            return (Criteria) this;
        }

        public Criteria andCredentialIdNotIn(List<String> values) {
            addCriterion("CREDENTIAL_ID not in", values, "credentialId");
            return (Criteria) this;
        }

        public Criteria andCredentialIdBetween(String value1, String value2) {
            addCriterion("CREDENTIAL_ID between", value1, value2, "credentialId");
            return (Criteria) this;
        }

        public Criteria andCredentialIdNotBetween(String value1, String value2) {
            addCriterion("CREDENTIAL_ID not between", value1, value2, "credentialId");
            return (Criteria) this;
        }

        public Criteria andClonePathIsNull() {
            addCriterion("CLONE_PATH is null");
            return (Criteria) this;
        }

        public Criteria andClonePathIsNotNull() {
            addCriterion("CLONE_PATH is not null");
            return (Criteria) this;
        }

        public Criteria andClonePathEqualTo(String value) {
            addCriterion("CLONE_PATH =", value, "clonePath");
            return (Criteria) this;
        }

        public Criteria andClonePathNotEqualTo(String value) {
            addCriterion("CLONE_PATH <>", value, "clonePath");
            return (Criteria) this;
        }

        public Criteria andClonePathGreaterThan(String value) {
            addCriterion("CLONE_PATH >", value, "clonePath");
            return (Criteria) this;
        }

        public Criteria andClonePathGreaterThanOrEqualTo(String value) {
            addCriterion("CLONE_PATH >=", value, "clonePath");
            return (Criteria) this;
        }

        public Criteria andClonePathLessThan(String value) {
            addCriterion("CLONE_PATH <", value, "clonePath");
            return (Criteria) this;
        }

        public Criteria andClonePathLessThanOrEqualTo(String value) {
            addCriterion("CLONE_PATH <=", value, "clonePath");
            return (Criteria) this;
        }

        public Criteria andClonePathLike(String value) {
            addCriterion("CLONE_PATH like", value, "clonePath");
            return (Criteria) this;
        }

        public Criteria andClonePathNotLike(String value) {
            addCriterion("CLONE_PATH not like", value, "clonePath");
            return (Criteria) this;
        }

        public Criteria andClonePathIn(List<String> values) {
            addCriterion("CLONE_PATH in", values, "clonePath");
            return (Criteria) this;
        }

        public Criteria andClonePathNotIn(List<String> values) {
            addCriterion("CLONE_PATH not in", values, "clonePath");
            return (Criteria) this;
        }

        public Criteria andClonePathBetween(String value1, String value2) {
            addCriterion("CLONE_PATH between", value1, value2, "clonePath");
            return (Criteria) this;
        }

        public Criteria andClonePathNotBetween(String value1, String value2) {
            addCriterion("CLONE_PATH not between", value1, value2, "clonePath");
            return (Criteria) this;
        }

        public Criteria andDefaultBranchIsNull() {
            addCriterion("DEFAULT_BRANCH is null");
            return (Criteria) this;
        }

        public Criteria andDefaultBranchIsNotNull() {
            addCriterion("DEFAULT_BRANCH is not null");
            return (Criteria) this;
        }

        public Criteria andDefaultBranchEqualTo(String value) {
            addCriterion("DEFAULT_BRANCH =", value, "defaultBranch");
            return (Criteria) this;
        }

        public Criteria andDefaultBranchNotEqualTo(String value) {
            addCriterion("DEFAULT_BRANCH <>", value, "defaultBranch");
            return (Criteria) this;
        }

        public Criteria andDefaultBranchGreaterThan(String value) {
            addCriterion("DEFAULT_BRANCH >", value, "defaultBranch");
            return (Criteria) this;
        }

        public Criteria andDefaultBranchGreaterThanOrEqualTo(String value) {
            addCriterion("DEFAULT_BRANCH >=", value, "defaultBranch");
            return (Criteria) this;
        }

        public Criteria andDefaultBranchLessThan(String value) {
            addCriterion("DEFAULT_BRANCH <", value, "defaultBranch");
            return (Criteria) this;
        }

        public Criteria andDefaultBranchLessThanOrEqualTo(String value) {
            addCriterion("DEFAULT_BRANCH <=", value, "defaultBranch");
            return (Criteria) this;
        }

        public Criteria andDefaultBranchLike(String value) {
            addCriterion("DEFAULT_BRANCH like", value, "defaultBranch");
            return (Criteria) this;
        }

        public Criteria andDefaultBranchNotLike(String value) {
            addCriterion("DEFAULT_BRANCH not like", value, "defaultBranch");
            return (Criteria) this;
        }

        public Criteria andDefaultBranchIn(List<String> values) {
            addCriterion("DEFAULT_BRANCH in", values, "defaultBranch");
            return (Criteria) this;
        }

        public Criteria andDefaultBranchNotIn(List<String> values) {
            addCriterion("DEFAULT_BRANCH not in", values, "defaultBranch");
            return (Criteria) this;
        }

        public Criteria andDefaultBranchBetween(String value1, String value2) {
            addCriterion("DEFAULT_BRANCH between", value1, value2, "defaultBranch");
            return (Criteria) this;
        }

        public Criteria andDefaultBranchNotBetween(String value1, String value2) {
            addCriterion("DEFAULT_BRANCH not between", value1, value2, "defaultBranch");
            return (Criteria) this;
        }

        public Criteria andVisibilityIsNull() {
            addCriterion("VISIBILITY is null");
            return (Criteria) this;
        }

        public Criteria andVisibilityIsNotNull() {
            addCriterion("VISIBILITY is not null");
            return (Criteria) this;
        }

        public Criteria andVisibilityEqualTo(String value) {
            addCriterion("VISIBILITY =", value, "visibility");
            return (Criteria) this;
        }

        public Criteria andVisibilityNotEqualTo(String value) {
            addCriterion("VISIBILITY <>", value, "visibility");
            return (Criteria) this;
        }

        public Criteria andVisibilityGreaterThan(String value) {
            addCriterion("VISIBILITY >", value, "visibility");
            return (Criteria) this;
        }

        public Criteria andVisibilityGreaterThanOrEqualTo(String value) {
            addCriterion("VISIBILITY >=", value, "visibility");
            return (Criteria) this;
        }

        public Criteria andVisibilityLessThan(String value) {
            addCriterion("VISIBILITY <", value, "visibility");
            return (Criteria) this;
        }

        public Criteria andVisibilityLessThanOrEqualTo(String value) {
            addCriterion("VISIBILITY <=", value, "visibility");
            return (Criteria) this;
        }

        public Criteria andVisibilityLike(String value) {
            addCriterion("VISIBILITY like", value, "visibility");
            return (Criteria) this;
        }

        public Criteria andVisibilityNotLike(String value) {
            addCriterion("VISIBILITY not like", value, "visibility");
            return (Criteria) this;
        }

        public Criteria andVisibilityIn(List<String> values) {
            addCriterion("VISIBILITY in", values, "visibility");
            return (Criteria) this;
        }

        public Criteria andVisibilityNotIn(List<String> values) {
            addCriterion("VISIBILITY not in", values, "visibility");
            return (Criteria) this;
        }

        public Criteria andVisibilityBetween(String value1, String value2) {
            addCriterion("VISIBILITY between", value1, value2, "visibility");
            return (Criteria) this;
        }

        public Criteria andVisibilityNotBetween(String value1, String value2) {
            addCriterion("VISIBILITY not between", value1, value2, "visibility");
            return (Criteria) this;
        }

        public Criteria andStatusIsNull() {
            addCriterion("STATUS is null");
            return (Criteria) this;
        }

        public Criteria andStatusIsNotNull() {
            addCriterion("STATUS is not null");
            return (Criteria) this;
        }

        public Criteria andStatusEqualTo(String value) {
            addCriterion("STATUS =", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotEqualTo(String value) {
            addCriterion("STATUS <>", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThan(String value) {
            addCriterion("STATUS >", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThanOrEqualTo(String value) {
            addCriterion("STATUS >=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThan(String value) {
            addCriterion("STATUS <", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThanOrEqualTo(String value) {
            addCriterion("STATUS <=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLike(String value) {
            addCriterion("STATUS like", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotLike(String value) {
            addCriterion("STATUS not like", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusIn(List<String> values) {
            addCriterion("STATUS in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotIn(List<String> values) {
            addCriterion("STATUS not in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusBetween(String value1, String value2) {
            addCriterion("STATUS between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotBetween(String value1, String value2) {
            addCriterion("STATUS not between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andLastSyncedAtIsNull() {
            addCriterion("LAST_SYNCED_AT is null");
            return (Criteria) this;
        }

        public Criteria andLastSyncedAtIsNotNull() {
            addCriterion("LAST_SYNCED_AT is not null");
            return (Criteria) this;
        }

        public Criteria andLastSyncedAtEqualTo(LocalDateTime value) {
            addCriterion("LAST_SYNCED_AT =", value, "lastSyncedAt");
            return (Criteria) this;
        }

        public Criteria andLastSyncedAtNotEqualTo(LocalDateTime value) {
            addCriterion("LAST_SYNCED_AT <>", value, "lastSyncedAt");
            return (Criteria) this;
        }

        public Criteria andLastSyncedAtGreaterThan(LocalDateTime value) {
            addCriterion("LAST_SYNCED_AT >", value, "lastSyncedAt");
            return (Criteria) this;
        }

        public Criteria andLastSyncedAtGreaterThanOrEqualTo(LocalDateTime value) {
            addCriterion("LAST_SYNCED_AT >=", value, "lastSyncedAt");
            return (Criteria) this;
        }

        public Criteria andLastSyncedAtLessThan(LocalDateTime value) {
            addCriterion("LAST_SYNCED_AT <", value, "lastSyncedAt");
            return (Criteria) this;
        }

        public Criteria andLastSyncedAtLessThanOrEqualTo(LocalDateTime value) {
            addCriterion("LAST_SYNCED_AT <=", value, "lastSyncedAt");
            return (Criteria) this;
        }

        public Criteria andLastSyncedAtIn(List<LocalDateTime> values) {
            addCriterion("LAST_SYNCED_AT in", values, "lastSyncedAt");
            return (Criteria) this;
        }

        public Criteria andLastSyncedAtNotIn(List<LocalDateTime> values) {
            addCriterion("LAST_SYNCED_AT not in", values, "lastSyncedAt");
            return (Criteria) this;
        }

        public Criteria andLastSyncedAtBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("LAST_SYNCED_AT between", value1, value2, "lastSyncedAt");
            return (Criteria) this;
        }

        public Criteria andLastSyncedAtNotBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("LAST_SYNCED_AT not between", value1, value2, "lastSyncedAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtIsNull() {
            addCriterion("CREATED_AT is null");
            return (Criteria) this;
        }

        public Criteria andCreatedAtIsNotNull() {
            addCriterion("CREATED_AT is not null");
            return (Criteria) this;
        }

        public Criteria andCreatedAtEqualTo(LocalDateTime value) {
            addCriterion("CREATED_AT =", value, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtNotEqualTo(LocalDateTime value) {
            addCriterion("CREATED_AT <>", value, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtGreaterThan(LocalDateTime value) {
            addCriterion("CREATED_AT >", value, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtGreaterThanOrEqualTo(LocalDateTime value) {
            addCriterion("CREATED_AT >=", value, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtLessThan(LocalDateTime value) {
            addCriterion("CREATED_AT <", value, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtLessThanOrEqualTo(LocalDateTime value) {
            addCriterion("CREATED_AT <=", value, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtIn(List<LocalDateTime> values) {
            addCriterion("CREATED_AT in", values, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtNotIn(List<LocalDateTime> values) {
            addCriterion("CREATED_AT not in", values, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("CREATED_AT between", value1, value2, "createdAt");
            return (Criteria) this;
        }

        public Criteria andCreatedAtNotBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("CREATED_AT not between", value1, value2, "createdAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtIsNull() {
            addCriterion("UPDATED_AT is null");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtIsNotNull() {
            addCriterion("UPDATED_AT is not null");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtEqualTo(LocalDateTime value) {
            addCriterion("UPDATED_AT =", value, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtNotEqualTo(LocalDateTime value) {
            addCriterion("UPDATED_AT <>", value, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtGreaterThan(LocalDateTime value) {
            addCriterion("UPDATED_AT >", value, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtGreaterThanOrEqualTo(LocalDateTime value) {
            addCriterion("UPDATED_AT >=", value, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtLessThan(LocalDateTime value) {
            addCriterion("UPDATED_AT <", value, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtLessThanOrEqualTo(LocalDateTime value) {
            addCriterion("UPDATED_AT <=", value, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtIn(List<LocalDateTime> values) {
            addCriterion("UPDATED_AT in", values, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtNotIn(List<LocalDateTime> values) {
            addCriterion("UPDATED_AT not in", values, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("UPDATED_AT between", value1, value2, "updatedAt");
            return (Criteria) this;
        }

        public Criteria andUpdatedAtNotBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("UPDATED_AT not between", value1, value2, "updatedAt");
            return (Criteria) this;
        }
    }

    public static class Criteria extends GeneratedCriteria {
        protected Criteria() {
            super();
        }
    }

    public static class Criterion {
        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }

        public String getTypeHandler() {
            return typeHandler;
        }

        protected Criterion(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }
    }
}