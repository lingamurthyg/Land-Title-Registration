package com.trianz.ltr.dao;

import com.trianz.ltr.model.TitleTransfer;
import com.trianz.ltr.util.DataSourceUtil;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.Instant;
 *   CREATE TABLE TITLE_TRANSFER_HISTORY (
 *     TRANSFER_ID           BIGINT         PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
 *     TITLE_NUMBER          VARCHAR(30)    NOT NULL REFERENCES LAND_TITLE(TITLE_NUMBER),
 *     PREV_OWNER_NATIONAL_ID VARCHAR(50),
 *     PREV_OWNER_NAME       VARCHAR(200),
 *     NEW_OWNER_NATIONAL_ID  VARCHAR(50)   NOT NULL,
 *     NEW_OWNER_NAME         VARCHAR(200)  NOT NULL,
 *     NEW_OWNER_EMAIL        VARCHAR(200),
 *     NEW_OWNER_PHONE        VARCHAR(50),
 *     TRANSFER_TYPE          VARCHAR(30),
 *     TRANSFER_STATUS        VARCHAR(30)   DEFAULT 'INITIATED',
 *     TRANSFER_PRICE         DECIMAL(18,2),
 *     CURRENCY_CODE          CHAR(3)       DEFAULT 'USD',
 *     STAMP_DUTY_PAID        DECIMAL(18,2),
 *     TRANSFER_DATE          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
 *     EFFECTIVE_DATE         TIMESTAMP,
 *     DEED_NUMBER            VARCHAR(100),
 *     NOTARY_NATIONAL_ID     VARCHAR(50),
 *     NOTARY_NAME            VARCHAR(200),
 *     INITIATED_BY           VARCHAR(100),
 *     APPROVED_BY            VARCHAR(100),
@Repository

    private static final Logger LOGGER = Logger.getLogger(TitleTransferDAO.class.getName());

    private static final String SQL_INSERT =
        "INSERT INTO TITLE_TRANSFER_HISTORY " +
        "(TITLE_NUMBER, PREV_OWNER_NATIONAL_ID, PREV_OWNER_NAME, NEW_OWNER_NATIONAL_ID, " +
        "NEW_OWNER_NAME, NEW_OWNER_EMAIL, NEW_OWNER_PHONE, TRANSFER_TYPE, TRANSFER_STATUS, " +
        "TRANSFER_PRICE, CURRENCY_CODE, STAMP_DUTY_PAID, TRANSFER_DATE, EFFECTIVE_DATE, " +
        "DEED_NUMBER, NOTARY_NATIONAL_ID, NOTARY_NAME, INITIATED_BY, REMARKS) " +
        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String SQL_SELECT_BY_TITLE =
        "SELECT * FROM TITLE_TRANSFER_HISTORY WHERE TITLE_NUMBER = ? ORDER BY TRANSFER_DATE DESC";

    private static final String SQL_SELECT_BY_ID =
        "SELECT * FROM TITLE_TRANSFER_HISTORY WHERE TRANSFER_ID = ?";

    private static final String SQL_SELECT_PENDING =
        "SELECT * FROM TITLE_TRANSFER_HISTORY WHERE TRANSFER_STATUS = 'UNDER_REVIEW' " +
        "ORDER BY TRANSFER_DATE ASC";

    private static final String SQL_UPDATE_STATUS =
        "UPDATE TITLE_TRANSFER_HISTORY SET TRANSFER_STATUS=?, APPROVED_BY=?, " +
            conn = DataSourceUtil.getConnection();
            conn = DataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1,  tr.getTitleNumber());
                ps.setString(2,  tr.getPreviousOwnerNationalId());
                ps.setString(3,  tr.getPreviousOwnerName());
                ps.setString(4,  tr.getNewOwnerNationalId());
                ps.setString(5,  tr.getNewOwnerName());
                ps.setTimestamp(13, tr.getTransferDate() != null
                        ? Timestamp.from(tr.getTransferDate()) : Timestamp.from(Instant.now()));
                ps.setTimestamp(14, tr.getEffectiveDate() != null
                        ? Timestamp.from(tr.getEffectiveDate()) : null);
                ps.setTimestamp(14, tr.getEffectiveDate() != null
                        ? new Timestamp(tr.getEffectiveDate().getTime()) : null);
                ps.setString(15, tr.getTransferDeedNumber());
                ps.setString(16, tr.getNotaryNationalId());
                ps.setString(17, tr.getNotaryName());
                ps.setString(18, tr.getInitiatedBy());
                ps.setString(19, tr.getRemarks());
                ps.executeUpdate();

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
            DataSourceUtil.closeQuietly(conn);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to insert TitleTransfer", e);
            conn = DataSourceUtil.getConnection();

    public List<TitleTransfer> findByTitleNumber(String titleNumber) throws SQLException {
        Connection conn = null;
        try {
            conn = DataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_TITLE)) {
                ps.setString(1, titleNumber);
            DataSourceUtil.closeQuietly(conn);
            throw new SQLException("findByTitleNumber(transfer) failed: " + e.getMessage(), e);
        } finally {
            conn = DataSourceUtil.getConnection();
        Connection conn = null;
        try {
            conn = DataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_PENDING);
                 ResultSet rs = ps.executeQuery()) {
            DataSourceUtil.closeQuietly(conn);
            DataSourceUtil.closeQuietly(conn);
        }
    }
            conn = DataSourceUtil.getConnection();
                ps.setTimestamp(3, Timestamp.from(Instant.now()));
                ps.setString(4, rejectionReason);
                ps.setLong(5, transferId);
            DataSourceUtil.closeQuietly(conn);
    }

    // ── Row Mapper ─────────────────────────────────────────────────────────────

    private TitleTransfer mapRow(ResultSet rs) throws SQLException {
        TitleTransfer tr = new TitleTransfer();
        tr.setTransferId(rs.getLong("TRANSFER_ID"));
        tr.setTitleNumber(rs.getString("TITLE_NUMBER"));
        tr.setPreviousOwnerNationalId(rs.getString("PREV_OWNER_NATIONAL_ID"));
        tr.setPreviousOwnerName(rs.getString("PREV_OWNER_NAME"));
        tr.setNewOwnerNationalId(rs.getString("NEW_OWNER_NATIONAL_ID"));
        tr.setNewOwnerName(rs.getString("NEW_OWNER_NAME"));
        tr.setNewOwnerContactEmail(rs.getString("NEW_OWNER_EMAIL"));
        tr.setNewOwnerContactPhone(rs.getString("NEW_OWNER_PHONE"));

        String type = rs.getString("TRANSFER_TYPE");
        if (type != null) tr.setTransferType(TransferType.valueOf(type));

        String status = rs.getString("TRANSFER_STATUS");
        if (td != null) tr.setTransferDate(td.toInstant());
        if (ed != null) tr.setEffectiveDate(ed.toInstant());
        tr.setNotaryNationalId(rs.getString("NOTARY_NATIONAL_ID"));
        tr.setNotaryName(rs.getString("NOTARY_NAME"));
        if (ad != null) tr.setApprovedDate(ad.toInstant());
        tr.setRemarks(rs.getString("REMARKS"));
        return tr;
    }

    private void setBigDecimal(PreparedStatement ps, int idx, BigDecimal val) throws SQLException {
        if (val != null) ps.setBigDecimal(idx, val);
        else ps.setNull(idx, Types.DECIMAL);
    }
}
