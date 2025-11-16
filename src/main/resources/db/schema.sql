-- Example schema for high-speed exporter demonstration
-- This creates lookup tables (small) and main tables (large)

-- =====================================================
-- LOOKUP TABLES (10-500 entries each)
-- These are cached in-memory for fast resolution
-- =====================================================

-- Categories lookup table
CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

-- Suppliers lookup table
CREATE TABLE IF NOT EXISTS suppliers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    country VARCHAR(100),
    contact_email VARCHAR(255)
);

-- Customers lookup table
CREATE TABLE IF NOT EXISTS customers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50)
);

-- Order statuses lookup table
CREATE TABLE IF NOT EXISTS order_statuses (
    id SERIAL PRIMARY KEY,
    status_name VARCHAR(50) NOT NULL,
    description TEXT
);

-- =====================================================
-- MAIN TABLES (30M - 300M records)
-- These are streamed from the database
-- =====================================================

-- Products table (can have 30M+ records)
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    price DECIMAL(15, 2) NOT NULL,
    category_id INTEGER REFERENCES categories(id),
    supplier_id INTEGER REFERENCES suppliers(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for the foreign keys
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_supplier ON products(supplier_id);

-- Orders table (can have 100M+ records)
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id INTEGER REFERENCES customers(id),
    status_id INTEGER REFERENCES order_statuses(id),
    total_amount DECIMAL(15, 2) NOT NULL,
    order_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    shipped_date TIMESTAMP WITH TIME ZONE,
    notes TEXT
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status_id);
CREATE INDEX IF NOT EXISTS idx_orders_date ON orders(order_date);

-- =====================================================
-- SAMPLE DATA FOR LOOKUP TABLES
-- =====================================================

-- Insert sample categories
INSERT INTO categories (name, description) VALUES
('Electronics', 'Electronic devices and components'),
('Clothing', 'Apparel and fashion items'),
('Books', 'Books and educational materials'),
('Home & Garden', 'Home improvement and garden supplies'),
('Sports', 'Sports equipment and accessories')
ON CONFLICT DO NOTHING;

-- Insert sample suppliers
INSERT INTO suppliers (name, country, contact_email) VALUES
('TechSupply Co.', 'USA', 'contact@techsupply.com'),
('Global Traders', 'Germany', 'info@globaltraders.de'),
('Asian Imports', 'China', 'sales@asianimports.cn'),
('Euro Distributors', 'France', 'hello@eurodist.fr')
ON CONFLICT DO NOTHING;

-- Insert sample customers
INSERT INTO customers (name, email, phone) VALUES
('John Doe', 'john.doe@email.com', '+1-555-0101'),
('Jane Smith', 'jane.smith@email.com', '+1-555-0102'),
('Bob Johnson', 'bob.johnson@email.com', '+1-555-0103'),
('Alice Williams', 'alice.williams@email.com', '+1-555-0104')
ON CONFLICT DO NOTHING;

-- Insert order statuses
INSERT INTO order_statuses (status_name, description) VALUES
('Pending', 'Order received, awaiting processing'),
('Processing', 'Order is being prepared'),
('Shipped', 'Order has been shipped'),
('Delivered', 'Order has been delivered'),
('Cancelled', 'Order has been cancelled')
ON CONFLICT DO NOTHING;
