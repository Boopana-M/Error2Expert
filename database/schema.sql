-- CodeMemory Database Schema (PostgreSQL)
-- Matches JPA Entities in com.codememory.entity

-- Drop tables if they exist (in dependency order)
DROP TABLE IF EXISTS recommendations CASCADE;
DROP TABLE IF EXISTS weaknesses CASCADE;
DROP TABLE IF EXISTS mistakes CASCADE;
DROP TABLE IF EXISTS submissions CASCADE;
DROP TABLE IF EXISTS problem_topics CASCADE;
DROP TABLE IF EXISTS problems CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 1. Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 2. Problems Table
CREATE TABLE problems (
    id BIGSERIAL PRIMARY KEY,
    leetcode_id VARCHAR(50),
    title VARCHAR(255) NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    url VARCHAR(500)
);

-- 2b. Problem Topics Table
CREATE TABLE problem_topics (
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    topic VARCHAR(100) NOT NULL,
    PRIMARY KEY (problem_id, topic)
);

-- 3. Submissions Table
CREATE TABLE submissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    code TEXT,
    language VARCHAR(50),
    result VARCHAR(50) NOT NULL,
    attempt_number INT,
    submitted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_submissions_user_id ON submissions(user_id);
CREATE INDEX idx_submissions_problem_id ON submissions(problem_id);
CREATE INDEX idx_submissions_result ON submissions(result);

-- 4. Mistakes Table
CREATE TABLE mistakes (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    category VARCHAR(60) NOT NULL,
    user_explanation TEXT,
    ai_explanation TEXT,
    confidence DOUBLE PRECISION,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mistakes_submission_id ON mistakes(submission_id);
CREATE INDEX idx_mistakes_category ON mistakes(category);

-- 5. Weaknesses Table
CREATE TABLE weaknesses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    concept VARCHAR(255) NOT NULL,
    mistake_category VARCHAR(60) NOT NULL,
    frequency INT NOT NULL DEFAULT 1,
    confidence DOUBLE PRECISION,
    mastery_score DOUBLE PRECISION DEFAULT 100.0,
    first_observed TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    last_observed TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_concept_mistake UNIQUE (user_id, concept, mistake_category)
);

CREATE INDEX idx_weaknesses_user_id ON weaknesses(user_id);

-- 6. Recommendations Table
CREATE TABLE recommendations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    weakness_id BIGINT REFERENCES weaknesses(id) ON DELETE SET NULL,
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    reason TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recommendations_user_id ON recommendations(user_id);
CREATE INDEX idx_recommendations_status ON recommendations(status);
