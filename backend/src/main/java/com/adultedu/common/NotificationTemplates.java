package com.adultedu.common;

/**
 * 站内信模板编码，取值与 db/data.sql 中 notification_template 的播种数据一一对应。
 */
public final class NotificationTemplates {
    public static final String PROJECT_JOIN = "project_join";
    public static final String PROJECT_REJOIN = "project_rejoin";
    public static final String PROJECT_OPEN = "project_open";
    public static final String TEACHER_ASSIGN = "teacher_assign";
    public static final String COURSE_PUBLISH = "course_publish";
    public static final String STUDY_FINISHED = "study_finished";
    public static final String STUDY_WARNING = "study_warning";
    public static final String STUDY_ABNORMAL = "study_abnormal";
    public static final String EXAM_PASSED = "exam_passed";
    public static final String CREDIT_GRANTED = "credit_granted";
    public static final String CREDIT_REVOKED = "credit_revoked";
    public static final String CREDIT_APPROVED = "credit_approved";
    public static final String CREDIT_REJECTED = "credit_rejected";
    public static final String CREDIT_TODO = "credit_todo";
    public static final String GRADUATE_NODE_TEACHER = "graduate_node_teacher";
    public static final String GRADUATE_NODE_LEADER = "graduate_node_leader";
    public static final String GRADUATE_APPROVED = "graduate_approved";
    public static final String GRADUATE_REJECTED = "graduate_rejected";
    public static final String VERIFY_REVIEWED = "verify_reviewed";
    public static final String ACCOUNT_CREATED = "account_created";
    public static final String PASSWORD_RESET = "password_reset";
    public static final String APPROVAL_TIMEOUT = "approval_timeout";

    private NotificationTemplates() {
    }
}
