-- Create rooms table for hospital room management
CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,  -- PRIVATE, SHARED
    capacity INT NOT NULL DEFAULT 1 CHECK (capacity >= 1),
    floor VARCHAR(20),
    wing VARCHAR(50),
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_rooms_deleted_at ON rooms(deleted_at);
CREATE INDEX idx_rooms_type ON rooms(type);
CREATE INDEX idx_rooms_floor ON rooms(floor);

-- Seed some default rooms for testing
INSERT INTO rooms (name, type, capacity, floor, wing, created_at, updated_at) VALUES
('101', 'PRIVATE', 1, '1', 'A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('102', 'PRIVATE', 1, '1', 'A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('103', 'SHARED', 2, '1', 'B', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('201', 'PRIVATE', 1, '2', 'A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('202', 'SHARED', 3, '2', 'B', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('203', 'SHARED', 4, '2', 'B', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
