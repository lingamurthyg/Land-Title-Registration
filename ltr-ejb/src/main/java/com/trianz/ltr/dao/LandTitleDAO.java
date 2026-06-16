package com.trianz.ltr.dao;

import com.trianz.ltr.model.LandTitle;
import com.trianz.ltr.util.DataSourceUtil;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.Instant;
import java.time.ZoneOffset;
 * Obtains connections from the WAS-managed DataSource (JNDI: jdbc/LandTitleDS).
 * Uses standard JDBC; transactions are managed by the calling EJB (CMT).
 *
 * DATABASE SCHEMA (Oracle / DB2 / PostgreSQL compatible):
 *
 *   CREATE TABLE LAND_TITLE (
 *     TITLE_NUMBER       VARCHAR(30)    PRIMARY KEY,
 *     PARCEL_ID          VARCHAR(50)    NOT NULL UNIQUE,
 *     LEGAL_DESCRIPTION  CLOB,
 *     AREA_SQM           DECIMAL(15,4),
 *     STATUS             VARCHAR(20)    DEFAULT 'PENDING',
 *     LAND_USE_TYPE      VARCHAR(30),
 *     STREET_ADDRESS     VARCHAR(200),
 *     SUBURB             VARCHAR(100),
 *     CITY               VARCHAR(100),
 *     STATE_PROVINCE     VARCHAR(100),
 *     COUNTRY            VARCHAR(100),
 *     POSTAL_CODE        VARCHAR(20),
 *     LATITUDE           DECIMAL(10,7),
 *     LONGITUDE          DECIMAL(10,7),
 *     OWNER_NATIONAL_ID  VARCHAR(50)    NOT NULL,
 *     OWNER_FULL_NAME    VARCHAR(200)   NOT NULL,
 *     OWNER_EMAIL        VARCHAR(200),
 *     OWNER_PHONE        VARCHAR(50),
 *     ASSESSED_VALUE     DECIMAL(18,2),
 *     MARKET_VALUE       DECIMAL(18,2),
 *     CURRENCY_CODE      CHAR(3)        DEFAULT 'USD',
 *     REGISTRATION_DATE  TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
 *     LAST_MODIFIED_DATE TIMESTAMP,
 *     REGISTERED_BY      VARCHAR(100),
 *     LAST_MODIFIED_BY   VARCHAR(100),
 *     HAS_LIEN           SMALLINT       DEFAULT 0,
 *     HAS_MORTGAGE       SMALLINT       DEFAULT 0,
 *     ENCUMBRANCE_DETAIL CLOB,
 *     REMARKS            VARCHAR(1000)
 *   );
 */
public class LandTitleDAO {
@Repository
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

    private static final String SQL_COUNT_BY_STATUS =
        "SELECT STATUS, COUNT(*) AS CNT FROM LAND_TITLE GROUP BY STATUS";

    private static final String SQL_SEARCH =
        "SELECT * FROM LAND_TITLE WHERE " +
        "(UPPER(OWNER_FULL_NAME) LIKE UPPER(?) OR UPPER(CITY) LIKE UPPER(?) " +
        "OR UPPER(PARCEL_ID) LIKE UPPER(?)) ORDER BY REGISTRATION_DATE DESC";

    // ── Insert ─────────────────────────────────────────────────────────────────

    public void insert(LandTitle t) throws SQLException {
        Connection conn = null;
        try {
            conn = DataSourceUtil.getConnection();
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
            conn = DataSourceUtil.getConnection();
                ps.setString(18, t.getOwnerContactPhone());
                setBigDecimal(ps, 19, t.getAssessedValue());
                setBigDecimal(ps, 20, t.getMarketValue());
                ps.setString(21, t.getCurrencyCode());
                ps.setTimestamp(22, t.getRegistrationDate() != null
                        ? new Timestamp(t.getRegistrationDate().getTime()) : new Timestamp(System.currentTimeMillis()));
                ps.setString(23, t.getRegisteredBy());
                ps.setInt(24, t.isHasLien() ? 1 : 0);
                ps.setInt(25, t.isHasMortgage() ? 1 : 0);
                ps.setString(26, t.getEncumbranceDetails());
                ps.setString(27, t.getRemarks());
                ps.executeUpdate();
                ps.setTimestamp(22, t.getRegistrationDate() != null
                        ? Timestamp.from(t.getRegistrationDate()) : Timestamp.from(Instant.now()));
        }
    }

