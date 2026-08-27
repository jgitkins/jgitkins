package io.jgitkins.server.change.review.adapter.out.persistence.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PullRequestEntityCondition {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public PullRequestEntityCondition() {
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

        public Criteria andRepositoryIdIsNull() {
            addCriterion("REPOSITORY_ID is null");
            return (Criteria) this;
        }

        public Criteria andRepositoryIdIsNotNull() {
            addCriterion("REPOSITORY_ID is not null");
            return (Criteria) this;
        }

        public Criteria andRepositoryIdEqualTo(Long value) {
            addCriterion("REPOSITORY_ID =", value, "repositoryId");
            return (Criteria) this;
        }

        public Criteria andRepositoryIdNotEqualTo(Long value) {
            addCriterion("REPOSITORY_ID <>", value, "repositoryId");
            return (Criteria) this;
        }

        public Criteria andRepositoryIdGreaterThan(Long value) {
            addCriterion("REPOSITORY_ID >", value, "repositoryId");
            return (Criteria) this;
        }

        public Criteria andRepositoryIdGreaterThanOrEqualTo(Long value) {
            addCriterion("REPOSITORY_ID >=", value, "repositoryId");
            return (Criteria) this;
        }

        public Criteria andRepositoryIdLessThan(Long value) {
            addCriterion("REPOSITORY_ID <", value, "repositoryId");
            return (Criteria) this;
        }

        public Criteria andRepositoryIdLessThanOrEqualTo(Long value) {
            addCriterion("REPOSITORY_ID <=", value, "repositoryId");
            return (Criteria) this;
        }

        public Criteria andRepositoryIdIn(List<Long> values) {
            addCriterion("REPOSITORY_ID in", values, "repositoryId");
            return (Criteria) this;
        }

        public Criteria andRepositoryIdNotIn(List<Long> values) {
            addCriterion("REPOSITORY_ID not in", values, "repositoryId");
            return (Criteria) this;
        }

        public Criteria andRepositoryIdBetween(Long value1, Long value2) {
            addCriterion("REPOSITORY_ID between", value1, value2, "repositoryId");
            return (Criteria) this;
        }

        public Criteria andRepositoryIdNotBetween(Long value1, Long value2) {
            addCriterion("REPOSITORY_ID not between", value1, value2, "repositoryId");
            return (Criteria) this;
        }

        public Criteria andSourceBranchIsNull() {
            addCriterion("SOURCE_BRANCH is null");
            return (Criteria) this;
        }

        public Criteria andSourceBranchIsNotNull() {
            addCriterion("SOURCE_BRANCH is not null");
            return (Criteria) this;
        }

        public Criteria andSourceBranchEqualTo(String value) {
            addCriterion("SOURCE_BRANCH =", value, "sourceBranch");
            return (Criteria) this;
        }

        public Criteria andSourceBranchNotEqualTo(String value) {
            addCriterion("SOURCE_BRANCH <>", value, "sourceBranch");
            return (Criteria) this;
        }

        public Criteria andSourceBranchGreaterThan(String value) {
            addCriterion("SOURCE_BRANCH >", value, "sourceBranch");
            return (Criteria) this;
        }

        public Criteria andSourceBranchGreaterThanOrEqualTo(String value) {
            addCriterion("SOURCE_BRANCH >=", value, "sourceBranch");
            return (Criteria) this;
        }

        public Criteria andSourceBranchLessThan(String value) {
            addCriterion("SOURCE_BRANCH <", value, "sourceBranch");
            return (Criteria) this;
        }

        public Criteria andSourceBranchLessThanOrEqualTo(String value) {
            addCriterion("SOURCE_BRANCH <=", value, "sourceBranch");
            return (Criteria) this;
        }

        public Criteria andSourceBranchLike(String value) {
            addCriterion("SOURCE_BRANCH like", value, "sourceBranch");
            return (Criteria) this;
        }

        public Criteria andSourceBranchNotLike(String value) {
            addCriterion("SOURCE_BRANCH not like", value, "sourceBranch");
            return (Criteria) this;
        }

        public Criteria andSourceBranchIn(List<String> values) {
            addCriterion("SOURCE_BRANCH in", values, "sourceBranch");
            return (Criteria) this;
        }

        public Criteria andSourceBranchNotIn(List<String> values) {
            addCriterion("SOURCE_BRANCH not in", values, "sourceBranch");
            return (Criteria) this;
        }

        public Criteria andSourceBranchBetween(String value1, String value2) {
            addCriterion("SOURCE_BRANCH between", value1, value2, "sourceBranch");
            return (Criteria) this;
        }

        public Criteria andSourceBranchNotBetween(String value1, String value2) {
            addCriterion("SOURCE_BRANCH not between", value1, value2, "sourceBranch");
            return (Criteria) this;
        }

        public Criteria andSourceHeadIsNull() {
            addCriterion("SOURCE_HEAD is null");
            return (Criteria) this;
        }

        public Criteria andSourceHeadIsNotNull() {
            addCriterion("SOURCE_HEAD is not null");
            return (Criteria) this;
        }

        public Criteria andSourceHeadEqualTo(String value) {
            addCriterion("SOURCE_HEAD =", value, "sourceHead");
            return (Criteria) this;
        }

        public Criteria andSourceHeadNotEqualTo(String value) {
            addCriterion("SOURCE_HEAD <>", value, "sourceHead");
            return (Criteria) this;
        }

        public Criteria andSourceHeadGreaterThan(String value) {
            addCriterion("SOURCE_HEAD >", value, "sourceHead");
            return (Criteria) this;
        }

        public Criteria andSourceHeadGreaterThanOrEqualTo(String value) {
            addCriterion("SOURCE_HEAD >=", value, "sourceHead");
            return (Criteria) this;
        }

        public Criteria andSourceHeadLessThan(String value) {
            addCriterion("SOURCE_HEAD <", value, "sourceHead");
            return (Criteria) this;
        }

        public Criteria andSourceHeadLessThanOrEqualTo(String value) {
            addCriterion("SOURCE_HEAD <=", value, "sourceHead");
            return (Criteria) this;
        }

        public Criteria andSourceHeadLike(String value) {
            addCriterion("SOURCE_HEAD like", value, "sourceHead");
            return (Criteria) this;
        }

        public Criteria andSourceHeadNotLike(String value) {
            addCriterion("SOURCE_HEAD not like", value, "sourceHead");
            return (Criteria) this;
        }

        public Criteria andSourceHeadIn(List<String> values) {
            addCriterion("SOURCE_HEAD in", values, "sourceHead");
            return (Criteria) this;
        }

        public Criteria andSourceHeadNotIn(List<String> values) {
            addCriterion("SOURCE_HEAD not in", values, "sourceHead");
            return (Criteria) this;
        }

        public Criteria andSourceHeadBetween(String value1, String value2) {
            addCriterion("SOURCE_HEAD between", value1, value2, "sourceHead");
            return (Criteria) this;
        }

        public Criteria andSourceHeadNotBetween(String value1, String value2) {
            addCriterion("SOURCE_HEAD not between", value1, value2, "sourceHead");
            return (Criteria) this;
        }

        public Criteria andTargetBranchIsNull() {
            addCriterion("TARGET_BRANCH is null");
            return (Criteria) this;
        }

        public Criteria andTargetBranchIsNotNull() {
            addCriterion("TARGET_BRANCH is not null");
            return (Criteria) this;
        }

        public Criteria andTargetBranchEqualTo(String value) {
            addCriterion("TARGET_BRANCH =", value, "targetBranch");
            return (Criteria) this;
        }

        public Criteria andTargetBranchNotEqualTo(String value) {
            addCriterion("TARGET_BRANCH <>", value, "targetBranch");
            return (Criteria) this;
        }

        public Criteria andTargetBranchGreaterThan(String value) {
            addCriterion("TARGET_BRANCH >", value, "targetBranch");
            return (Criteria) this;
        }

        public Criteria andTargetBranchGreaterThanOrEqualTo(String value) {
            addCriterion("TARGET_BRANCH >=", value, "targetBranch");
            return (Criteria) this;
        }

        public Criteria andTargetBranchLessThan(String value) {
            addCriterion("TARGET_BRANCH <", value, "targetBranch");
            return (Criteria) this;
        }

        public Criteria andTargetBranchLessThanOrEqualTo(String value) {
            addCriterion("TARGET_BRANCH <=", value, "targetBranch");
            return (Criteria) this;
        }

        public Criteria andTargetBranchLike(String value) {
            addCriterion("TARGET_BRANCH like", value, "targetBranch");
            return (Criteria) this;
        }

        public Criteria andTargetBranchNotLike(String value) {
            addCriterion("TARGET_BRANCH not like", value, "targetBranch");
            return (Criteria) this;
        }

        public Criteria andTargetBranchIn(List<String> values) {
            addCriterion("TARGET_BRANCH in", values, "targetBranch");
            return (Criteria) this;
        }

        public Criteria andTargetBranchNotIn(List<String> values) {
            addCriterion("TARGET_BRANCH not in", values, "targetBranch");
            return (Criteria) this;
        }

        public Criteria andTargetBranchBetween(String value1, String value2) {
            addCriterion("TARGET_BRANCH between", value1, value2, "targetBranch");
            return (Criteria) this;
        }

        public Criteria andTargetBranchNotBetween(String value1, String value2) {
            addCriterion("TARGET_BRANCH not between", value1, value2, "targetBranch");
            return (Criteria) this;
        }

        public Criteria andTargetHeadIsNull() {
            addCriterion("TARGET_HEAD is null");
            return (Criteria) this;
        }

        public Criteria andTargetHeadIsNotNull() {
            addCriterion("TARGET_HEAD is not null");
            return (Criteria) this;
        }

        public Criteria andTargetHeadEqualTo(String value) {
            addCriterion("TARGET_HEAD =", value, "targetHead");
            return (Criteria) this;
        }

        public Criteria andTargetHeadNotEqualTo(String value) {
            addCriterion("TARGET_HEAD <>", value, "targetHead");
            return (Criteria) this;
        }

        public Criteria andTargetHeadGreaterThan(String value) {
            addCriterion("TARGET_HEAD >", value, "targetHead");
            return (Criteria) this;
        }

        public Criteria andTargetHeadGreaterThanOrEqualTo(String value) {
            addCriterion("TARGET_HEAD >=", value, "targetHead");
            return (Criteria) this;
        }

        public Criteria andTargetHeadLessThan(String value) {
            addCriterion("TARGET_HEAD <", value, "targetHead");
            return (Criteria) this;
        }

        public Criteria andTargetHeadLessThanOrEqualTo(String value) {
            addCriterion("TARGET_HEAD <=", value, "targetHead");
            return (Criteria) this;
        }

        public Criteria andTargetHeadLike(String value) {
            addCriterion("TARGET_HEAD like", value, "targetHead");
            return (Criteria) this;
        }

        public Criteria andTargetHeadNotLike(String value) {
            addCriterion("TARGET_HEAD not like", value, "targetHead");
            return (Criteria) this;
        }

        public Criteria andTargetHeadIn(List<String> values) {
            addCriterion("TARGET_HEAD in", values, "targetHead");
            return (Criteria) this;
        }

        public Criteria andTargetHeadNotIn(List<String> values) {
            addCriterion("TARGET_HEAD not in", values, "targetHead");
            return (Criteria) this;
        }

        public Criteria andTargetHeadBetween(String value1, String value2) {
            addCriterion("TARGET_HEAD between", value1, value2, "targetHead");
            return (Criteria) this;
        }

        public Criteria andTargetHeadNotBetween(String value1, String value2) {
            addCriterion("TARGET_HEAD not between", value1, value2, "targetHead");
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

        public Criteria andTargetDriftedIsNull() {
            addCriterion("TARGET_DRIFTED is null");
            return (Criteria) this;
        }

        public Criteria andTargetDriftedIsNotNull() {
            addCriterion("TARGET_DRIFTED is not null");
            return (Criteria) this;
        }

        public Criteria andTargetDriftedEqualTo(Boolean value) {
            addCriterion("TARGET_DRIFTED =", value, "targetDrifted");
            return (Criteria) this;
        }

        public Criteria andTargetDriftedNotEqualTo(Boolean value) {
            addCriterion("TARGET_DRIFTED <>", value, "targetDrifted");
            return (Criteria) this;
        }

        public Criteria andTargetDriftedGreaterThan(Boolean value) {
            addCriterion("TARGET_DRIFTED >", value, "targetDrifted");
            return (Criteria) this;
        }

        public Criteria andTargetDriftedGreaterThanOrEqualTo(Boolean value) {
            addCriterion("TARGET_DRIFTED >=", value, "targetDrifted");
            return (Criteria) this;
        }

        public Criteria andTargetDriftedLessThan(Boolean value) {
            addCriterion("TARGET_DRIFTED <", value, "targetDrifted");
            return (Criteria) this;
        }

        public Criteria andTargetDriftedLessThanOrEqualTo(Boolean value) {
            addCriterion("TARGET_DRIFTED <=", value, "targetDrifted");
            return (Criteria) this;
        }

        public Criteria andTargetDriftedIn(List<Boolean> values) {
            addCriterion("TARGET_DRIFTED in", values, "targetDrifted");
            return (Criteria) this;
        }

        public Criteria andTargetDriftedNotIn(List<Boolean> values) {
            addCriterion("TARGET_DRIFTED not in", values, "targetDrifted");
            return (Criteria) this;
        }

        public Criteria andTargetDriftedBetween(Boolean value1, Boolean value2) {
            addCriterion("TARGET_DRIFTED between", value1, value2, "targetDrifted");
            return (Criteria) this;
        }

        public Criteria andTargetDriftedNotBetween(Boolean value1, Boolean value2) {
            addCriterion("TARGET_DRIFTED not between", value1, value2, "targetDrifted");
            return (Criteria) this;
        }

        public Criteria andPreviousTargetHeadIsNull() {
            addCriterion("PREVIOUS_TARGET_HEAD is null");
            return (Criteria) this;
        }

        public Criteria andPreviousTargetHeadIsNotNull() {
            addCriterion("PREVIOUS_TARGET_HEAD is not null");
            return (Criteria) this;
        }

        public Criteria andPreviousTargetHeadEqualTo(String value) {
            addCriterion("PREVIOUS_TARGET_HEAD =", value, "previousTargetHead");
            return (Criteria) this;
        }

        public Criteria andPreviousTargetHeadNotEqualTo(String value) {
            addCriterion("PREVIOUS_TARGET_HEAD <>", value, "previousTargetHead");
            return (Criteria) this;
        }

        public Criteria andPreviousTargetHeadGreaterThan(String value) {
            addCriterion("PREVIOUS_TARGET_HEAD >", value, "previousTargetHead");
            return (Criteria) this;
        }

        public Criteria andPreviousTargetHeadGreaterThanOrEqualTo(String value) {
            addCriterion("PREVIOUS_TARGET_HEAD >=", value, "previousTargetHead");
            return (Criteria) this;
        }

        public Criteria andPreviousTargetHeadLessThan(String value) {
            addCriterion("PREVIOUS_TARGET_HEAD <", value, "previousTargetHead");
            return (Criteria) this;
        }

        public Criteria andPreviousTargetHeadLessThanOrEqualTo(String value) {
            addCriterion("PREVIOUS_TARGET_HEAD <=", value, "previousTargetHead");
            return (Criteria) this;
        }

        public Criteria andPreviousTargetHeadLike(String value) {
            addCriterion("PREVIOUS_TARGET_HEAD like", value, "previousTargetHead");
            return (Criteria) this;
        }

        public Criteria andPreviousTargetHeadNotLike(String value) {
            addCriterion("PREVIOUS_TARGET_HEAD not like", value, "previousTargetHead");
            return (Criteria) this;
        }

        public Criteria andPreviousTargetHeadIn(List<String> values) {
            addCriterion("PREVIOUS_TARGET_HEAD in", values, "previousTargetHead");
            return (Criteria) this;
        }

        public Criteria andPreviousTargetHeadNotIn(List<String> values) {
            addCriterion("PREVIOUS_TARGET_HEAD not in", values, "previousTargetHead");
            return (Criteria) this;
        }

        public Criteria andPreviousTargetHeadBetween(String value1, String value2) {
            addCriterion("PREVIOUS_TARGET_HEAD between", value1, value2, "previousTargetHead");
            return (Criteria) this;
        }

        public Criteria andPreviousTargetHeadNotBetween(String value1, String value2) {
            addCriterion("PREVIOUS_TARGET_HEAD not between", value1, value2, "previousTargetHead");
            return (Criteria) this;
        }

        public Criteria andCurrentTargetHeadIsNull() {
            addCriterion("CURRENT_TARGET_HEAD is null");
            return (Criteria) this;
        }

        public Criteria andCurrentTargetHeadIsNotNull() {
            addCriterion("CURRENT_TARGET_HEAD is not null");
            return (Criteria) this;
        }

        public Criteria andCurrentTargetHeadEqualTo(String value) {
            addCriterion("CURRENT_TARGET_HEAD =", value, "currentTargetHead");
            return (Criteria) this;
        }

        public Criteria andCurrentTargetHeadNotEqualTo(String value) {
            addCriterion("CURRENT_TARGET_HEAD <>", value, "currentTargetHead");
            return (Criteria) this;
        }

        public Criteria andCurrentTargetHeadGreaterThan(String value) {
            addCriterion("CURRENT_TARGET_HEAD >", value, "currentTargetHead");
            return (Criteria) this;
        }

        public Criteria andCurrentTargetHeadGreaterThanOrEqualTo(String value) {
            addCriterion("CURRENT_TARGET_HEAD >=", value, "currentTargetHead");
            return (Criteria) this;
        }

        public Criteria andCurrentTargetHeadLessThan(String value) {
            addCriterion("CURRENT_TARGET_HEAD <", value, "currentTargetHead");
            return (Criteria) this;
        }

        public Criteria andCurrentTargetHeadLessThanOrEqualTo(String value) {
            addCriterion("CURRENT_TARGET_HEAD <=", value, "currentTargetHead");
            return (Criteria) this;
        }

        public Criteria andCurrentTargetHeadLike(String value) {
            addCriterion("CURRENT_TARGET_HEAD like", value, "currentTargetHead");
            return (Criteria) this;
        }

        public Criteria andCurrentTargetHeadNotLike(String value) {
            addCriterion("CURRENT_TARGET_HEAD not like", value, "currentTargetHead");
            return (Criteria) this;
        }

        public Criteria andCurrentTargetHeadIn(List<String> values) {
            addCriterion("CURRENT_TARGET_HEAD in", values, "currentTargetHead");
            return (Criteria) this;
        }

        public Criteria andCurrentTargetHeadNotIn(List<String> values) {
            addCriterion("CURRENT_TARGET_HEAD not in", values, "currentTargetHead");
            return (Criteria) this;
        }

        public Criteria andCurrentTargetHeadBetween(String value1, String value2) {
            addCriterion("CURRENT_TARGET_HEAD between", value1, value2, "currentTargetHead");
            return (Criteria) this;
        }

        public Criteria andCurrentTargetHeadNotBetween(String value1, String value2) {
            addCriterion("CURRENT_TARGET_HEAD not between", value1, value2, "currentTargetHead");
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
