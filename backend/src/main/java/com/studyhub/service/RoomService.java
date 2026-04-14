package com.studyhub.service;

import com.studyhub.exception.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoomService {

    private final JdbcTemplate jdbcTemplate;

    public RoomService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listRooms() {
        return jdbcTemplate.queryForList(
            "SELECT r.id, r.name, r.location_desc, r.facilities, r.seat_total, r.status, " +
                "COALESCE(ROUND(AVG(rv.score),1), 0) AS avg_score " +
                "FROM study_room r LEFT JOIN review rv ON r.id = rv.room_id GROUP BY r.id ORDER BY r.id"
        );
    }

    public Map<String, Object> roomDetail(Long roomId) {
        Map<String, Object> room = jdbcTemplate.queryForMap("SELECT * FROM study_room WHERE id = ?", roomId);
        List<Map<String, Object>> openRules = jdbcTemplate.queryForList(
            "SELECT day_type, open_time, close_time FROM room_open_rule WHERE room_id = ? ORDER BY day_type", roomId
        );
        List<Map<String, Object>> seats = jdbcTemplate.queryForList(
            "SELECT id, seat_no, seat_type, x_pos, y_pos, status FROM seat WHERE room_id = ? ORDER BY id", roomId
        );
        List<Map<String, Object>> reviews = jdbcTemplate.queryForList(
            "SELECT rv.id, rv.score, rv.content, rv.created_at, up.nickname " +
                "FROM review rv LEFT JOIN user_profile up ON rv.user_id = up.user_id " +
                "WHERE rv.room_id = ? ORDER BY rv.id DESC LIMIT 20",
            roomId
        );
        Map<String, Object> result = new HashMap<>();
        result.put("room", room);
        result.put("openRules", openRules);
        result.put("seats", seats);
        result.put("reviews", reviews);
        return result;
    }

    public List<Map<String, Object>> seatSlots(Long roomId, LocalDate date, Integer slotCode) {
        return jdbcTemplate.queryForList(
            "SELECT s.id seat_id, s.seat_no, s.seat_type, s.status seat_base_status, " +
                "ss.current_order_id, COALESCE(ss.status, IF(s.status = 2, 3, 0)) AS slot_status " +
                "FROM seat s LEFT JOIN seat_slot ss ON s.id = ss.seat_id AND ss.booking_date = ? AND ss.slot_code = ? " +
                "WHERE s.room_id = ? ORDER BY s.id",
            date, slotCode, roomId
        );
    }

    public void addReview(Long userId, Long roomId, Integer score, String content, Long orderId) {
        if (score < 1 || score > 5) {
            throw new BizException("评分范围 1-5");
        }
        jdbcTemplate.update("INSERT INTO review(user_id, room_id, order_id, score, content) VALUES (?, ?, ?, ?, ?)",
            userId, roomId, orderId, score, content);
    }
}
