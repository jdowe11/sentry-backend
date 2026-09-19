DROP TABLE IF EXISTS users CASCADE;

--- Table structure for users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    display_name VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS friend_requests CASCADE;

--- Table structure for friend_requests
CREATE TABLE friend_requests (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('pending', 'accepted', 'declined', 'cancelled')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_sender_receiver UNIQUE(sender_id, receiver_id),
    CONSTRAINT check_sender_receiver_different CHECK(sender_id <> receiver_id)
);

DROP TABLE IF EXISTS friendships CASCADE;

--- Table structure for friendships
CREATE TABLE friendships (
    user_id_1 BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_id_2 BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id_1, user_id_2),

    CONSTRAINT check_user_order CHECK(user_id_1 < user_id_2)
);

CREATE INDEX idx_friendships_user_id_2 ON friendships(user_id_2);

DROP TABLE IF EXISTS messages CASCADE;
DROP TABLE IF EXISTS chat_participants CASCADE;
DROP TABLE IF EXISTS chats CASCADE;

--- Table structure for chats
CREATE TABLE chats (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--- Table structure for chat_participants
CREATE TABLE chat_participants (
    chat_id BIGINT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (chat_id, user_id)
);

CREATE INDEX idx_chat_participants_user_id ON chat_participants(user_id);

--- Table structure for messages
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ciphertext TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_chat_id_created_at ON messages(chat_id, created_at DESC);
