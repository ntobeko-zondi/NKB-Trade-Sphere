<?php
/**
 * NKB Trade Sphere API
 * Upload this file as: https://wmc.ms.wits.ac.za/students/sgroup2729/api.php
 * Connected PostgreSQL account: sgroup2729 / dgroup2729
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

const DB_HOSTS = ['localhost', '127.0.0.1', 'courses.ms.wits.ac.za'];
const DB_PORT = '5432';
const DB_NAME = 'dgroup2729';
const DB_USER = 'sgroup2729';
const DB_PASS = '82814b9da7599ffcbd9d';

function respond(array $payload, int $code = 200): void {
    http_response_code($code);
    echo json_encode($payload, JSON_UNESCAPED_SLASHES);
    exit;
}

function debug_enabled(): bool {
    return (isset($_GET['debug']) && $_GET['debug'] === '1') || (isset($_POST['debug']) && $_POST['debug'] === '1');
}

function fail(string $message, int $code = 400, string $detail = ''): void {

    $payload = ['success' => false, 'error' => $message, 'http_code' => $code];
    if ($detail !== '' && debug_enabled()) {
        $payload['detail'] = $detail;
    }
    respond($payload, 200);
}

function ping(): void {
    respond([
        'success' => true,
        'message' => 'API reachable',
        'php_version' => PHP_VERSION,
        'method' => $_SERVER['REQUEST_METHOD'] ?? 'unknown'
    ]);
}

function db_test(): void {
    $db = pdo();
    $stmt = $db->query('SELECT current_database() AS database_name, current_user AS db_user, NOW() AS server_time');
    $row = $stmt->fetch();
    $tables = $db->query("SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename")->fetchAll(PDO::FETCH_COLUMN);
    $required = ['users', 'listings', 'messages', 'saved_items', 'ratings', 'listing_ratings'];
    $missing = array_values(array_diff($required, $tables));
    if (!empty($missing)) {
        fail('Database reachable but required tables are missing. Run schema.sql once.', 500, 'Missing: ' . implode(', ', $missing));
    }
    respond(['success' => true, 'message' => 'Database reachable and schema ready', 'database' => $row, 'tables' => $tables]);
}

function ensure_schema(PDO $db): void {
    static $done = false;
    if ($done) {
        return;
    }

    $statements = [
        "CREATE TABLE IF NOT EXISTS users (
            id BIGSERIAL PRIMARY KEY,
            user_id TEXT UNIQUE,
            email TEXT UNIQUE NOT NULL,
            password_hash TEXT,
            full_name TEXT,
            phone TEXT,
            id_number_hash TEXT,
            average_rating REAL NOT NULL DEFAULT 0 CHECK (average_rating >= 0 AND average_rating <= 5),
            external_auth_id TEXT,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        )",
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS user_id TEXT",
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS email TEXT",
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash TEXT",
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name TEXT",
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS phone TEXT",
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS id_number_hash TEXT",
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS average_rating REAL NOT NULL DEFAULT 0",
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS external_auth_id TEXT",
        "ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP",
        "ALTER TABLE users DROP COLUMN IF EXISTS id_number",
        "UPDATE users SET user_id = email WHERE (user_id IS NULL OR user_id = '') AND email IS NOT NULL",

        "CREATE TABLE IF NOT EXISTS listings (
            listing_id BIGSERIAL PRIMARY KEY,
            seller_id TEXT NOT NULL,
            title TEXT NOT NULL,
            category TEXT NOT NULL,
            condition TEXT,
            description TEXT,
            price TEXT NOT NULL,
            image_url TEXT,
            quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity >= 0),
            average_rating REAL NOT NULL DEFAULT 0 CHECK (average_rating >= 0 AND average_rating <= 5),
            status TEXT NOT NULL DEFAULT 'active',
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        )",
        "ALTER TABLE listings ADD COLUMN IF NOT EXISTS seller_id TEXT",
        "ALTER TABLE listings ADD COLUMN IF NOT EXISTS title TEXT",
        "ALTER TABLE listings ADD COLUMN IF NOT EXISTS category TEXT",
        "ALTER TABLE listings ADD COLUMN IF NOT EXISTS condition TEXT",
        "ALTER TABLE listings ADD COLUMN IF NOT EXISTS description TEXT",
        "ALTER TABLE listings ADD COLUMN IF NOT EXISTS price TEXT",
        "ALTER TABLE listings ADD COLUMN IF NOT EXISTS image_url TEXT",
        "ALTER TABLE listings ADD COLUMN IF NOT EXISTS quantity INTEGER NOT NULL DEFAULT 1",
        "ALTER TABLE listings ADD COLUMN IF NOT EXISTS average_rating REAL NOT NULL DEFAULT 0",
        "ALTER TABLE listings ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'active'",
        "ALTER TABLE listings ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP",

        "CREATE TABLE IF NOT EXISTS messages (
            message_id BIGSERIAL PRIMARY KEY,
            sender_id TEXT NOT NULL,
            receiver_id TEXT NOT NULL,
            listing_id BIGINT,
            message_text TEXT NOT NULL,
            is_read BOOLEAN NOT NULL DEFAULT false,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        )",
        "ALTER TABLE messages ADD COLUMN IF NOT EXISTS sender_id TEXT",
        "ALTER TABLE messages ADD COLUMN IF NOT EXISTS receiver_id TEXT",
        "ALTER TABLE messages ADD COLUMN IF NOT EXISTS listing_id BIGINT",
        "ALTER TABLE messages ADD COLUMN IF NOT EXISTS message_text TEXT",
        "ALTER TABLE messages ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT false",
        "ALTER TABLE messages ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP",

        "CREATE TABLE IF NOT EXISTS saved_items (
            save_id BIGSERIAL PRIMARY KEY,
            user_id TEXT NOT NULL,
            listing_id BIGINT NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        )",
        "DO $$ BEGIN IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='saved_items' AND column_name='saved_listing_id') AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='saved_items' AND column_name='listing_id') THEN ALTER TABLE saved_items RENAME COLUMN saved_listing_id TO listing_id; END IF; END $$",
        "ALTER TABLE saved_items ADD COLUMN IF NOT EXISTS user_id TEXT",
        "ALTER TABLE saved_items ADD COLUMN IF NOT EXISTS listing_id BIGINT",
        "ALTER TABLE saved_items ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP",

        "CREATE TABLE IF NOT EXISTS ratings (
            rating_id BIGSERIAL PRIMARY KEY,
            rater_id TEXT NOT NULL,
            rated_id TEXT NOT NULL,
            rating REAL NOT NULL CHECK (rating >= 1 AND rating <= 5),
            comment TEXT,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        )",
        "ALTER TABLE ratings ADD COLUMN IF NOT EXISTS rater_id TEXT",
        "ALTER TABLE ratings ADD COLUMN IF NOT EXISTS rated_id TEXT",
        "ALTER TABLE ratings ADD COLUMN IF NOT EXISTS rating REAL",
        "ALTER TABLE ratings ADD COLUMN IF NOT EXISTS comment TEXT",
        "ALTER TABLE ratings ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP",

        "CREATE TABLE IF NOT EXISTS listing_ratings (
            rating_id BIGSERIAL PRIMARY KEY,
            listing_id BIGINT NOT NULL,
            buyer_id TEXT NOT NULL,
            rating REAL NOT NULL CHECK (rating >= 1 AND rating <= 5),
            comment TEXT,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        )",
        "ALTER TABLE listing_ratings ADD COLUMN IF NOT EXISTS listing_id BIGINT",
        "ALTER TABLE listing_ratings ADD COLUMN IF NOT EXISTS buyer_id TEXT",
        "ALTER TABLE listing_ratings ADD COLUMN IF NOT EXISTS rating REAL",
        "ALTER TABLE listing_ratings ADD COLUMN IF NOT EXISTS comment TEXT",
        "ALTER TABLE listing_ratings ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP",

        "CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users (LOWER(email))",
        "CREATE INDEX IF NOT EXISTS idx_users_user_id_lower ON users (LOWER(user_id)) WHERE user_id IS NOT NULL",
        "CREATE INDEX IF NOT EXISTS idx_listings_seller_id ON listings(seller_id)",
        "CREATE INDEX IF NOT EXISTS idx_listings_category ON listings(category)",
        "CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages(sender_id)",
        "CREATE INDEX IF NOT EXISTS idx_messages_receiver_id ON messages(receiver_id)",
        "DELETE FROM saved_items a USING saved_items b WHERE a.ctid < b.ctid AND a.user_id = b.user_id AND a.listing_id = b.listing_id",
        "CREATE UNIQUE INDEX IF NOT EXISTS uq_saved_items_user_listing_idx ON saved_items(user_id, listing_id)",
        "CREATE INDEX IF NOT EXISTS idx_saved_items_user_id ON saved_items(user_id)",
        "DELETE FROM listing_ratings a USING listing_ratings b WHERE a.ctid < b.ctid AND a.listing_id = b.listing_id AND a.buyer_id = b.buyer_id",
        "CREATE UNIQUE INDEX IF NOT EXISTS uq_listing_ratings_listing_buyer_idx ON listing_ratings(listing_id, buyer_id)",
        "DROP VIEW IF EXISTS v_listing_rating_summary CASCADE",
        "DROP VIEW IF EXISTS v_listing_details CASCADE",
        "CREATE OR REPLACE VIEW v_listing_details AS
            SELECT l.listing_id, l.seller_id, COALESCE(u.full_name, l.seller_id) AS seller_name,
                   l.title, l.category, l.condition, l.description, l.price, l.image_url,
                   l.quantity, l.status, ROUND(l.average_rating::numeric, 1) AS average_rating, l.created_at
            FROM listings l
            LEFT JOIN users u ON LOWER(u.email)=LOWER(l.seller_id)",
        "CREATE OR REPLACE VIEW v_listing_rating_summary AS
            SELECT l.listing_id, COUNT(r.rating_id) AS rating_count, ROUND(COALESCE(AVG(r.rating), 0)::numeric, 1) AS average_rating
            FROM listings l
            LEFT JOIN listing_ratings r ON r.listing_id = l.listing_id
            GROUP BY l.listing_id"
    ];

    foreach ($statements as $sql) {
        $db->exec($sql);
    }

    $done = true;
}

function pdo(): PDO {
    static $pdo = null;
    if ($pdo instanceof PDO) {
        return $pdo;
    }

    if (!extension_loaded('pdo_pgsql')) {
        fail('Database connection failed', 500, 'PHP extension pdo_pgsql is not enabled on this server. Ask the lab/tutor to enable PostgreSQL PDO, or use a PHP server that supports PostgreSQL.');
    }

    $errors = [];
    foreach (DB_HOSTS as $host) {
        $dsn = 'pgsql:host=' . $host . ';port=' . DB_PORT . ';dbname=' . DB_NAME . ';connect_timeout=5';
        try {
            $candidate = new PDO($dsn, DB_USER, DB_PASS, [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES => false,
            ]);
            // Do not run schema/table/view DDL automatically on every app request.
            // Running DROP/CREATE VIEW during normal message refreshes caused intermittent
            // "failed to load messages" errors when multiple API calls arrived together.
            // The schema must be installed once using schema.sql; dbTest checks it.
            $pdo = $candidate;
            return $pdo;
        } catch (PDOException $e) {
            $errors[] = $host . ': ' . $e->getMessage();
        }
    }

    fail('Database connection failed', 500, implode(' | ', $errors));
}

function input(string $key, $default = ''): string {
    if (isset($_POST[$key])) {
        return is_string($_POST[$key]) ? trim($_POST[$key]) : trim((string)$_POST[$key]);
    }
    if (isset($_GET[$key])) {
        return is_string($_GET[$key]) ? trim($_GET[$key]) : trim((string)$_GET[$key]);
    }
    static $json = null;
    if ($json === null) {
        $raw = file_get_contents('php://input');
        $json = json_decode($raw ?: '{}', true);
        if (!is_array($json)) {
            $json = [];
        }
    }
    return array_key_exists($key, $json) ? trim((string)$json[$key]) : (string)$default;
}

function normalize_email(string $email): string {
    return strtolower(trim($email));
}

function normalize_phone(string $phone): string {
    return preg_replace('/\s+/', '', trim($phone));
}

function normalize_id_number(string $id): string {
    return preg_replace('/\D+/', '', trim($id));
}

function round_rating_value($value): float {
    return round((float)($value ?: 0), 1);
}

function normalize_rating_fields($data) {
    if (is_array($data)) {
        foreach (['average_rating', 'seller_rating', 'rating'] as $key) {
            if (array_key_exists($key, $data) && is_numeric($data[$key])) {
                $data[$key] = round_rating_value($data[$key]);
            }
        }
    }
    return $data;
}

function require_fields(array $keys): void {
    foreach ($keys as $key) {
        if (input($key) === '') {
            fail("Missing required field: $key");
        }
    }
}

function fetch_listing(PDO $db, int $listingId): ?array {
    $stmt = $db->prepare(
        "SELECT l.*, COALESCE(u.full_name, l.seller_id) AS seller_name, u.average_rating AS seller_rating
         FROM listings l
         LEFT JOIN users u ON u.email = l.seller_id
         WHERE l.listing_id = :listing_id"
    );
    $stmt->execute([':listing_id' => $listingId]);
    $row = $stmt->fetch();
    return $row ?: null;
}

function listing_select_sql(): string {
    return "SELECT l.*, COALESCE(u.full_name, l.seller_id) AS seller_name, u.average_rating AS seller_rating
            FROM listings l
            LEFT JOIN users u ON u.email = l.seller_id";
}

function register_user(): void {
    require_fields(['email', 'password', 'full_name']);

    $email = normalize_email(input('email'));
    $userId = normalize_email(input('user_id', $email));
    if ($userId === '') {
        $userId = $email;
    }
    $password = input('password');
    $fullName = input('full_name');
    $phone = normalize_phone(input('phone'));
    $idNumber = normalize_id_number(input('id_number'));

    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        fail('Invalid email address');
    }
    if (strlen($password) < 8) {
        fail('Password must be at least 8 characters');
    }

    $db = pdo();
    $hash = password_hash($password, PASSWORD_DEFAULT);
    // South African ID is never stored in plain text. It is stored only as a one-way hash.
    $idHash = $idNumber !== '' ? password_hash($idNumber, PASSWORD_DEFAULT) : null;

    try {
        $stmt = $db->prepare(
            "INSERT INTO users (user_id, email, password_hash, full_name, phone, id_number_hash)
             VALUES (:user_id, :email, :password_hash, :full_name, :phone, :id_number_hash)
             RETURNING id, user_id, email, full_name, phone, average_rating, created_at"
        );
        $stmt->execute([
            ':user_id' => $userId,
            ':email' => $email,
            ':password_hash' => $hash,
            ':full_name' => $fullName,
            ':phone' => $phone,
            ':id_number_hash' => $idHash,
        ]);
        $user = $stmt->fetch();
        respond(['success' => true, 'user' => $user, 'id' => (int)$user['id']]);
    } catch (PDOException $e) {
        if ($e->getCode() === '23505') {
            fail('Email already exists');
        }
        fail('Registration failed', 500, $e->getMessage());
    }
}

function authenticate_user(): void {
    require_fields(['user_id', 'password']);
    $login = normalize_email(input('user_id'));
    $password = input('password');

    $stmt = pdo()->prepare(
        "SELECT id, user_id, email, password_hash, full_name, phone, average_rating, created_at
         FROM users
         WHERE LOWER(email) = :login OR LOWER(user_id) = :login
         LIMIT 1"
    );
    $stmt->execute([':login' => $login]);
    $user = $stmt->fetch();

    if (!$user || !password_verify($password, $user['password_hash'])) {
        fail('Invalid email or password', 401);
    }

    unset($user['password_hash']);
    respond(['success' => true, 'user' => $user]);
}

function get_user_details(): void {
    require_fields(['user_id']);
    $userId = normalize_email(input('user_id'));

    $stmt = pdo()->prepare(
        "SELECT id, user_id, email, full_name, phone, average_rating, created_at
         FROM users
         WHERE LOWER(email) = :user_id OR LOWER(user_id) = :user_id
         LIMIT 1"
    );
    $stmt->execute([':user_id' => $userId]);
    $user = $stmt->fetch();
    if (!$user) {
        fail('User not found', 404);
    }
    respond(['success' => true, 'user' => $user]);
}

function update_user_profile(): void {
    require_fields(['user_id', 'full_name']);
    $userId = normalize_email(input('user_id'));
    $email = normalize_email(input('email', $userId));
    $fullName = input('full_name');
    $phone = normalize_phone(input('phone'));

    $stmt = pdo()->prepare(
        "UPDATE users
         SET email = :email, full_name = :full_name, phone = :phone
         WHERE LOWER(email) = :user_id OR LOWER(user_id) = :user_id
         RETURNING id, user_id, email, full_name, phone, average_rating, created_at"
    );
    $stmt->execute([
        ':email' => $email,
        ':full_name' => $fullName,
        ':phone' => $phone,
        ':user_id' => $userId,
    ]);
    $user = $stmt->fetch();
    if (!$user) {
        fail('User not found', 404);
    }
    respond(['success' => true, 'user' => $user]);
}

function get_user_full_name(): void {
    require_fields(['user_id']);
    $userId = normalize_email(input('user_id'));
    $stmt = pdo()->prepare("SELECT full_name FROM users WHERE LOWER(email)=:u OR LOWER(user_id)=:u LIMIT 1");
    $stmt->execute([':u' => $userId]);
    $name = $stmt->fetchColumn();
    respond(['success' => true, 'full_name' => $name ?: $userId]);
}

function verify_reset_details(): void {
    require_fields(['email', 'id_number', 'phone']);
    $email = normalize_email(input('email'));
    $idNumber = normalize_id_number(input('id_number'));
    $phone = normalize_phone(input('phone'));

    $stmt = pdo()->prepare(
        "SELECT id_number_hash, phone FROM users WHERE LOWER(email)=:email LIMIT 1"
    );
    $stmt->execute([':email' => $email]);
    $user = $stmt->fetch();
    if (!$user) {
        fail('No account found with this email', 404);
    }
    if (empty($user['id_number_hash']) || empty($user['phone'])) {
        fail('Reset details are missing for this account');
    }
    if (!password_verify($idNumber, $user['id_number_hash'])) {
        fail('ID number does not match this account');
    }
    if (normalize_phone($user['phone']) !== $phone) {
        fail('Phone number does not match this account');
    }
    respond(['success' => true]);
}

function update_user_password(): void {
    require_fields(['email', 'password']);
    $email = normalize_email(input('email'));
    $password = input('password');
    if (strlen($password) < 8) {
        fail('Password must be at least 8 characters');
    }
    $hash = password_hash($password, PASSWORD_DEFAULT);
    $stmt = pdo()->prepare("UPDATE users SET password_hash=:hash WHERE LOWER(email)=:email");
    $stmt->execute([':hash' => $hash, ':email' => $email]);
    if ($stmt->rowCount() < 1) {
        fail('User not found', 404);
    }
    respond(['success' => true]);
}

function delete_account(): void {
    require_fields(['user_id']);
    $requestedUser = normalize_email(input('user_id'));
    $db = pdo();

    $lookup = $db->prepare("SELECT email, user_id FROM users WHERE LOWER(email)=:u OR LOWER(user_id)=:u LIMIT 1");
    $lookup->execute([':u' => $requestedUser]);
    $user = $lookup->fetch();
    if (!$user) {
        fail('User not found', 404);
    }

    $email = normalize_email($user['email'] ?? $requestedUser);
    $userId = normalize_email($user['user_id'] ?? $email);

    $db->beginTransaction();
    try {
        // Delete dependent rows first because this schema intentionally has no FK cascade rules.
        $deleteSaved = $db->prepare("DELETE FROM saved_items
            WHERE LOWER(user_id) IN (:saved_email, :saved_user_id)
               OR listing_id IN (
                    SELECT listing_id FROM listings
                    WHERE LOWER(seller_id) IN (:saved_seller_email, :saved_seller_user_id)
               )");
        $deleteSaved->execute([
            ':saved_email' => $email,
            ':saved_user_id' => $userId,
            ':saved_seller_email' => $email,
            ':saved_seller_user_id' => $userId,
        ]);
        $savedRows = $deleteSaved->rowCount();

        $deleteListingRatings = $db->prepare("DELETE FROM listing_ratings
            WHERE LOWER(buyer_id) IN (:lr_email, :lr_user_id)
               OR listing_id IN (
                    SELECT listing_id FROM listings
                    WHERE LOWER(seller_id) IN (:lr_seller_email, :lr_seller_user_id)
               )");
        $deleteListingRatings->execute([
            ':lr_email' => $email,
            ':lr_user_id' => $userId,
            ':lr_seller_email' => $email,
            ':lr_seller_user_id' => $userId,
        ]);
        $listingRatingRows = $deleteListingRatings->rowCount();

        $deleteMessages = $db->prepare("DELETE FROM messages
            WHERE LOWER(sender_id) IN (:msg_sender_email, :msg_sender_user_id)
               OR LOWER(receiver_id) IN (:msg_receiver_email, :msg_receiver_user_id)
               OR listing_id IN (
                    SELECT listing_id FROM listings
                    WHERE LOWER(seller_id) IN (:msg_seller_email, :msg_seller_user_id)
               )");
        $deleteMessages->execute([
            ':msg_sender_email' => $email,
            ':msg_sender_user_id' => $userId,
            ':msg_receiver_email' => $email,
            ':msg_receiver_user_id' => $userId,
            ':msg_seller_email' => $email,
            ':msg_seller_user_id' => $userId,
        ]);
        $messageRows = $deleteMessages->rowCount();

        $deleteRatings = $db->prepare("DELETE FROM ratings
            WHERE LOWER(rater_id) IN (:rating_rater_email, :rating_rater_user_id)
               OR LOWER(rated_id) IN (:rating_rated_email, :rating_rated_user_id)");
        $deleteRatings->execute([
            ':rating_rater_email' => $email,
            ':rating_rater_user_id' => $userId,
            ':rating_rated_email' => $email,
            ':rating_rated_user_id' => $userId,
        ]);
        $ratingRows = $deleteRatings->rowCount();

        $deleteListings = $db->prepare("DELETE FROM listings WHERE LOWER(seller_id) IN (:listing_email, :listing_user_id)");
        $deleteListings->execute([':listing_email' => $email, ':listing_user_id' => $userId]);
        $listingRows = $deleteListings->rowCount();

        $deleteUser = $db->prepare("DELETE FROM users WHERE LOWER(email)=:delete_email OR LOWER(user_id)=:delete_user_id");
        $deleteUser->execute([':delete_email' => $email, ':delete_user_id' => $userId]);
        $userRows = $deleteUser->rowCount();

        if ($userRows < 1) {
            $db->rollBack();
            fail('User not found', 404);
        }

        $db->commit();
        respond([
            'success' => true,
            'deleted' => [
                'users' => $userRows,
                'listings' => $listingRows,
                'messages' => $messageRows,
                'saved_items' => $savedRows,
                'ratings' => $ratingRows,
                'listing_ratings' => $listingRatingRows,
            ]
        ]);
    } catch (Throwable $e) {
        if ($db->inTransaction()) {
            $db->rollBack();
        }
        fail('Delete account failed', 500, $e->getMessage());
    }
}

function create_listing(): void {
    require_fields(['seller_id', 'title', 'category', 'price']);
    $quantity = max(0, (int)input('quantity', '0'));
    $status = $quantity > 0 ? 'active' : 'out_of_stock';
    $seller = normalize_email(input('seller_id'));

    $db = pdo();
    $stmt = $db->prepare(
        "INSERT INTO listings (seller_id, title, category, condition, description, price, image_url, quantity, status)
         VALUES (:seller_id, :title, :category, :condition, :description, :price, :image_url, :quantity, :status)
         RETURNING listing_id"
    );
    $stmt->execute([
        ':seller_id' => $seller,
        ':title' => input('title'),
        ':category' => input('category'),
        ':condition' => input('condition'),
        ':description' => input('description'),
        ':price' => input('price'),
        ':image_url' => input('image_url'),
        ':quantity' => $quantity,
        ':status' => $status,
    ]);
    $id = (int)$stmt->fetchColumn();
    respond(['success' => true, 'listing_id' => $id, 'listing' => fetch_listing($db, $id)]);
}

function get_all_listings(): void {
    // Newer items are returned first, matching the mobile market specification.
    $stmt = pdo()->query(listing_select_sql() . " ORDER BY l.created_at DESC");
    respond(['success' => true, 'listings' => $stmt->fetchAll()]);
}

function search_listings(): void {
    require_fields(['query']);
    $query = '%' . strtolower(input('query')) . '%';
    $stmt = pdo()->prepare(listing_select_sql() . "
        WHERE LOWER(l.title) LIKE :query
        ORDER BY l.created_at DESC");
    $stmt->execute([':query' => $query]);
    respond(['success' => true, 'listings' => $stmt->fetchAll()]);
}

function get_listings_by_category(): void {
    require_fields(['category']);
    $stmt = pdo()->prepare(listing_select_sql() . " WHERE LOWER(l.category) = LOWER(:category) ORDER BY l.created_at DESC");
    $stmt->execute([':category' => input('category')]);
    respond(['success' => true, 'listings' => $stmt->fetchAll()]);
}

function get_seller_listings(): void {
    require_fields(['seller_id']);
    $stmt = pdo()->prepare(listing_select_sql() . " WHERE LOWER(l.seller_id) = :seller_id ORDER BY l.created_at DESC");
    $stmt->execute([':seller_id' => normalize_email(input('seller_id'))]);
    respond(['success' => true, 'listings' => $stmt->fetchAll()]);
}

function get_listing(): void {
    require_fields(['listing_id']);
    $listing = fetch_listing(pdo(), (int)input('listing_id'));
    if (!$listing) {
        fail('Listing not found', 404);
    }
    respond(['success' => true, 'listing' => $listing]);
}

function update_listing(): void {
    require_fields(['listing_id', 'seller_id', 'title', 'category', 'price']);
    $quantity = max(0, (int)input('quantity', '1'));
    $status = $quantity > 0 ? 'active' : 'out_of_stock';
    $db = pdo();
    $stmt = $db->prepare(
        "UPDATE listings
         SET title=:title, category=:category, condition=:condition, description=:description,
             price=:price, image_url=COALESCE(NULLIF(:image_url, ''), image_url), quantity=:quantity, status=:status
         WHERE listing_id=:listing_id AND LOWER(seller_id)=:seller_id
         RETURNING listing_id"
    );
    $stmt->execute([
        ':title' => input('title'),
        ':category' => input('category'),
        ':condition' => input('condition'),
        ':description' => input('description'),
        ':price' => input('price'),
        ':image_url' => input('image_url'),
        ':quantity' => $quantity,
        ':status' => $status,
        ':listing_id' => (int)input('listing_id'),
        ':seller_id' => normalize_email(input('seller_id')),
    ]);
    $id = $stmt->fetchColumn();
    if (!$id) {
        fail('Listing not found or permission denied', 404);
    }
    respond(['success' => true, 'listing' => fetch_listing($db, (int)$id)]);
}

function delete_listing(): void {
    require_fields(['listing_id', 'seller_id']);
    $stmt = pdo()->prepare("DELETE FROM listings WHERE listing_id=:listing_id AND LOWER(seller_id)=:seller_id");
    $stmt->execute([
        ':listing_id' => (int)input('listing_id'),
        ':seller_id' => normalize_email(input('seller_id')),
    ]);
    if ($stmt->rowCount() < 1) {
        fail('Listing not found or permission denied', 404);
    }
    respond(['success' => true]);
}

function update_listing_quantity(): void {
    require_fields(['listing_id', 'quantity']);
    $quantity = max(0, (int)input('quantity'));
    $status = $quantity > 0 ? 'active' : 'out_of_stock';
    $stmt = pdo()->prepare("UPDATE listings SET quantity=:quantity, status=:status WHERE listing_id=:listing_id");
    $stmt->execute([':quantity' => $quantity, ':status' => $status, ':listing_id' => (int)input('listing_id')]);
    respond(['success' => true, 'quantity' => $quantity, 'out_of_stock' => $quantity <= 0]);
}

function purchase_listing(): void {
    require_fields(['listing_id']);
    $db = pdo();
    $listingId = (int)input('listing_id');
    $buyer = normalize_email(input('buyer_id', ''));

    $db->beginTransaction();
    try {
        $stmt = $db->prepare("SELECT listing_id, seller_id, title, quantity FROM listings WHERE listing_id=:id FOR UPDATE");
        $stmt->execute([':id' => $listingId]);
        $row = $stmt->fetch();
        if (!$row) {
            $db->rollBack();
            fail('Listing not found', 404);
        }

        $seller = normalize_email($row['seller_id'] ?? '');
        if ($buyer !== '' && $seller !== '' && $buyer === $seller) {
            $db->rollBack();
            fail('You cannot buy your own listing');
        }

        $quantity = (int)$row['quantity'];
        if ($quantity <= 0) {
            $db->rollBack();
            respond([
                'success' => true,
                'purchase_completed' => false,
                'quantity' => 0,
                'out_of_stock' => true
            ]);
        }

        $newQuantity = $quantity - 1;
        $status = $newQuantity > 0 ? 'active' : 'out_of_stock';
        $update = $db->prepare("UPDATE listings SET quantity=:q, status=:s WHERE listing_id=:id");
        $update->execute([':q' => $newQuantity, ':s' => $status, ':id' => $listingId]);

        // Create a lightweight seller notification using the existing messages table.
        // This avoids needing a separate migration and makes purchases visible in Notification Center.
        if ($buyer !== '' && $seller !== '' && $buyer !== $seller) {
            $message = 'Purchase update: your item "' . ($row['title'] ?? 'Listing') . '" has been bought.';
            $notify = $db->prepare(
                "INSERT INTO messages (sender_id, receiver_id, listing_id, message_text, is_read)
                 VALUES (:sender_id, :receiver_id, :listing_id, :message_text, false)"
            );
            $notify->execute([
                ':sender_id' => $buyer,
                ':receiver_id' => $seller,
                ':listing_id' => $listingId,
                ':message_text' => $message,
            ]);
        }

        $db->commit();
        respond([
            'success' => true,
            'purchase_completed' => true,
            'quantity' => $newQuantity,
            'out_of_stock' => $newQuantity <= 0
        ]);
    } catch (Throwable $e) {
        if ($db->inTransaction()) {
            $db->rollBack();
        }
        fail('Purchase failed', 500, $e->getMessage());
    }
}

function get_seller_listing_count(): void {
    require_fields(['seller_id']);
    $stmt = pdo()->prepare("SELECT COUNT(*) FROM listings WHERE LOWER(seller_id)=:seller_id");
    $stmt->execute([':seller_id' => normalize_email(input('seller_id'))]);
    respond(['success' => true, 'count' => (int)$stmt->fetchColumn()]);
}

function save_item(): void {
    require_fields(['user_id', 'listing_id']);
    $user = normalize_email(input('user_id'));
    $listingId = (int)input('listing_id');
    $listing = fetch_listing(pdo(), $listingId);
    if (!$listing) {
        fail('Listing not found', 404);
    }
    if (normalize_email($listing['seller_id']) === $user) {
        fail('You cannot save your own listing');
    }
    $stmt = pdo()->prepare(
        "INSERT INTO saved_items (user_id, listing_id)
         VALUES (:user_id, :listing_id)
         ON CONFLICT (user_id, listing_id) DO NOTHING"
    );
    $stmt->execute([':user_id' => $user, ':listing_id' => $listingId]);
    respond(['success' => true]);
}

function remove_saved_item(): void {
    require_fields(['user_id', 'listing_id']);
    $stmt = pdo()->prepare("DELETE FROM saved_items WHERE LOWER(user_id)=:user_id AND listing_id=:listing_id");
    $stmt->execute([':user_id' => normalize_email(input('user_id')), ':listing_id' => (int)input('listing_id')]);
    respond(['success' => true]);
}

function is_saved(): void {
    require_fields(['user_id', 'listing_id']);
    $stmt = pdo()->prepare("SELECT 1 FROM saved_items WHERE LOWER(user_id)=:user_id AND listing_id=:listing_id LIMIT 1");
    $stmt->execute([':user_id' => normalize_email(input('user_id')), ':listing_id' => (int)input('listing_id')]);
    respond(['success' => true, 'is_saved' => (bool)$stmt->fetchColumn()]);
}

function get_saved_items(): void {
    require_fields(['user_id']);
    $stmt = pdo()->prepare(
        listing_select_sql() . "
        INNER JOIN saved_items si ON si.listing_id = l.listing_id
        WHERE LOWER(si.user_id)=:user_id
        ORDER BY si.created_at DESC"
    );
    $stmt->execute([':user_id' => normalize_email(input('user_id'))]);
    respond(['success' => true, 'listings' => $stmt->fetchAll()]);
}

function get_saved_items_count(): void {
    require_fields(['user_id']);
    $stmt = pdo()->prepare("SELECT COUNT(*) FROM saved_items WHERE LOWER(user_id)=:user_id");
    $stmt->execute([':user_id' => normalize_email(input('user_id'))]);
    respond(['success' => true, 'count' => (int)$stmt->fetchColumn()]);
}

function send_message(): void {
    require_fields(['sender_id', 'listing_id', 'message_text']);
    $db = pdo();
    $listingId = (int)input('listing_id');
    $sender = normalize_email(input('sender_id'));
    $receiver = normalize_email(input('receiver_id', ''));

    if ($sender === '') {
        fail('Missing required field: sender_id');
    }

    $listing = fetch_listing($db, $listingId);
    if (!$listing) {
        fail('Listing not found for this conversation', 404);
    }

    $seller = normalize_email($listing['seller_id'] ?? '');

    // Buyer-to-seller chat: if the app did not send receiver_id, use the listing owner.
    if ($receiver === '' && $seller !== '' && $sender !== $seller) {
        $receiver = $seller;
    }

    // Buyers should always message the actual seller for the listing.
    if ($seller !== '' && $sender !== $seller && $receiver !== $seller) {
        $receiver = $seller;
    }

    if ($receiver === '') {
        fail('Receiver missing for this conversation');
    }
    if ($sender === $receiver) {
        fail('You cannot message yourself');
    }

    $stmt = $db->prepare(
        "INSERT INTO messages (sender_id, receiver_id, listing_id, message_text, is_read)
         VALUES (:sender_id, :receiver_id, :listing_id, :message_text, false)
         RETURNING message_id, sender_id, receiver_id, listing_id, message_text, is_read, created_at"
    );
    $stmt->execute([
        ':sender_id' => $sender,
        ':receiver_id' => $receiver,
        ':listing_id' => $listingId,
        ':message_text' => input('message_text'),
    ]);
    respond(['success' => true, 'message' => $stmt->fetch()]);
}

function get_conversation(): void {
    require_fields(['user_id_1', 'user_id_2']);
    $u1 = normalize_email(input('user_id_1'));
    $u2 = normalize_email(input('user_id_2'));
    $listingId = (int)input('listing_id', '0');

    // Be defensive: some older notification rows may not have a valid listing id.
    // Return an empty conversation instead of failing the Android screen.
    if ($u1 === '' || $u2 === '' || $listingId <= 0) {
        respond(['success' => true, 'messages' => []]);
    }

    $stmt = pdo()->prepare(
        "SELECT message_id, sender_id, receiver_id, listing_id, message_text, is_read, created_at
         FROM messages
         WHERE listing_id=:listing_id
           AND ((LOWER(sender_id)=:u1 AND LOWER(receiver_id)=:u2) OR (LOWER(sender_id)=:u2 AND LOWER(receiver_id)=:u1))
         ORDER BY created_at ASC, message_id ASC"
    );
    $stmt->execute([':listing_id' => $listingId, ':u1' => $u1, ':u2' => $u2]);
    respond(['success' => true, 'messages' => $stmt->fetchAll()]);
}

function get_user_conversations(): void {
    require_fields(['user_id']);
    $user = normalize_email(input('user_id'));
    $stmt = pdo()->prepare(
        "WITH cleaned AS (
            SELECT m.*,
                   CASE WHEN LOWER(m.sender_id)=:user THEN m.receiver_id ELSE m.sender_id END AS other_user_id
            FROM messages m
            WHERE (LOWER(m.sender_id)=:user OR LOWER(m.receiver_id)=:user)
              AND m.listing_id IS NOT NULL
              AND m.listing_id > 0
              AND COALESCE(m.sender_id, '') <> ''
              AND COALESCE(m.receiver_id, '') <> ''
         ), ranked AS (
            SELECT c.*,
                   ROW_NUMBER() OVER (
                       PARTITION BY LOWER(c.other_user_id), c.listing_id
                       ORDER BY c.created_at DESC, c.message_id DESC
                   ) AS rn
            FROM cleaned c
            WHERE COALESCE(c.other_user_id, '') <> ''
         )
         SELECT r.other_user_id,
                COALESCE(u.full_name, r.other_user_id) AS other_user_name,
                r.listing_id,
                COALESCE(r.message_text, '') AS last_message,
                r.created_at AS last_message_time,
                (SELECT COUNT(*) FROM messages um
                 WHERE LOWER(um.receiver_id)=:user
                   AND um.is_read=false
                   AND um.listing_id=r.listing_id
                   AND LOWER(um.sender_id)=LOWER(r.other_user_id)) AS unread_count
         FROM ranked r
         LEFT JOIN users u ON LOWER(u.email)=LOWER(r.other_user_id)
         WHERE r.rn=1
         ORDER BY r.created_at DESC, r.message_id DESC"
    );
    $stmt->execute([':user' => $user]);
    respond(['success' => true, 'conversations' => $stmt->fetchAll() ?: []]);
}

function mark_conversation_as_read(): void {
    require_fields(['user_id', 'other_user_id']);
    $listingId = (int)input('listing_id', '0');
    if ($listingId <= 0) {
        respond(['success' => true, 'rows' => 0]);
    }
    $stmt = pdo()->prepare(
        "UPDATE messages SET is_read=true
         WHERE LOWER(receiver_id)=:user_id AND LOWER(sender_id)=:other_user_id AND listing_id=:listing_id"
    );
    $stmt->execute([
        ':user_id' => normalize_email(input('user_id')),
        ':other_user_id' => normalize_email(input('other_user_id')),
        ':listing_id' => $listingId,
    ]);
    respond(['success' => true, 'rows' => $stmt->rowCount()]);
}

function get_unread_message_count(): void {
    require_fields(['user_id']);
    $stmt = pdo()->prepare("SELECT COUNT(*) FROM messages WHERE LOWER(receiver_id)=:user_id AND is_read=false");
    $stmt->execute([':user_id' => normalize_email(input('user_id'))]);
    respond(['success' => true, 'count' => (int)$stmt->fetchColumn()]);
}

function add_rating(): void {
    require_fields(['rater_id', 'rated_id', 'rating']);
    $rating = max(1, min(5, (float)input('rating')));
    $stmt = pdo()->prepare(
        "INSERT INTO ratings (rater_id, rated_id, rating, comment)
         VALUES (:rater_id, :rated_id, :rating, :comment)
         RETURNING rating_id"
    );
    $rated = normalize_email(input('rated_id'));
    $stmt->execute([
        ':rater_id' => normalize_email(input('rater_id')),
        ':rated_id' => $rated,
        ':rating' => $rating,
        ':comment' => input('comment'),
    ]);
    $ratingId = (int)$stmt->fetchColumn();
    pdo()->prepare("UPDATE users SET average_rating = ROUND(COALESCE((SELECT AVG(rating) FROM ratings WHERE LOWER(rated_id)=:rated), 0)::numeric, 1) WHERE LOWER(email)=:rated OR LOWER(user_id)=:rated")
        ->execute([':rated' => $rated]);
    respond(['success' => true, 'rating_id' => $ratingId]);
}

function get_average_rating(): void {
    require_fields(['user_id']);
    $stmt = pdo()->prepare("SELECT average_rating FROM users WHERE LOWER(email)=:user_id OR LOWER(user_id)=:user_id LIMIT 1");
    $stmt->execute([':user_id' => normalize_email(input('user_id'))]);
    respond(['success' => true, 'average_rating' => round_rating_value($stmt->fetchColumn())]);
}

function get_user_ratings(): void {
    require_fields(['user_id']);
    $stmt = pdo()->prepare("SELECT * FROM ratings WHERE LOWER(rated_id)=:user_id ORDER BY created_at DESC");
    $stmt->execute([':user_id' => normalize_email(input('user_id'))]);
    respond(['success' => true, 'ratings' => $stmt->fetchAll()]);
}

function rate_listing(): void {
    require_fields(['buyer_id', 'listing_id', 'rating']);
    $buyer = normalize_email(input('buyer_id'));
    $listingId = (int)input('listing_id');
    $rating = max(1, min(5, (float)input('rating')));
    $db = pdo();
    $listing = fetch_listing($db, $listingId);
    if (!$listing) {
        fail('Listing not found', 404);
    }
    if (normalize_email($listing['seller_id']) === $buyer) {
        fail('You cannot rate your own listing');
    }

    // Business rule: one user may rate a particular item only once.
    $check = $db->prepare("SELECT 1 FROM listing_ratings WHERE listing_id=:listing_id AND LOWER(buyer_id)=:buyer_id LIMIT 1");
    $check->execute([':listing_id' => $listingId, ':buyer_id' => $buyer]);
    if ($check->fetchColumn()) {
        fail('You have already rated this item');
    }

    $stmt = $db->prepare(
        "INSERT INTO listing_ratings (listing_id, buyer_id, rating, comment)
         VALUES (:listing_id, :buyer_id, :rating, :comment)
         RETURNING rating_id"
    );
    $stmt->execute([
        ':listing_id' => $listingId,
        ':buyer_id' => $buyer,
        ':rating' => $rating,
        ':comment' => input('comment'),
    ]);
    $ratingId = (int)$stmt->fetchColumn();
    $db->prepare("UPDATE listings SET average_rating = ROUND(COALESCE((SELECT AVG(rating) FROM listing_ratings WHERE listing_id=:listing_id), 0)::numeric, 1) WHERE listing_id=:listing_id")
        ->execute([':listing_id' => $listingId]);
    respond(['success' => true, 'rating_id' => $ratingId]);
}

$action = input('action');
if ($action === '') {
    fail('Missing action');
}

try {
    switch ($action) {
        case 'ping': ping(); break;
        case 'dbTest': db_test(); break;
        case 'registerUser': register_user(); break;
        case 'authenticateUser': authenticate_user(); break;
        case 'getUserDetails': get_user_details(); break;
        case 'updateUserProfile': update_user_profile(); break;
        case 'getUserFullName': get_user_full_name(); break;
        case 'verifyResetDetails': verify_reset_details(); break;
        case 'updateUserPassword': update_user_password(); break;
        case 'deleteUserAccount': delete_account(); break;
        case 'createListing': create_listing(); break;
        case 'getAllListings': get_all_listings(); break;
        case 'searchListings': search_listings(); break;
        case 'getListingsByCategory': get_listings_by_category(); break;
        case 'getSellerListings': get_seller_listings(); break;
        case 'getListing': get_listing(); break;
        case 'updateListing': update_listing(); break;
        case 'deleteListing': delete_listing(); break;
        case 'updateListingQuantity': update_listing_quantity(); break;
        case 'purchaseListing': purchase_listing(); break;
        case 'getSellerListingCount': get_seller_listing_count(); break;
        case 'saveItem': save_item(); break;
        case 'removeSavedItem': remove_saved_item(); break;
        case 'isSaved': is_saved(); break;
        case 'getSavedItems': get_saved_items(); break;
        case 'getSavedItemsCount': get_saved_items_count(); break;
        case 'sendMessage': send_message(); break;
        case 'getConversation': get_conversation(); break;
        case 'getUserConversations': get_user_conversations(); break;
        case 'markConversationAsRead': mark_conversation_as_read(); break;
        case 'getUnreadMessageCount': get_unread_message_count(); break;
        case 'addRating': add_rating(); break;
        case 'getAverageRating': get_average_rating(); break;
        case 'getUserRatings': get_user_ratings(); break;
        case 'rateListing': rate_listing(); break;
        default: fail('Unknown action: ' . $action, 404);
    }
} catch (PDOException $e) {
    fail('Database query failed', 500, $e->getMessage());
} catch (Throwable $e) {
    fail('Server error', 500, $e->getMessage());
}
