package io.jgitkins.runner.infrastructure.persistence.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JobHistoryEntityCondition {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public JobHistoryEntityCondition() {
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

        public Criteria andRunnerIdIsNull() {
            addCriterion("RUNNER_ID is null");
            return (Criteria) this;
        }

        public Criteria andRunnerIdIsNotNull() {
            addCriterion("RUNNER_ID is not null");
            return (Criteria) this;
        }

        public Criteria andRunnerIdEqualTo(Long value) {
            addCriterion("RUNNER_ID =", value, "runnerId");
            return (Criteria) this;
        }

        public Criteria andRunnerIdNotEqualTo(Long value) {
            addCriterion("RUNNER_ID <>", value, "runnerId");
            return (Criteria) this;
        }

        public Criteria andRunnerIdGreaterThan(Long value) {
            addCriterion("RUNNER_ID >", value, "runnerId");
            return (Criteria) this;
        }

        public Criteria andRunnerIdGreaterThanOrEqualTo(Long value) {
            addCriterion("RUNNER_ID >=", value, "runnerId");
            return (Criteria) this;
        }

        public Criteria andRunnerIdLessThan(Long value) {
            addCriterion("RUNNER_ID <", value, "runnerId");
            return (Criteria) this;
        }

        public Criteria andRunnerIdLessThanOrEqualTo(Long value) {
            addCriterion("RUNNER_ID <=", value, "runnerId");
            return (Criteria) this;
        }

        public Criteria andRunnerIdIn(List<Long> values) {
            addCriterion("RUNNER_ID in", values, "runnerId");
            return (Criteria) this;
        }

        public Criteria andRunnerIdNotIn(List<Long> values) {
            addCriterion("RUNNER_ID not in", values, "runnerId");
            return (Criteria) this;
        }

        public Criteria andRunnerIdBetween(Long value1, Long value2) {
            addCriterion("RUNNER_ID between", value1, value2, "runnerId");
            return (Criteria) this;
        }

        public Criteria andRunnerIdNotBetween(Long value1, Long value2) {
            addCriterion("RUNNER_ID not between", value1, value2, "runnerId");
            return (Criteria) this;
        }

        public Criteria andJobIdIsNull() {
            addCriterion("JOB_ID is null");
            return (Criteria) this;
        }

        public Criteria andJobIdIsNotNull() {
            addCriterion("JOB_ID is not null");
            return (Criteria) this;
        }

        public Criteria andJobIdEqualTo(Long value) {
            addCriterion("JOB_ID =", value, "jobId");
            return (Criteria) this;
        }

        public Criteria andJobIdNotEqualTo(Long value) {
            addCriterion("JOB_ID <>", value, "jobId");
            return (Criteria) this;
        }

        public Criteria andJobIdGreaterThan(Long value) {
            addCriterion("JOB_ID >", value, "jobId");
            return (Criteria) this;
        }

        public Criteria andJobIdGreaterThanOrEqualTo(Long value) {
            addCriterion("JOB_ID >=", value, "jobId");
            return (Criteria) this;
        }

        public Criteria andJobIdLessThan(Long value) {
            addCriterion("JOB_ID <", value, "jobId");
            return (Criteria) this;
        }

        public Criteria andJobIdLessThanOrEqualTo(Long value) {
            addCriterion("JOB_ID <=", value, "jobId");
            return (Criteria) this;
        }

        public Criteria andJobIdIn(List<Long> values) {
            addCriterion("JOB_ID in", values, "jobId");
            return (Criteria) this;
        }

        public Criteria andJobIdNotIn(List<Long> values) {
            addCriterion("JOB_ID not in", values, "jobId");
            return (Criteria) this;
        }

        public Criteria andJobIdBetween(Long value1, Long value2) {
            addCriterion("JOB_ID between", value1, value2, "jobId");
            return (Criteria) this;
        }

        public Criteria andJobIdNotBetween(Long value1, Long value2) {
            addCriterion("JOB_ID not between", value1, value2, "jobId");
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

        public Criteria andStartedAtIsNull() {
            addCriterion("STARTED_AT is null");
            return (Criteria) this;
        }

        public Criteria andStartedAtIsNotNull() {
            addCriterion("STARTED_AT is not null");
            return (Criteria) this;
        }

        public Criteria andStartedAtEqualTo(LocalDateTime value) {
            addCriterion("STARTED_AT =", value, "startedAt");
            return (Criteria) this;
        }

        public Criteria andStartedAtNotEqualTo(LocalDateTime value) {
            addCriterion("STARTED_AT <>", value, "startedAt");
            return (Criteria) this;
        }

        public Criteria andStartedAtGreaterThan(LocalDateTime value) {
            addCriterion("STARTED_AT >", value, "startedAt");
            return (Criteria) this;
        }

        public Criteria andStartedAtGreaterThanOrEqualTo(LocalDateTime value) {
            addCriterion("STARTED_AT >=", value, "startedAt");
            return (Criteria) this;
        }

        public Criteria andStartedAtLessThan(LocalDateTime value) {
            addCriterion("STARTED_AT <", value, "startedAt");
            return (Criteria) this;
        }

        public Criteria andStartedAtLessThanOrEqualTo(LocalDateTime value) {
            addCriterion("STARTED_AT <=", value, "startedAt");
            return (Criteria) this;
        }

        public Criteria andStartedAtIn(List<LocalDateTime> values) {
            addCriterion("STARTED_AT in", values, "startedAt");
            return (Criteria) this;
        }

        public Criteria andStartedAtNotIn(List<LocalDateTime> values) {
            addCriterion("STARTED_AT not in", values, "startedAt");
            return (Criteria) this;
        }

        public Criteria andStartedAtBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("STARTED_AT between", value1, value2, "startedAt");
            return (Criteria) this;
        }

        public Criteria andStartedAtNotBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("STARTED_AT not between", value1, value2, "startedAt");
            return (Criteria) this;
        }

        public Criteria andFinishedAtIsNull() {
            addCriterion("FINISHED_AT is null");
            return (Criteria) this;
        }

        public Criteria andFinishedAtIsNotNull() {
            addCriterion("FINISHED_AT is not null");
            return (Criteria) this;
        }

        public Criteria andFinishedAtEqualTo(LocalDateTime value) {
            addCriterion("FINISHED_AT =", value, "finishedAt");
            return (Criteria) this;
        }

        public Criteria andFinishedAtNotEqualTo(LocalDateTime value) {
            addCriterion("FINISHED_AT <>", value, "finishedAt");
            return (Criteria) this;
        }

        public Criteria andFinishedAtGreaterThan(LocalDateTime value) {
            addCriterion("FINISHED_AT >", value, "finishedAt");
            return (Criteria) this;
        }

        public Criteria andFinishedAtGreaterThanOrEqualTo(LocalDateTime value) {
            addCriterion("FINISHED_AT >=", value, "finishedAt");
            return (Criteria) this;
        }

        public Criteria andFinishedAtLessThan(LocalDateTime value) {
            addCriterion("FINISHED_AT <", value, "finishedAt");
            return (Criteria) this;
        }

        public Criteria andFinishedAtLessThanOrEqualTo(LocalDateTime value) {
            addCriterion("FINISHED_AT <=", value, "finishedAt");
            return (Criteria) this;
        }

        public Criteria andFinishedAtIn(List<LocalDateTime> values) {
            addCriterion("FINISHED_AT in", values, "finishedAt");
            return (Criteria) this;
        }

        public Criteria andFinishedAtNotIn(List<LocalDateTime> values) {
            addCriterion("FINISHED_AT not in", values, "finishedAt");
            return (Criteria) this;
        }

        public Criteria andFinishedAtBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("FINISHED_AT between", value1, value2, "finishedAt");
            return (Criteria) this;
        }

        public Criteria andFinishedAtNotBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("FINISHED_AT not between", value1, value2, "finishedAt");
            return (Criteria) this;
        }

        public Criteria andLogPathIsNull() {
            addCriterion("LOG_PATH is null");
            return (Criteria) this;
        }

        public Criteria andLogPathIsNotNull() {
            addCriterion("LOG_PATH is not null");
            return (Criteria) this;
        }

        public Criteria andLogPathEqualTo(String value) {
            addCriterion("LOG_PATH =", value, "logPath");
            return (Criteria) this;
        }

        public Criteria andLogPathNotEqualTo(String value) {
            addCriterion("LOG_PATH <>", value, "logPath");
            return (Criteria) this;
        }

        public Criteria andLogPathGreaterThan(String value) {
            addCriterion("LOG_PATH >", value, "logPath");
            return (Criteria) this;
        }

        public Criteria andLogPathGreaterThanOrEqualTo(String value) {
            addCriterion("LOG_PATH >=", value, "logPath");
            return (Criteria) this;
        }

        public Criteria andLogPathLessThan(String value) {
            addCriterion("LOG_PATH <", value, "logPath");
            return (Criteria) this;
        }

        public Criteria andLogPathLessThanOrEqualTo(String value) {
            addCriterion("LOG_PATH <=", value, "logPath");
            return (Criteria) this;
        }

        public Criteria andLogPathLike(String value) {
            addCriterion("LOG_PATH like", value, "logPath");
            return (Criteria) this;
        }

        public Criteria andLogPathNotLike(String value) {
            addCriterion("LOG_PATH not like", value, "logPath");
            return (Criteria) this;
        }

        public Criteria andLogPathIn(List<String> values) {
            addCriterion("LOG_PATH in", values, "logPath");
            return (Criteria) this;
        }

        public Criteria andLogPathNotIn(List<String> values) {
            addCriterion("LOG_PATH not in", values, "logPath");
            return (Criteria) this;
        }

        public Criteria andLogPathBetween(String value1, String value2) {
            addCriterion("LOG_PATH between", value1, value2, "logPath");
            return (Criteria) this;
        }

        public Criteria andLogPathNotBetween(String value1, String value2) {
            addCriterion("LOG_PATH not between", value1, value2, "logPath");
            return (Criteria) this;
        }

        public Criteria andExitCodeIsNull() {
            addCriterion("EXIT_CODE is null");
            return (Criteria) this;
        }

        public Criteria andExitCodeIsNotNull() {
            addCriterion("EXIT_CODE is not null");
            return (Criteria) this;
        }

        public Criteria andExitCodeEqualTo(Integer value) {
            addCriterion("EXIT_CODE =", value, "exitCode");
            return (Criteria) this;
        }

        public Criteria andExitCodeNotEqualTo(Integer value) {
            addCriterion("EXIT_CODE <>", value, "exitCode");
            return (Criteria) this;
        }

        public Criteria andExitCodeGreaterThan(Integer value) {
            addCriterion("EXIT_CODE >", value, "exitCode");
            return (Criteria) this;
        }

        public Criteria andExitCodeGreaterThanOrEqualTo(Integer value) {
            addCriterion("EXIT_CODE >=", value, "exitCode");
            return (Criteria) this;
        }

        public Criteria andExitCodeLessThan(Integer value) {
            addCriterion("EXIT_CODE <", value, "exitCode");
            return (Criteria) this;
        }

        public Criteria andExitCodeLessThanOrEqualTo(Integer value) {
            addCriterion("EXIT_CODE <=", value, "exitCode");
            return (Criteria) this;
        }

        public Criteria andExitCodeIn(List<Integer> values) {
            addCriterion("EXIT_CODE in", values, "exitCode");
            return (Criteria) this;
        }

        public Criteria andExitCodeNotIn(List<Integer> values) {
            addCriterion("EXIT_CODE not in", values, "exitCode");
            return (Criteria) this;
        }

        public Criteria andExitCodeBetween(Integer value1, Integer value2) {
            addCriterion("EXIT_CODE between", value1, value2, "exitCode");
            return (Criteria) this;
        }

        public Criteria andExitCodeNotBetween(Integer value1, Integer value2) {
            addCriterion("EXIT_CODE not between", value1, value2, "exitCode");
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