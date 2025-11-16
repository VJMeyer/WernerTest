package com.example.app.repository;

import com.example.app.model.Product;
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

@Repository
public class ProductRepository {

    private static final Logger logger = LoggerFactory.getLogger(ProductRepository.class);

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Product> rowMapper = (rs, rowNum) -> {
        Product p = new Product();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setQuantity(rs.getInt("quantity"));
        return p;
    };

    public ProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initializeSchema() {
        logger.info("Initializing database schema...");

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS products (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description VARCHAR(1000),
                price DECIMAL(10,2) NOT NULL,
                quantity INT DEFAULT 0
            )
            """);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        if (count != null && count == 0) {
            logger.info("Inserting sample data...");
            jdbcTemplate.update("INSERT INTO products (name, description, price, quantity) VALUES (?, ?, ?, ?)",
                    "Laptop", "High-performance laptop", 999.99, 10);
            jdbcTemplate.update("INSERT INTO products (name, description, price, quantity) VALUES (?, ?, ?, ?)",
                    "Mouse", "Wireless mouse", 29.99, 50);
            jdbcTemplate.update("INSERT INTO products (name, description, price, quantity) VALUES (?, ?, ?, ?)",
                    "Keyboard", "Mechanical keyboard", 79.99, 25);
        }

        logger.info("Database schema initialized");
    }

    public List<Product> findAll() {
        return jdbcTemplate.query("SELECT * FROM products", rowMapper);
    }

    public Optional<Product> findById(Long id) {
        List<Product> results = jdbcTemplate.query("SELECT * FROM products WHERE id = ?", rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Product save(Product product) {
        if (product.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO products (name, description, price, quantity) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, product.getName());
                ps.setString(2, product.getDescription());
                ps.setBigDecimal(3, product.getPrice());
                ps.setInt(4, product.getQuantity());
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key != null) product.setId(key.longValue());
        } else {
            jdbcTemplate.update("UPDATE products SET name=?, description=?, price=?, quantity=? WHERE id=?",
                    product.getName(), product.getDescription(), product.getPrice(), product.getQuantity(), product.getId());
        }
        return product;
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM products WHERE id = ?", id);
    }

    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }
}
