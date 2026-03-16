-- =============================================================
-- Point of Sale (POS) Database Schema
-- PostgreSQL
-- =============================================================

BEGIN;

-- ============================================================
-- TABLE: users
-- Stores admin user accounts and credentials.
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,          -- Login username
    password    VARCHAR(255) NOT NULL,                  -- BCrypt hashed password
    full_name   VARCHAR(200),
    email       VARCHAR(200),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
COMMENT ON TABLE users IS 'Admin user accounts for POS access';

-- ============================================================
-- TABLE: store_settings
-- Single-row table for global store configuration.
-- ============================================================
CREATE TABLE IF NOT EXISTS store_settings (
    id                  BIGSERIAL PRIMARY KEY,
    store_name          VARCHAR(200) NOT NULL DEFAULT 'Culinary Lab POS',
    address             TEXT,
    phone               VARCHAR(50),
    logo_path           VARCHAR(500),                   -- File path or URL of store logo
    tax_rate            NUMERIC(5,2) NOT NULL DEFAULT 11.00,  -- PPN percentage (default 11%)
    low_stock_threshold INTEGER NOT NULL DEFAULT 10,           -- Global low stock warning level
    receipt_footer      VARCHAR(500) DEFAULT 'Terima kasih telah berbelanja!',
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE store_settings IS 'Single-row global POS store configuration';

-- ============================================================
-- TABLE: categories
-- Product categories for grouping and filtering.
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_categories_name ON categories (name);
CREATE INDEX IF NOT EXISTS idx_categories_active ON categories (is_active);
COMMENT ON TABLE categories IS 'Product categories for grouping and filtering';

-- ============================================================
-- TABLE: products
-- Product catalog with pricing, stock, and metadata.
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    id                  BIGSERIAL PRIMARY KEY,
    category_id         BIGINT REFERENCES categories (id) ON DELETE SET NULL,
    name                VARCHAR(255) NOT NULL,
    sku                 VARCHAR(100) UNIQUE,                    -- Barcode or SKU code
    description         TEXT,
    unit                VARCHAR(50) DEFAULT 'pcs',             -- Unit: pcs, kg, box, etc.
    purchase_price      NUMERIC(15,2) NOT NULL DEFAULT 0,      -- Cost/purchase price
    selling_price       NUMERIC(15,2) NOT NULL,                -- Customer selling price
    stock               INTEGER NOT NULL DEFAULT 0,
    low_stock_threshold INTEGER,                               -- Override global threshold (nullable)
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_products_name ON products (name);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products (sku);
CREATE INDEX IF NOT EXISTS idx_products_category ON products (category_id);
CREATE INDEX IF NOT EXISTS idx_products_active ON products (is_active);
COMMENT ON TABLE products IS 'Product catalog with pricing and stock information';
COMMENT ON COLUMN products.sku IS 'SKU or barcode identifier, must be unique';
COMMENT ON COLUMN products.low_stock_threshold IS 'Overrides store_settings.low_stock_threshold when set';

-- ============================================================
-- TABLE: discounts
-- Reusable discount presets selectable during sale transactions.
-- ============================================================
CREATE TABLE IF NOT EXISTS discounts (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    type        VARCHAR(20) NOT NULL CHECK (type IN ('PERSENTASE', 'NOMINAL')),  -- discount type enum
    value       NUMERIC(15,2) NOT NULL,                       -- Percent value or fixed amount
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_discounts_active ON discounts (is_active);
COMMENT ON TABLE discounts IS 'Reusable discount presets (percentage or fixed nominal)';
COMMENT ON COLUMN discounts.type IS 'PERSENTASE = percentage discount, NOMINAL = fixed Rupiah amount';

-- ============================================================
-- TABLE: transactions
-- Master record for each sale transaction.
-- ============================================================
CREATE TABLE IF NOT EXISTS transactions (
    id                  BIGSERIAL PRIMARY KEY,
    transaction_number  VARCHAR(30) NOT NULL UNIQUE,           -- Format: TRX-YYYYMMDD-XXXX
    user_id             BIGINT REFERENCES users (id),          -- Cashier who processed the sale
    discount_id         BIGINT REFERENCES discounts (id) ON DELETE SET NULL, -- Preset discount used
    customer_name       VARCHAR(200),                          -- Optional customer name
    payment_method      VARCHAR(20) NOT NULL CHECK (payment_method IN ('TUNAI', 'QRIS', 'DEBIT', 'KARTU_KREDIT')),
    status              VARCHAR(20) NOT NULL DEFAULT 'SELESAI' CHECK (status IN ('SELESAI', 'VOID')),
    subtotal            NUMERIC(15,2) NOT NULL DEFAULT 0,      -- Before discount and tax
    discount_type       VARCHAR(20) CHECK (discount_type IN ('PERSENTASE', 'NOMINAL')),  -- Actual applied discount type
    discount_value      NUMERIC(15,2) DEFAULT 0,               -- Actual applied discount value
    discount_amount     NUMERIC(15,2) DEFAULT 0,               -- Calculated discount in Rupiah
    tax_rate            NUMERIC(5,2) NOT NULL DEFAULT 11.00,  -- Tax rate at time of sale
    tax_amount          NUMERIC(15,2) DEFAULT 0,               -- Calculated tax in Rupiah
    grand_total         NUMERIC(15,2) NOT NULL DEFAULT 0,
    amount_paid         NUMERIC(15,2) DEFAULT 0,               -- Cash received (for TUNAI)
    change_amount       NUMERIC(15,2) DEFAULT 0,               -- Change given back (kembalian)
    void_reason         TEXT,                                  -- Reason for voiding (if status = VOID)
    voided_at           TIMESTAMP,
    transaction_date    TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_transactions_number ON transactions (transaction_number);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions (transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_transactions_payment ON transactions (payment_method);
CREATE INDEX IF NOT EXISTS idx_transactions_user ON transactions (user_id);
COMMENT ON TABLE transactions IS 'Master record for each sale transaction';
COMMENT ON COLUMN transactions.transaction_number IS 'Unique transaction number in format TRX-YYYYMMDD-XXXX';
COMMENT ON COLUMN transactions.discount_type IS 'Actual discount type applied (may differ from preset if manually entered)';

-- ============================================================
-- TABLE: transaction_items
-- Line items for each transaction with snapshotted product data.
-- ============================================================
CREATE TABLE IF NOT EXISTS transaction_items (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT NOT NULL REFERENCES transactions (id) ON DELETE CASCADE,
    product_id      BIGINT REFERENCES products (id) ON DELETE SET NULL,
    product_name    VARCHAR(255) NOT NULL,           -- Snapshot: product name at time of sale
    product_sku     VARCHAR(100),                    -- Snapshot: SKU at time of sale
    unit            VARCHAR(50),                     -- Snapshot: unit at time of sale
    quantity        INTEGER NOT NULL,
    unit_price      NUMERIC(15,2) NOT NULL,          -- Snapshot: selling price at time of sale
    subtotal        NUMERIC(15,2) NOT NULL,          -- quantity * unit_price
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_transaction_items_transaction ON transaction_items (transaction_id);
CREATE INDEX IF NOT EXISTS idx_transaction_items_product ON transaction_items (product_id);
COMMENT ON TABLE transaction_items IS 'Line items for each transaction (product data snapshotted at sale time)';

-- ============================================================
-- DEFAULT DATA
-- ============================================================

-- Insert default store settings row (single-row config table)
INSERT INTO store_settings (store_name, address, phone, tax_rate, low_stock_threshold, receipt_footer)
SELECT 'SMK Tunas Pembangunan', 'Jl. KH. Moh. Naim I No.68 5, RT.5/RW.11, Cipete Utara, Kec. Kby. Baru, Kota Jakarta Selatan', '021-7261850', 11.00, 10, 'Terima kasih telah berbelanja!'
WHERE NOT EXISTS (SELECT 1 FROM store_settings);

COMMIT;

INSERT INTO users (created_at, updated_at, email, username, password, full_name, is_active)
VALUES (NOW(), NOW(), 'fatimaariza@toepan.com', 'fatimaariza', '$2a$10$7pkCocrHw3iDfDjRRe11eeS8XOEKzC3nId5QXXE.zl4.OC87H4Evm', 'Fatima Tertia Ariza', TRUE);