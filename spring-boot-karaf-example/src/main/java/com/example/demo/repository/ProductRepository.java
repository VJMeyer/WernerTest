package com.example.demo.repository;

import com.example.demo.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-based repository for Product entity.
 */
@Repository
public class ProductRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProductRepository.class);

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Product> productRowMapper = (rs, rowNum) -> {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setName(rs.getString("name"));
        product.setDescription(rs.getString("description"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setQuantity(rs.getInt("quantity"));
        return product;
    };

    public ProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Initialize database schema on startup.
     */
    @PostConstruct
    public void initializeSchema() {
        logger.info("Initializing database schema...");

        String createTableSql = """
            CREATE TABLE IF NOT EXISTS products (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description VARCHAR(1000),
                price DECIMAL(10,2) NOT NULL,
                quantity INT DEFAULT 0
            )
            """;

        jdbcTemplate.execute(createTableSql);
        logger.info("Database schema initialized successfully");

        // Insert sample data if table is empty
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        if (count != null && count == 0) {
            insertSampleData();
        }
    }

    private void insertSampleData() {
        logger.info("Inserting sample data...");

        String insertSql = "INSERT INTO products (name, description, price, quantity) VALUES (?, ?, ?, ?)";

        jdbcTemplate.update(insertSql, "Laptop", "High-performance laptop", 999.99, 10);
        jdbcTemplate.update(insertSql, "Mouse", "Wireless mouse", 29.99, 50);
        jdbcTemplate.update(insertSql, "Keyboard", "Mechanical keyboard", 79.99, 25);
        jdbcTemplate.update(insertSql, "Monitor", "27-inch 4K monitor", 399.99, 15);
        jdbcTemplate.update(insertSql, "Headphones", "Noise-canceling headphones", 199.99, 30);

        logger.info("Sample data inserted successfully");
    }

    public List<Product> findAll() {
        String sql = "SELECT id, name, description, price, quantity FROM products";
        return jdbcTemplate.query(sql, productRowMapper);
    }

    public Optional<Product> findById(Long id) {
        String sql = "SELECT id, name, description, price, quantity FROM products WHERE id = ?";
        List<Product> results = jdbcTemplate.query(sql, productRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Product save(Product product) {
        if (product.getId() == null) {
            return insert(product);
        } else {
            return update(product);
        }
    }

    private Product insert(Product product) {
        String sql = "INSERT INTO products (name, description, price, quantity) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, product.getName());
            ps.setString(2, product.getDescription());
            ps.setBigDecimal(3, product.getPrice());
            ps.setInt(4, product.getQuantity());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            product.setId(key.longValue());
        }

        return product;
    }

    private Product update(Product product) {
        String sql = "UPDATE products SET name = ?, description = ?, price = ?, quantity = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getId());
        return product;
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM products WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM products WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}
