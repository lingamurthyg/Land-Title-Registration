package com.trianz.ltr.dao;

import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.model.LandTitle.TitleStatus;
import com.trianz.ltr.model.LandTitle.LandUseType;
import com.trianz.ltr.util.CloudDataSourceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * LandTitleDAO - JDBC Data Access Object for LAND_TITLE table.
 * 
 * Cloud-native improvements:
 * - Uses CloudDataSourceUtil with HikariCP connection pooling
 * - Uses java.time.Instant for all timestamps (UTC)
 * - Structured logging with SLF4J
 * - Proper exception handling for cloud environments
 * - Connection timeout handling
 */
public class LandTitleDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(LandTitleDAO.class);

    // ── SQL Statements ──────────────────────────────────────────────────────────

    private static final String SQL_INSERT =
        "INSERT INTO LAND_TITLE (TITLE_NUMBER, PARCEL_ID, LEGAL_DESCRIPTION, AREA_SQM, " +
        "STATUS, LAND_USE_TYPE, STREET_ADDRESS, SUBURB, CITY, STATE_PROVINCE, COUNTRY, " +
        "POSTAL_CODE, LATITUDE, LONGITUDE, OWNER_NATIONAL_ID, OWNER_FULL_NAME, " +
        "OWNER_EMAIL, OWNER_PHONE, ASSESSED_VALUE, MARKET_VALUE, CURRENCY_CODE, " +
        "REGISTRATION_DATE, REGISTERED_BY, HAS_LIEN, HAS_MORTGAGE, ENCUMBRANCE_DETAIL, REMARKS) " +
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String SQL_SELECT_BY_TITLE =
        "SELECT * FROM LAND_TITLE WHERE TITLE_NUMBER = ?";

    private static final String SQL_SELECT_BY_PARCEL =
        "SELECT * FROM LAND_TITLE WHERE PARCEL_ID = ?";

    private static final String SQL_SELECT_BY_OWNER =
        "SELECT * FROM LAND_TITLE WHERE OWNER_NATIONAL_ID = ? ORDER BY REGISTRATION_DATE DESC";

    private static final String SQL_SELECT_BY_STATUS =
        "SELECT * FROM LAND_TITLE WHERE STATUS = ? ORDER BY REGISTRATION_DATE DESC";

    private static final String SQL_UPDATE =
        "UPDATE LAND_TITLE SET STATUS=?, OWNER_NATIONAL_ID=?, OWNER_FULL_NAME=?, " +
        "OWNER_EMAIL=?, OWNER_PHONE=?, ASSESSED_VALUE=?, MARKET_VALUE=?, " +
        "LAST_MODIFIED_DATE=?, LAST_MODIFIED_BY=?, HAS_LIEN=?, HAS_MORTGAGE=?, " +
        "ENCUMBRANCE_DETAIL=?, REMARKS=? WHERE TITLE_NUMBER=?";

    private static final String SQL_UPDATE_STATUS =
        "UPDATE LAND_TITLE SET STATUS=?, LAST_MODIFIED_DATE=?, LAST_MODIFIED_BY=? " +
        "WHERE TITLE_NUMBER=?";

    private static final String SQL_DELETE =
        "DELETE FROM LAND_TITLE WHERE TITLE_NUMBER = ?";

    private static final String SQL_SEARCH =
        "SELECT * FROM LAND_TITLE WHERE " +
        "(UPPER(OWNER_FULL_NAME) LIKE UPPER(?) OR UPPER(CITY) LIKE UPPER(?) " +
        "OR UPPER(PARCEL_ID) LIKE UPPER(?)) ORDER BY REGISTRATION_DATE DESC";

    // ── Insert ─────────────────────────────────────────────────────────────────

    public void insert(LandTitle t) throws SQLException {
        Connection conn = null;
        try {
            conn = CloudDataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
                ps.setString(1,  t.getTitleNumber());
                ps.setString(2,  t.getParcelId());
                ps.setString(3,  t.getLegalDescription());
                setBigDecimal(ps, 4, t.getAreaSquareMetres());
                ps.setString(5,  t.getStatus() != null ? t.getStatus().name() : TitleStatus.PENDING.name());
                ps.setString(6,  t.getLandUseType() != null ? t.getLandUseType().name() : null);
                ps.setString(7,  t.getStreetAddress());
                ps.setString(8,  t.getSuburb());
                ps.setString(9,  t.getCity());
                ps.setString(10, t.getStateProvince());
                ps.setString(11, t.getCountry());
                ps.setString(12, t.getPostalCode());
                setBigDecimal(ps, 13, t.getLatitude());
                setBigDecimal(ps, 14, t.getLongitude());
                ps.setString(15, t.getOwnerNationalId());
                ps.setString(16, t.getOwnerFullName());
                ps.setString(17, t.getOwnerContactEmail());
                ps.setString(18, t.getOwnerContactPhone());
                setBigDecimal(ps, 19, t.getAssessedValue());
                setBigDecimal(ps, 20, t.getMarketValue());
                ps.setString(21, t.getCurrencyCode());
                ps.setTimestamp(22, t.getRegistrationDate() != null
                        ? Timestamp.from(t.getRegistrationDate()) : Timestamp.from(Instant.now()));
                ps.setString(23, t.getRegisteredBy());
                ps.setInt(24, t.isHasLien() ? 1 : 0);
                ps.setInt(25, t.isHasMortgage() ? 1 : 0);
                ps.setString(26, t.getEncumbranceDetails());
                ps.setString(27, t.getRemarks());
                ps.executeUpdate();
                LOGGER.info("Inserted LandTitle: {}", t.getTitleNumber());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to insert LandTitle: {}", t.getTitleNumber(), e);
            throw new SQLException("Insert failed: " + e.getMessage(), e);
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }
    }

    // ── Find by Title Number ───────────────────────────────────────────────────

    public LandTitle findByTitleNumber(String titleNumber) throws SQLException {
        Connection conn = null;
        try {
            conn = CloudDataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_TITLE)) {
                ps.setString(1, titleNumber);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapRow(rs) : null;
                }
            }
        } catch (Exception e) {
            LOGGER.error("findByTitleNumber failed for: {}", titleNumber, e);
            throw new SQLException("findByTitleNumber failed: " + e.getMessage(), e);
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }
    }

    // ── Find by Parcel ID ──────────────────────────────────────────────────────

    public LandTitle findByParcelId(String parcelId) throws SQLException {
        Connection conn = null;
        try {
            conn = CloudDataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_PARCEL)) {
                ps.setString(1, parcelId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapRow(rs) : null;
                }
            }
        } catch (Exception e) {
            LOGGER.error("findByParcelId failed for: {}", parcelId, e);
            throw new SQLException("findByParcelId failed: " + e.getMessage(), e);
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }
    }

    // ── Find by Owner ──────────────────────────────────────────────────────────

    public List<LandTitle> findByOwner(String ownerNationalId) throws SQLException {
        Connection conn = null;
        try {
            conn = CloudDataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_OWNER)) {
                ps.setString(1, ownerNationalId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<LandTitle> list = new ArrayList<>();
                    while (rs.next()) list.add(mapRow(rs));
                    LOGGER.debug("Found {} titles for owner: {}", list.size(), ownerNationalId);
                    return list;
                }
            }
        } catch (Exception e) {
            LOGGER.error("findByOwner failed for: {}", ownerNationalId, e);
            throw new SQLException("findByOwner failed: " + e.getMessage(), e);
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }
    }

    // ── Find by Status ─────────────────────────────────────────────────────────

    public List<LandTitle> findByStatus(TitleStatus status) throws SQLException {
        Connection conn = null;
        try {
            conn = CloudDataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_STATUS)) {
                ps.setString(1, status.name());
                try (ResultSet rs = ps.executeQuery()) {
                    List<LandTitle> list = new ArrayList<>();
                    while (rs.next()) list.add(mapRow(rs));
                    LOGGER.debug("Found {} titles with status: {}", list.size(), status);
                    return list;
                }
            }
        } catch (Exception e) {
            LOGGER.error("findByStatus failed for: {}", status, e);
            throw new SQLException("findByStatus failed: " + e.getMessage(), e);
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }
    }

    // ── Search ─────────────────────────────────────────────────────────────────

    public List<LandTitle> search(String keyword) throws SQLException {
        Connection conn = null;
        String pattern = "%" + keyword + "%";
        try {
            conn = CloudDataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_SEARCH)) {
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
                try (ResultSet rs = ps.executeQuery()) {
                    List<LandTitle> list = new ArrayList<>();
                    while (rs.next()) list.add(mapRow(rs));
                    LOGGER.debug("Search for '{}' returned {} results", keyword, list.size());
                    return list;
                }
            }
        } catch (Exception e) {
            LOGGER.error("search failed for keyword: {}", keyword, e);
            throw new SQLException("search failed: " + e.getMessage(), e);
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }
    }

    // ── Update ─────────────────────────────────────────────────────────────────

    public int update(LandTitle t) throws SQLException {
        Connection conn = null;
        try {
            conn = CloudDataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
                ps.setString(1,  t.getStatus().name());
                ps.setString(2,  t.getOwnerNationalId());
                ps.setString(3,  t.getOwnerFullName());
                ps.setString(4,  t.getOwnerContactEmail());
                ps.setString(5,  t.getOwnerContactPhone());
                setBigDecimal(ps, 6, t.getAssessedValue());
                setBigDecimal(ps, 7, t.getMarketValue());
                ps.setTimestamp(8, t.getLastModifiedDate() != null
                        ? Timestamp.from(t.getLastModifiedDate()) : Timestamp.from(Instant.now()));
                ps.setString(9,  t.getLastModifiedBy());
                ps.setInt(10, t.isHasLien() ? 1 : 0);
                ps.setInt(11, t.isHasMortgage() ? 1 : 0);
                ps.setString(12, t.getEncumbranceDetails());
                ps.setString(13, t.getRemarks());
                ps.setString(14, t.getTitleNumber());
                int rows = ps.executeUpdate();
                LOGGER.info("Updated LandTitle: {}, rows affected: {}", t.getTitleNumber(), rows);
                return rows;
            }
        } catch (Exception e) {
            LOGGER.error("update failed for: {}", t.getTitleNumber(), e);
            throw new SQLException("update failed: " + e.getMessage(), e);
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }
    }

    // ── Update Status Only ─────────────────────────────────────────────────────

    public int updateStatus(String titleNumber, TitleStatus status, String modifiedBy) throws SQLException {
        Connection conn = null;
        try {
            conn = CloudDataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {
                ps.setString(1, status.name());
                ps.setTimestamp(2, Timestamp.from(Instant.now()));
                ps.setString(3, modifiedBy);
                ps.setString(4, titleNumber);
                int rows = ps.executeUpdate();
                LOGGER.info("Updated status for {}: {} -> {}, rows: {}", titleNumber, status, modifiedBy, rows);
                return rows;
            }
        } catch (Exception e) {
            LOGGER.error("updateStatus failed for: {}", titleNumber, e);
            throw new SQLException("updateStatus failed: " + e.getMessage(), e);
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    public int delete(String titleNumber) throws SQLException {
        Connection conn = null;
        try {
            conn = CloudDataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
                ps.setString(1, titleNumber);
                int rows = ps.executeUpdate();
                LOGGER.info("Deleted LandTitle: {}, rows: {}", titleNumber, rows);
                return rows;
            }
        } catch (Exception e) {
            LOGGER.error("delete failed for: {}", titleNumber, e);
            throw new SQLException("delete failed: " + e.getMessage(), e);
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }
    }

    // ── Row Mapper ─────────────────────────────────────────────────────────────

    private LandTitle mapRow(ResultSet rs) throws SQLException {
        LandTitle t = new LandTitle();
        t.setTitleNumber(rs.getString("TITLE_NUMBER"));
        t.setParcelId(rs.getString("PARCEL_ID"));
        t.setLegalDescription(rs.getString("LEGAL_DESCRIPTION"));
        t.setAreaSquareMetres(rs.getBigDecimal("AREA_SQM"));

        String status = rs.getString("STATUS");
        if (status != null) t.setStatus(TitleStatus.valueOf(status));

        String landUse = rs.getString("LAND_USE_TYPE");
        if (landUse != null) t.setLandUseType(LandUseType.valueOf(landUse));

        t.setStreetAddress(rs.getString("STREET_ADDRESS"));
        t.setSuburb(rs.getString("SUBURB"));
        t.setCity(rs.getString("CITY"));
        t.setStateProvince(rs.getString("STATE_PROVINCE"));
        t.setCountry(rs.getString("COUNTRY"));
        t.setPostalCode(rs.getString("POSTAL_CODE"));
        t.setLatitude(rs.getBigDecimal("LATITUDE"));
        t.setLongitude(rs.getBigDecimal("LONGITUDE"));
        t.setOwnerNationalId(rs.getString("OWNER_NATIONAL_ID"));
        t.setOwnerFullName(rs.getString("OWNER_FULL_NAME"));
        t.setOwnerContactEmail(rs.getString("OWNER_EMAIL"));
        t.setOwnerContactPhone(rs.getString("OWNER_PHONE"));
        t.setAssessedValue(rs.getBigDecimal("ASSESSED_VALUE"));
        t.setMarketValue(rs.getBigDecimal("MARKET_VALUE"));
        t.setCurrencyCode(rs.getString("CURRENCY_CODE"));

        Timestamp reg = rs.getTimestamp("REGISTRATION_DATE");
        if (reg != null) t.setRegistrationDate(reg.toInstant());

        Timestamp mod = rs.getTimestamp("LAST_MODIFIED_DATE");
        if (mod != null) t.setLastModifiedDate(mod.toInstant());

        t.setRegisteredBy(rs.getString("REGISTERED_BY"));
        t.setLastModifiedBy(rs.getString("LAST_MODIFIED_BY"));
        t.setHasLien(rs.getInt("HAS_LIEN") == 1);
        t.setHasMortgage(rs.getInt("HAS_MORTGAGE") == 1);
        t.setEncumbranceDetails(rs.getString("ENCUMBRANCE_DETAIL"));
        t.setRemarks(rs.getString("REMARKS"));
        return t;
    }

    private void setBigDecimal(PreparedStatement ps, int idx, BigDecimal val) throws SQLException {
        if (val != null) ps.setBigDecimal(idx, val);
        else ps.setNull(idx, Types.DECIMAL);
    }
}
