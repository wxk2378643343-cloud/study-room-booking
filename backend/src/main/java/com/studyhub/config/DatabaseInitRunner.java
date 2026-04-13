package com.studyhub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DatabaseInitRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Value("${app.db.auto-init:true}")
    private boolean autoInit;

    public DatabaseInitRunner(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!autoInit) {
            log.info("Database auto init disabled.");
            return;
        }
        Integer exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_user'",
            Integer.class
        );
        if (exists != null && exists > 0) {
            log.info("Database already initialized, skip schema/seed.");
            ensureDemoUsers();
            return;
        }

        log.info("Database not initialized, running schema and seed scripts...");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setContinueOnError(false);
        populator.addScript(new ClassPathResource("db/schema.sql"));
        populator.addScript(new ClassPathResource("db/seed.sql"));
        populator.execute(dataSource);
        ensureDemoUsers();
        log.info("Database initialization completed.");
    }

    private void ensureDemoUsers() {
        // Ensure demo users exist and use password 123456 (sha256)
        final String pwd = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";
        jdbcTemplate.update(
            "INSERT INTO sys_user(id, phone, password_hash, role, status, violation_count) VALUES (1, '13800000001', ?, 9, 1, 0) " +
                "ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), role = VALUES(role), status = 1",
            pwd
        );
        jdbcTemplate.update(
            "INSERT INTO sys_user(id, phone, password_hash, role, status, violation_count) VALUES (2, '13800000002', ?, 1, 1, 0) " +
                "ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), role = VALUES(role), status = 1",
            pwd
        );
        jdbcTemplate.update(
            "INSERT INTO user_profile(user_id, nickname) VALUES (1, '管理员') ON DUPLICATE KEY UPDATE nickname = VALUES(nickname)"
        );
        jdbcTemplate.update(
            "INSERT INTO user_profile(user_id, nickname) VALUES (2, '测试用户') ON DUPLICATE KEY UPDATE nickname = VALUES(nickname)"
        );
        jdbcTemplate.update(
            "INSERT INTO points_account(user_id, points_balance) VALUES (1, 0) ON DUPLICATE KEY UPDATE user_id = user_id"
        );
        jdbcTemplate.update(
            "INSERT INTO points_account(user_id, points_balance) VALUES (2, 100) ON DUPLICATE KEY UPDATE user_id = user_id"
        );
        jdbcTemplate.update(
            "INSERT INTO points_txn(user_id, change_amount, biz_type, biz_ref_id) " +
                "SELECT 2, 100, 3, NULL FROM dual WHERE NOT EXISTS (SELECT 1 FROM points_txn WHERE user_id = 2)"
        );
        jdbcTemplate.update(
            "UPDATE booking_rule SET checkin_grace_minutes = 120 WHERE checkin_grace_minutes < 120"
        );
    }
}
