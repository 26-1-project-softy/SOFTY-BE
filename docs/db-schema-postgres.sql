-- Softy DB Schema (PostgreSQL)
-- Source of truth provided by team (2026-04-17)

-- 확장 (VECTOR 사용)
CREATE EXTENSION IF NOT EXISTS vector;

-- 1. USER
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    login_id VARCHAR(50),
    pw TEXT,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. SOCIAL_ACCOUNT
CREATE TABLE social_account (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 3. SCHOOL
CREATE TABLE school (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. CLASSROOM
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

-- 5. CLASS_CODE
CREATE TABLE class_code (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    classroom_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (classroom_id) REFERENCES classroom(id)
);

-- 6. STUDENT
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

-- 7. PARENT_STUDENT
CREATE TABLE parent_student (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES users(id),
    FOREIGN KEY (student_id) REFERENCES student(id)
);

-- 8. TEACHER_SETTING
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

-- 9. CHAT_ROOM
CREATE TABLE chat_room (
    id BIGSERIAL PRIMARY KEY,
    intent_label VARCHAR(50),
    status VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. CHAT_ROOM_USER_MAP
CREATE TABLE chat_room_user_map (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    unread_count INT NOT NULL,
    last_read_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_room_id) REFERENCES chat_room(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 11. MESSAGE
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

-- 12. AI_RECOMMENDATION
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

-- 13. PDF_FILE
CREATE TABLE pdf_file (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,
    file_url TEXT NOT NULL,
    file_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_room_id) REFERENCES chat_room(id)
);

-- 14. model_training_history
CREATE TABLE model_training_history (
    id BIGSERIAL PRIMARY KEY,
    jobId VARCHAR,
    evaluation_id VARCHAR,
    trained_at TIMESTAMP NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    dataset_version VARCHAR(50) NOT NULL,
    f1_score DOUBLE PRECISION,
    status VARCHAR(20) NOT NULL,
    is_deployed BOOLEAN DEFAULT FALSE,
    model_path TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 15. AI_FEEDBACK
CREATE TABLE ai_feedback (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    actual_risk_score INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES message(id)
);
