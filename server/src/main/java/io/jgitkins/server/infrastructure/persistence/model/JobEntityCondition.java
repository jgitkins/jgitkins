package io.jgitkins.server.infrastructure.persistence.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JobEntityCondition {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public JobEntityCondition() {
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

        public Criteria andCommitHashIsNull() {
            addCriterion("COMMIT_HASH is null");
            return (Criteria) this;
        }

        public Criteria andCommitHashIsNotNull() {
            addCriterion("COMMIT_HASH is not null");
            return (Criteria) this;
        }

        public Criteria andCommitHashEqualTo(String value) {
            addCriterion("COMMIT_HASH =", value, "commitHash");
            return (Criteria) this;
        }

        public Criteria andCommitHashNotEqualTo(String value) {
            addCriterion("COMMIT_HASH <>", value, "commitHash");
            return (Criteria) this;
        }

        public Criteria andCommitHashGreaterThan(String value) {
            addCriterion("COMMIT_HASH >", value, "commitHash");
            return (Criteria) this;
        }

        public Criteria andCommitHashGreaterThanOrEqualTo(String value) {
            addCriterion("COMMIT_HASH >=", value, "commitHash");
            return (Criteria) this;
        }

        public Criteria andCommitHashLessThan(String value) {
            addCriterion("COMMIT_HASH <", value, "commitHash");
            return (Criteria) this;
        }

        public Criteria andCommitHashLessThanOrEqualTo(String value) {
            addCriterion("COMMIT_HASH <=", value, "commitHash");
            return (Criteria) this;
        }

        public Criteria andCommitHashLike(String value) {
            addCriterion("COMMIT_HASH like", value, "commitHash");
            return (Criteria) this;
        }

        public Criteria andCommitHashNotLike(String value) {
            addCriterion("COMMIT_HASH not like", value, "commitHash");
            return (Criteria) this;
        }

        public Criteria andCommitHashIn(List<String> values) {
            addCriterion("COMMIT_HASH in", values, "commitHash");
            return (Criteria) this;
        }

        public Criteria andCommitHashNotIn(List<String> values) {
            addCriterion("COMMIT_HASH not in", values, "commitHash");
            return (Criteria) this;
        }

        public Criteria andCommitHashBetween(String value1, String value2) {
            addCriterion("COMMIT_HASH between", value1, value2, "commitHash");
            return (Criteria) this;
        }

        public Criteria andCommitHashNotBetween(String value1, String value2) {
            addCriterion("COMMIT_HASH not between", value1, value2, "commitHash");
            return (Criteria) this;
        }

        public Criteria andBranchNameIsNull() {
            addCriterion("BRANCH_NAME is null");
            return (Criteria) this;
        }

        public Criteria andBranchNameIsNotNull() {
            addCriterion("BRANCH_NAME is not null");
            return (Criteria) this;
        }

        public Criteria andBranchNameEqualTo(String value) {
            addCriterion("BRANCH_NAME =", value, "branchName");
            return (Criteria) this;
        }

        public Criteria andBranchNameNotEqualTo(String value) {
            addCriterion("BRANCH_NAME <>", value, "branchName");
            return (Criteria) this;
        }

        public Criteria andBranchNameGreaterThan(String value) {
            addCriterion("BRANCH_NAME >", value, "branchName");
            return (Criteria) this;
        }

        public Criteria andBranchNameGreaterThanOrEqualTo(String value) {
            addCriterion("BRANCH_NAME >=", value, "branchName");
            return (Criteria) this;
        }

        public Criteria andBranchNameLessThan(String value) {
            addCriterion("BRANCH_NAME <", value, "branchName");
            return (Criteria) this;
        }

        public Criteria andBranchNameLessThanOrEqualTo(String value) {
            addCriterion("BRANCH_NAME <=", value, "branchName");
            return (Criteria) this;
        }

        public Criteria andBranchNameLike(String value) {
            addCriterion("BRANCH_NAME like", value, "branchName");
            return (Criteria) this;
        }

        public Criteria andBranchNameNotLike(String value) {
            addCriterion("BRANCH_NAME not like", value, "branchName");
            return (Criteria) this;
        }

        public Criteria andBranchNameIn(List<String> values) {
            addCriterion("BRANCH_NAME in", values, "branchName");
            return (Criteria) this;
        }

        public Criteria andBranchNameNotIn(List<String> values) {
            addCriterion("BRANCH_NAME not in", values, "branchName");
            return (Criteria) this;
        }

        public Criteria andBranchNameBetween(String value1, String value2) {
            addCriterion("BRANCH_NAME between", value1, value2, "branchName");
            return (Criteria) this;
        }

        public Criteria andBranchNameNotBetween(String value1, String value2) {
            addCriterion("BRANCH_NAME not between", value1, value2, "branchName");
            return (Criteria) this;
        }

        public Criteria andTriggeredByIsNull() {
            addCriterion("TRIGGERED_BY is null");
            return (Criteria) this;
        }

        public Criteria andTriggeredByIsNotNull() {
            addCriterion("TRIGGERED_BY is not null");
            return (Criteria) this;
        }

        public Criteria andTriggeredByEqualTo(Long value) {
            addCriterion("TRIGGERED_BY =", value, "triggeredBy");
            return (Criteria) this;
        }

        public Criteria andTriggeredByNotEqualTo(Long value) {
            addCriterion("TRIGGERED_BY <>", value, "triggeredBy");
            return (Criteria) this;
        }

        public Criteria andTriggeredByGreaterThan(Long value) {
            addCriterion("TRIGGERED_BY >", value, "triggeredBy");
            return (Criteria) this;
        }

        public Criteria andTriggeredByGreaterThanOrEqualTo(Long value) {
            addCriterion("TRIGGERED_BY >=", value, "triggeredBy");
            return (Criteria) this;
        }

        public Criteria andTriggeredByLessThan(Long value) {
            addCriterion("TRIGGERED_BY <", value, "triggeredBy");
            return (Criteria) this;
        }

        public Criteria andTriggeredByLessThanOrEqualTo(Long value) {
            addCriterion("TRIGGERED_BY <=", value, "triggeredBy");
            return (Criteria) this;
        }

        public Criteria andTriggeredByIn(List<Long> values) {
            addCriterion("TRIGGERED_BY in", values, "triggeredBy");
            return (Criteria) this;
        }

        public Criteria andTriggeredByNotIn(List<Long> values) {
            addCriterion("TRIGGERED_BY not in", values, "triggeredBy");
            return (Criteria) this;
        }

        public Criteria andTriggeredByBetween(Long value1, Long value2) {
            addCriterion("TRIGGERED_BY between", value1, value2, "triggeredBy");
            return (Criteria) this;
        }

        public Criteria andTriggeredByNotBetween(Long value1, Long value2) {
            addCriterion("TRIGGERED_BY not between", value1, value2, "triggeredBy");
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