    // ── Find by Title Number ───────────────────────────────────────────────────

    public LandTitle findByTitleNumber(String titleNumber) throws SQLException {
        Connection conn = null;
            DataSourceUtil.closeQuietly(conn);
            }
        } catch (Exception e) {
            conn = DataSourceUtil.getConnection();

    public LandTitle findByParcelId(String parcelId) throws SQLException {
        Connection conn = null;
        try {
            DataSourceUtil.closeQuietly(conn);
        } catch (Exception e) {
            throw new SQLException("findByParcelId failed: " + e.getMessage(), e);
            conn = DataSourceUtil.getConnection();
    public List<LandTitle> findByOwner(String ownerNationalId) throws SQLException {
        Connection conn = null;
        try {
            conn = DataSourceUtil.getConnection();
            DataSourceUtil.closeQuietly(conn);
            }
        } catch (Exception e) {
            conn = DataSourceUtil.getConnection();

    public List<LandTitle> findByStatus(TitleStatus status) throws SQLException {
        Connection conn = null;
        try {
            conn = DataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_STATUS)) {
                ps.setString(1, status.name());
            DataSourceUtil.closeQuietly(conn);
            throw new SQLException("findByStatus failed: " + e.getMessage(), e);
        } finally {
            conn = DataSourceUtil.getConnection();
        Connection conn = null;
        String pattern = "%" + keyword + "%";
        try {
            conn = DataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_SEARCH)) {
                ps.setString(1, pattern);
                ps.setString(2, pattern);
            DataSourceUtil.closeQuietly(conn);
        } catch (Exception e) {
            throw new SQLException("search failed: " + e.getMessage(), e);
        } finally {
            conn = DataSourceUtil.getConnection();
        Connection conn = null;
        try {
            conn = DataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
                ps.setString(1,  t.getStatus().name());
                ps.setString(2,  t.getOwnerNationalId());
                ps.setString(3,  t.getOwnerFullName());
                ps.setString(4,  t.getOwnerContactEmail());
                ps.setString(5,  t.getOwnerContactPhone());
            DataSourceUtil.closeQuietly(conn);
                ps.setString(12, t.getEncumbranceDetails());
                ps.setString(13, t.getRemarks());
            conn = DataSourceUtil.getConnection();
        }
    }

                ps.setTimestamp(8, t.getLastModifiedDate() != null
                        ? Timestamp.from(t.getLastModifiedDate()) : Timestamp.from(Instant.now()));
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                ps.setString(3, modifiedBy);
                ps.setString(4, titleNumber);
                return ps.executeUpdate();
            }
        } catch (Exception e) {
            DataSourceUtil.closeQuietly(conn);

    public int delete(String titleNumber) throws SQLException {
            conn = DataSourceUtil.getConnection();
                ps.setTimestamp(2, Timestamp.from(Instant.now()));
        }
    }
            DataSourceUtil.closeQuietly(conn);
        t.setLegalDescription(rs.getString("LEGAL_DESCRIPTION"));
        t.setAreaSquareMetres(rs.getBigDecimal("AREA_SQM"));
            conn = DataSourceUtil.getConnection();
        t.setStreetAddress(rs.getString("STREET_ADDRESS"));
        t.setSuburb(rs.getString("SUBURB"));
            DataSourceUtil.closeQuietly(conn);
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
        if (reg != null) t.setRegistrationDate(reg.toInstant());
        if (mod != null) t.setLastModifiedDate(mod.toInstant());
