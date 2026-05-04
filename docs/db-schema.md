# DB Schema (PostgreSQL 기준)

이 문서는 현재 공유된 PostgreSQL 스키마 기준입니다.
실행 가능한 SQL 원문은 `docs/db-schema-postgres.sql` 파일을 참고하세요.

## 1. 확장

- `CREATE EXTENSION IF NOT EXISTS vector`

## 2. 테이블 목록 (총 15개)

- `users`
- `social_account`
- `school`
- `classroom`
- `class_code`
- `student`
- `parent_student`
- `teacher_setting`
- `chat_room`
- `chat_room_user_map`
- `message`
- `ai_recommendation`
- `pdf_file`
- `ai_feedback`
- `message_analysis`

## 3. 공통 컬럼

대부분 테이블은 아래 공통 컬럼을 가집니다.

- `created_at` (NOT NULL, DEFAULT CURRENT_TIMESTAMP)
- `updated_at` (NOT NULL, DEFAULT CURRENT_TIMESTAMP)

## 4. 테이블 요약

### `users`
- PK: `id`
- 컬럼: `name`, `login_id`, `pw`, `role`

### `social_account`
- PK: `id`
- FK: `user_id -> users.id` (NOT NULL)
- 컬럼: `provider`, `provider_user_id`

### `school`
- PK: `id`
- 컬럼: `name`

### `classroom`
- PK: `id`
- FK: `school_id -> school.id` (NOT NULL)
- FK: `teacher_id -> users.id` (NOT NULL)
- 컬럼: `grade`, `class_number`

### `class_code`
- PK: `id`
- FK: `classroom_id -> classroom.id` (NOT NULL)
- 컬럼: `code`

### `student`
- PK: `id`
- FK: `classroom_id -> classroom.id` (NOT NULL)
- 컬럼: `name`, `birthday`, `gender`

### `parent_student`
- PK: `id`
- FK: `parent_id -> users.id` (NOT NULL)
- FK: `student_id -> student.id` (NOT NULL)

### `teacher_setting`
- PK: `id`
- FK: `teacher_id -> users.id` (NOT NULL)
- 컬럼: `day_of_week`, `start_time`, `end_time`

### `chat_room`
- PK: `id`
- 컬럼: `intent_label`, `status`

### `chat_room_user_map`
- PK: `id`
- FK: `chat_room_id -> chat_room.id` (NOT NULL)
- FK: `user_id -> users.id` (NOT NULL)
- 컬럼: `unread_count`, `last_read_at`

### `message`
- PK: `id`
- FK: `chat_room_id -> chat_room.id` (NOT NULL)
- FK: `sender_id -> users.id` (NOT NULL)
- 컬럼: `type`, `content`, `modify_content`
- 벡터/유사도: `content_embedding (VECTOR)`, `modify_content_embedding (VECTOR)`, `similarity_original`, `similarity_modified`
- 분쟁위험: `is_dispute_risk`

### `ai_recommendation`
- PK: `id`
- FK: `message_id -> message.id` (NOT NULL)
- 컬럼: `content`, `embedding (VECTOR)`, `is_recommendation_used`

### `pdf_file`
- PK: `id`
- FK: `chat_room_id -> chat_room.id` (NOT NULL)
- 컬럼: `file_url`, `file_name`

### `ai_feedback`
- PK: `id`
- FK: `message_analysis_id -> message_analysis.id` (NOT NULL)
- 컬럼: `type`, `actual_risk_score`

### `message_analysis`
- PK: `id`
- FK: `chat_room_id -> chat_room.id` (NOT NULL)
- FK: `teacher_id -> users.id` (NOT NULL)
- FK: `used_message_id -> message.id` (NULL)
- 컬럼: `original_content`, `risk_level`, `recommended_message`, `is_recommendation_adopted`, `expires_at`

## 5. 관계 요약

- `users (1) - (N) social_account`
- `users (1) - (N) classroom` (`teacher_id`)
- `users (1) - (N) teacher_setting` (`teacher_id`)
- `users (1) - (N) chat_room_user_map`
- `users (1) - (N) message` (`sender_id`)
- `school (1) - (N) classroom`
- `classroom (1) - (N) class_code`
- `classroom (1) - (N) student`
- `users (1) - (N) parent_student` (`parent_id`)
- `student (1) - (N) parent_student`
- `chat_room (1) - (N) chat_room_user_map`
- `chat_room (1) - (N) message`
- `chat_room (1) - (N) pdf_file`
- `chat_room (1) - (N) message_analysis`
- `message (1) - (N) message_analysis` (`used_message_id`)
- `message (1) - (N) ai_recommendation`
- `message_analysis (1) - (N) ai_feedback`
- `users (1) - (N) message_analysis` (`teacher_id`)

## 6. 참고

- 애플리케이션 설정이 `spring.jpa.hibernate.ddl-auto=validate` 인 경우, 애플리케이션은 테이블을 생성하지 않고 사전 준비된 스키마를 검증합니다.
