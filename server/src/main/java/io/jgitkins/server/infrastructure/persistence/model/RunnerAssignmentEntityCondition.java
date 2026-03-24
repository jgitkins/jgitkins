package io.jgitkins.server.infrastructure.persistence.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RunnerAssignmentEntityCondition {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public RunnerAssignmentEntityCondition() {
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

        public Criteria andTargetTypeIsNull() {
            addCriterion("TARGET_TYPE is null");
            return (Criteria) this;
        }

        public Criteria andTargetTypeIsNotNull() {
            addCriterion("TARGET_TYPE is not null");
            return (Criteria) this;
        }

        public Criteria andTargetTypeEqualTo(String value) {
            addCriterion("TARGET_TYPE =", value, "targetType");
            return (Criteria) this;
        }

        public Criteria andTargetTypeNotEqualTo(String value) {
            addCriterion("TARGET_TYPE <>", value, "targetType");
            return (Criteria) this;
        }

        public Criteria andTargetTypeGreaterThan(String value) {
            addCriterion("TARGET_TYPE >", value, "targetType");
            return (Criteria) this;
        }

        public Criteria andTargetTypeGreaterThanOrEqualTo(String value) {
            addCriterion("TARGET_TYPE >=", value, "targetType");
            return (Criteria) this;
        }

        public Criteria andTargetTypeLessThan(String value) {
            addCriterion("TARGET_TYPE <", value, "targetType");
            return (Criteria) this;
        }

        public Criteria andTargetTypeLessThanOrEqualTo(String value) {
            addCriterion("TARGET_TYPE <=", value, "targetType");
            return (Criteria) this;
        }

        public Criteria andTargetTypeLike(String value) {
            addCriterion("TARGET_TYPE like", value, "targetType");
            return (Criteria) this;
        }

        public Criteria andTargetTypeNotLike(String value) {
            addCriterion("TARGET_TYPE not like", value, "targetType");
            return (Criteria) this;
        }

        public Criteria andTargetTypeIn(List<String> values) {
            addCriterion("TARGET_TYPE in", values, "targetType");
            return (Criteria) this;
        }

        public Criteria andTargetTypeNotIn(List<String> values) {
            addCriterion("TARGET_TYPE not in", values, "targetType");
            return (Criteria) this;
        }

        public Criteria andTargetTypeBetween(String value1, String value2) {
            addCriterion("TARGET_TYPE between", value1, value2, "targetType");
            return (Criteria) this;
        }

        public Criteria andTargetTypeNotBetween(String value1, String value2) {
            addCriterion("TARGET_TYPE not between", value1, value2, "targetType");
            return (Criteria) this;
        }

        public Criteria andTargetIdIsNull() {
            addCriterion("TARGET_ID is null");
            return (Criteria) this;
        }

        public Criteria andTargetIdIsNotNull() {
            addCriterion("TARGET_ID is not null");
            return (Criteria) this;
        }

        public Criteria andTargetIdEqualTo(Long value) {
            addCriterion("TARGET_ID =", value, "targetId");
            return (Criteria) this;
        }

        public Criteria andTargetIdNotEqualTo(Long value) {
            addCriterion("TARGET_ID <>", value, "targetId");
            return (Criteria) this;
        }

        public Criteria andTargetIdGreaterThan(Long value) {
            addCriterion("TARGET_ID >", value, "targetId");
            return (Criteria) this;
        }

        public Criteria andTargetIdGreaterThanOrEqualTo(Long value) {
            addCriterion("TARGET_ID >=", value, "targetId");
            return (Criteria) this;
        }

        public Criteria andTargetIdLessThan(Long value) {
            addCriterion("TARGET_ID <", value, "targetId");
            return (Criteria) this;
        }

        public Criteria andTargetIdLessThanOrEqualTo(Long value) {
            addCriterion("TARGET_ID <=", value, "targetId");
            return (Criteria) this;
        }

        public Criteria andTargetIdIn(List<Long> values) {
            addCriterion("TARGET_ID in", values, "targetId");
            return (Criteria) this;
        }

        public Criteria andTargetIdNotIn(List<Long> values) {
            addCriterion("TARGET_ID not in", values, "targetId");
            return (Criteria) this;
        }

        public Criteria andTargetIdBetween(Long value1, Long value2) {
            addCriterion("TARGET_ID between", value1, value2, "targetId");
            return (Criteria) this;
        }

        public Criteria andTargetIdNotBetween(Long value1, Long value2) {
            addCriterion("TARGET_ID not between", value1, value2, "targetId");
            return (Criteria) this;
        }

        public Criteria andAssignedAtIsNull() {
            addCriterion("ASSIGNED_AT is null");
            return (Criteria) this;
        }

        public Criteria andAssignedAtIsNotNull() {
            addCriterion("ASSIGNED_AT is not null");
            return (Criteria) this;
        }

        public Criteria andAssignedAtEqualTo(LocalDateTime value) {
            addCriterion("ASSIGNED_AT =", value, "assignedAt");
            return (Criteria) this;
        }

        public Criteria andAssignedAtNotEqualTo(LocalDateTime value) {
            addCriterion("ASSIGNED_AT <>", value, "assignedAt");
            return (Criteria) this;
        }

        public Criteria andAssignedAtGreaterThan(LocalDateTime value) {
            addCriterion("ASSIGNED_AT >", value, "assignedAt");
            return (Criteria) this;
        }

        public Criteria andAssignedAtGreaterThanOrEqualTo(LocalDateTime value) {
            addCriterion("ASSIGNED_AT >=", value, "assignedAt");
            return (Criteria) this;
        }

        public Criteria andAssignedAtLessThan(LocalDateTime value) {
            addCriterion("ASSIGNED_AT <", value, "assignedAt");
            return (Criteria) this;
        }

        public Criteria andAssignedAtLessThanOrEqualTo(LocalDateTime value) {
            addCriterion("ASSIGNED_AT <=", value, "assignedAt");
            return (Criteria) this;
        }

        public Criteria andAssignedAtIn(List<LocalDateTime> values) {
            addCriterion("ASSIGNED_AT in", values, "assignedAt");
            return (Criteria) this;
        }

        public Criteria andAssignedAtNotIn(List<LocalDateTime> values) {
            addCriterion("ASSIGNED_AT not in", values, "assignedAt");
            return (Criteria) this;
        }

        public Criteria andAssignedAtBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("ASSIGNED_AT between", value1, value2, "assignedAt");
            return (Criteria) this;
        }

        public Criteria andAssignedAtNotBetween(LocalDateTime value1, LocalDateTime value2) {
            addCriterion("ASSIGNED_AT not between", value1, value2, "assignedAt");
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