-- Generate test data for performance testing
-- WARNING: This will generate a lot of data. Adjust the numbers as needed.

-- Function to generate test products
-- This will create N million products for testing
CREATE OR REPLACE FUNCTION generate_products(num_records BIGINT) RETURNS VOID AS $$
DECLARE
    batch_size INT := 10000;
    num_batches INT;
    i INT;
    category_count INT;
    supplier_count INT;
BEGIN
    SELECT COUNT(*) INTO category_count FROM categories;
    SELECT COUNT(*) INTO supplier_count FROM suppliers;

    IF category_count = 0 OR supplier_count = 0 THEN
        RAISE EXCEPTION 'Please populate categories and suppliers first';
    END IF;

    num_batches := CEIL(num_records::FLOAT / batch_size);

    FOR i IN 1..num_batches LOOP
        INSERT INTO products (name, price, category_id, supplier_id, created_at)
        SELECT
            'Product ' || generate_series,
            (random() * 1000)::DECIMAL(15,2),
            (random() * (category_count - 1) + 1)::INT,
            (random() * (supplier_count - 1) + 1)::INT,
            NOW() - (random() * INTERVAL '365 days')
        FROM generate_series(
            ((i - 1) * batch_size + 1),
            LEAST(i * batch_size, num_records)
        );

        IF i % 100 = 0 THEN
            RAISE NOTICE 'Generated % batches (% records)', i, i * batch_size;
        END IF;

        -- Commit after each batch for better performance
        COMMIT;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Function to generate test orders
CREATE OR REPLACE FUNCTION generate_orders(num_records BIGINT) RETURNS VOID AS $$
DECLARE
    batch_size INT := 10000;
    num_batches INT;
    i INT;
    customer_count INT;
    status_count INT;
BEGIN
    SELECT COUNT(*) INTO customer_count FROM customers;
    SELECT COUNT(*) INTO status_count FROM order_statuses;

    IF customer_count = 0 OR status_count = 0 THEN
        RAISE EXCEPTION 'Please populate customers and order_statuses first';
    END IF;

    num_batches := CEIL(num_records::FLOAT / batch_size);

    FOR i IN 1..num_batches LOOP
        INSERT INTO orders (customer_id, status_id, total_amount, order_date)
        SELECT
            (random() * (customer_count - 1) + 1)::INT,
            (random() * (status_count - 1) + 1)::INT,
            (random() * 10000)::DECIMAL(15,2),
            NOW() - (random() * INTERVAL '730 days')
        FROM generate_series(
            ((i - 1) * batch_size + 1),
            LEAST(i * batch_size, num_records)
        );

        IF i % 100 = 0 THEN
            RAISE NOTICE 'Generated % batches (% records)', i, i * batch_size;
        END IF;

        COMMIT;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Example usage (uncomment and adjust as needed):
-- SELECT generate_products(30000000);  -- Generate 30 million products
-- SELECT generate_orders(100000000);   -- Generate 100 million orders

-- Quick test with smaller dataset:
-- SELECT generate_products(1000000);   -- 1 million products
-- SELECT generate_orders(1000000);     -- 1 million orders

-- Check table sizes:
-- SELECT
--     relname AS table_name,
--     pg_size_pretty(pg_total_relation_size(relid)) AS total_size,
--     pg_size_pretty(pg_relation_size(relid)) AS table_size,
--     pg_size_pretty(pg_indexes_size(relid)) AS index_size
-- FROM pg_stat_user_tables
-- WHERE schemaname = 'public'
-- ORDER BY pg_total_relation_size(relid) DESC;
