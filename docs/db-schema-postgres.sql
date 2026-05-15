-- Softy DB Schema (PostgreSQL)
-- Current application-aligned schema

CREATE EXTENSION IF NOT EXISTS vector;

-- 1. USERS
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    login_id VARCHAR(50),
    pw TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. USER_ROLE
CREATE TABLE user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_role_user_id_role
        UNIQUE (user_id, role),
    CONSTRAINT chk_user_role_role
        CHECK (role IN ('TEACHER', 'PARENT', 'ADMIN'))
);

CREATE INDEX idx_user_role_user_id ON user_role(user_id);
CREATE INDEX idx_user_role_role ON user_role(role);

-- 3. SOCIAL_ACCOUNT
CREATE TABLE social_account (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 4. SCHOOL
CREATE TABLE school (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. CLASSROOM
CREATE TABLE classroom (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL,
    grade INT NOT NULL,
    class_number INT NOT NULL,
    teacher_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (school_id) REFERENCES school(id),
    FOREIGN KEY (teacher_id) REFERENCES users(id)
);

-- 6. CLASS_CODE
CREATE TABLE class_code (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    classroom_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (classroom_id) REFERENCES classroom(id)
);

-- 7. STUDENT
CREATE TABLE student (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    birthday DATE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    classroom_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (classroom_id) REFERENCES classroom(id)
);

-- 8. PARENT_STUDENT
CREATE TABLE parent_student (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES users(id),
    FOREIGN KEY (student_id) REFERENCES student(id)
);

-- 9. TEACHER_SETTING
CREATE TABLE teacher_setting (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    day_of_week SMALLINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (teacher_id) REFERENCES users(id)
);

-- 10. CHAT_ROOM
CREATE TABLE chat_room (
    id BIGSERIAL PRIMARY KEY,
    intent_label VARCHAR(50),
    status VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. CHAT_ROOM_USER_MAP
CREATE TABLE chat_room_user_map (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    participant_role VARCHAR(30) NOT NULL,
    unread_count INT NOT NULL,
    last_read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_room_id) REFERENCES chat_room(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_chat_room_user_map_participant_role
        CHECK (participant_role IN ('TEACHER', 'PARENT'))
);

CREATE INDEX idx_chat_room_user_map_chat_room_id_role
    ON chat_room_user_map(chat_room_id, participant_role);

CREATE INDEX idx_chat_room_user_map_user_id_role
    ON chat_room_user_map(user_id, participant_role);

-- 12. MESSAGE
CREATE TABLE message (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    modify_content TEXT,
    content_embedding VECTOR,
    modify_content_embedding VECTOR,
    similarity_original FLOAT,
    similarity_modified FLOAT,
    is_dispute_risk BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_room_id) REFERENCES chat_room(id),
    FOREIGN KEY (sender_id) REFERENCES users(id)
);

-- 13. AI_RECOMMENDATION
CREATE TABLE ai_recommendation (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR,
    is_recommendation_used BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES message(id)
);

-- 14. PDF_FILE
CREATE TABLE pdf_file (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    file_url TEXT NOT NULL,
    file_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_room_id) REFERENCES chat_room(id),
    FOREIGN KEY (teacher_id) REFERENCES users(id)
);

-- 15. MESSAGE_ANALYSIS
CREATE TABLE message_analysis (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    original_content TEXT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    recommended_message TEXT,
    is_recommendation_adopted BOOLEAN NOT NULL DEFAULT FALSE,
    used_message_id BIGINT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_room_id) REFERENCES chat_room(id),
    FOREIGN KEY (teacher_id) REFERENCES users(id),
    FOREIGN KEY (used_message_id) REFERENCES message(id)
);

-- 16. AI_FEEDBACK
CREATE TABLE ai_feedback (
    id BIGSERIAL PRIMARY KEY,
    message_analysis_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    actual_risk_score INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_analysis_id) REFERENCES message_analysis(id)
);
