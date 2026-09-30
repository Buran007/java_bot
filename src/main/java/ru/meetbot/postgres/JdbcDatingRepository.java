package ru.meetbot.postgres;

import ru.meetbot.core.DatingRepository;
import ru.meetbot.core.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class JdbcDatingRepository implements DatingRepository {
    private final DataSource dataSource;

    public JdbcDatingRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Profile> findProfile(long telegramId) {
        String sql = "SELECT telegram_user_id, username, display_name, age, city, bio FROM profiles WHERE telegram_user_id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, telegramId);
            try (ResultSet rows = q.executeQuery()) {
                return rows.next() ? Optional.of(profile(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw failure("load profile", e);
        }
    }

    @Override
    public void saveProfile(Profile p) {
        String sql = "INSERT INTO profiles(telegram_user_id, username, display_name, age, city, bio) VALUES (?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (telegram_user_id) DO UPDATE SET username=EXCLUDED.username, display_name=EXCLUDED.display_name, age=EXCLUDED.age, "
                + "city=EXCLUDED.city, bio=EXCLUDED.bio, updated_at=now()";
        try (Connection c = dataSource.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, p.telegramId());
            q.setString(2, p.username());
            q.setString(3, p.name());
            q.setInt(4, p.age());
            q.setString(5, p.city());
            q.setString(6, p.about());
            q.executeUpdate();
        } catch (SQLException e) {
            throw failure("save profile", e);
        }
    }

    @Override
    public void deleteProfile(long telegramId) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement q = c.prepareStatement("DELETE FROM profiles WHERE telegram_user_id = ?")) {
            q.setLong(1, telegramId);
            q.executeUpdate();
        } catch (SQLException e) {
            throw failure("delete profile", e);
        }
    }

    @Override
    public boolean isHealthy() {
        try (Connection c = dataSource.getConnection(); PreparedStatement q = c.prepareStatement("SELECT 1")) {
            return q.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public Optional<Profile> nextCandidate(long telegramId, Set<Long> seenIds) {
        String sql = "SELECT p.telegram_user_id, p.username, p.display_name, p.age, p.city, p.bio FROM profiles p "
                + "WHERE p.telegram_user_id <> ? AND NOT EXISTS (SELECT 1 FROM likes l "
                + "WHERE l.from_user_id = ? AND l.to_user_id = p.telegram_user_id) ORDER BY p.created_at, p.telegram_user_id";
        try (Connection c = dataSource.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, telegramId);
            q.setLong(2, telegramId);
            try (ResultSet rows = q.executeQuery()) {
                while (rows.next()) {
                    Profile candidate = profile(rows);
                    if (!seenIds.contains(candidate.telegramId())) return Optional.of(candidate);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw failure("find next profile", e);
        }
    }

    @Override
    public boolean addLike(long fromTelegramId, long toTelegramId) {
        String insertLike = "INSERT INTO likes(from_user_id, to_user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        String insertMatch = "INSERT INTO matches(user_low_id, user_high_id) "
                + "SELECT LEAST(?, ?), GREATEST(?, ?) WHERE EXISTS (SELECT 1 FROM likes "
                + "WHERE from_user_id = ? AND to_user_id = ?) AND EXISTS (SELECT 1 FROM likes "
                + "WHERE from_user_id = ? AND to_user_id = ?) ON CONFLICT DO NOTHING";
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement like = c.prepareStatement(insertLike)) {
                like.setLong(1, fromTelegramId);
                like.setLong(2, toTelegramId);
                if (like.executeUpdate() == 0) {
                    c.commit();
                    return false;
                }
            }
            try (PreparedStatement match = c.prepareStatement(insertMatch)) {
                match.setLong(1, fromTelegramId);
                match.setLong(2, toTelegramId);
                match.setLong(3, fromTelegramId);
                match.setLong(4, toTelegramId);
                match.setLong(5, fromTelegramId);
                match.setLong(6, toTelegramId);
                match.setLong(7, toTelegramId);
                match.setLong(8, fromTelegramId);
                match.executeUpdate();
            }
            c.commit();
            return true;
        } catch (SQLException e) {
            throw failure("save like", e);
        }
    }

    @Override
    public List<Profile> findMatches(long telegramId, int limit) {
        String sql = "SELECT p.telegram_user_id, p.username, p.display_name, p.age, p.city, p.bio FROM matches m "
                + "JOIN profiles p ON p.telegram_user_id = CASE WHEN m.user_low_id = ? THEN m.user_high_id ELSE m.user_low_id END "
                + "WHERE m.user_low_id = ? OR m.user_high_id = ? ORDER BY m.created_at DESC LIMIT ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, telegramId);
            q.setLong(2, telegramId);
            q.setLong(3, telegramId);
            q.setInt(4, Math.max(0, limit));
            try (ResultSet rows = q.executeQuery()) {
                List<Profile> result = new ArrayList<>();
                while (rows.next()) result.add(profile(rows));
                return List.copyOf(result);
            }
        } catch (SQLException e) {
            throw failure("load matches", e);
        }
    }

    private static Profile profile(ResultSet row) throws SQLException {
        return new Profile(row.getLong("telegram_user_id"), row.getString("username"), row.getString("display_name"),
                row.getInt("age"), row.getString("city"), row.getString("bio"));
    }

    private static IllegalStateException failure(String action, SQLException cause) {
        return new IllegalStateException("Could not " + action + " in PostgreSQL", cause);
    }
}
