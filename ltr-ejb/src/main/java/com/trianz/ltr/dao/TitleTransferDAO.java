package com.trianz.ltr.dao;

import com.trianz.ltr.model.TitleTransfer;
import com.trianz.ltr.model.TitleTransfer.TransferStatus;
import com.trianz.ltr.model.TitleTransfer.TransferType;
import com.trianz.ltr.util.CloudDataSourceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * TitleTransferDAO - JDBC DAO for TITLE_TRANSFER_HISTORY table.
 * 
 * Cloud-native improvements:
 * - Uses CloudDataSourceUtil with HikariCP connection pooling
 * - Uses java.time.Instant for all timestamps (UTC)
 * - Structured logging with SLF4J
 * - Proper exception handling for cloud environments
 */
public class TitleTransferDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(TitleTransferDAO.class);

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
        "APPROVED_DATE=?, REJECTION_REASON=? WHERE TRANSFER_ID=?";

    // ── Insert ─────────────────────────────────────────────────────────────────

    public Long insert(TitleTransfer tr) throws SQLException {
        Connection conn = null;
        try {
            conn = CloudDataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1,  tr.getTitleNumber());
                ps.setString(2,  tr.getPreviousOwnerNationalId());
                ps.setString(3,  tr.getPreviousOwnerName());
                ps.setString(4,  tr.getNewOwnerNationalId());
                ps.setString(5,  tr.getNewOwnerName());
                ps.setString(6,  tr.getNewOwnerContactEmail());
                ps.setString(7,  tr.getNewOwnerContactPhone());
                ps.setString(8,  tr.getTransferType() != null ? tr.getTransferType().name() : null);
                ps.setString(9,  tr.getTransferStatus() != null
                        ? tr.getTransferStatus().name() : TransferStatus.INITIATED.name());
                setBigDecimal(ps, 10, tr.getTransferPrice());
                ps.setString(11, tr.getCurrencyCode());
                setBigDecimal(ps, 12, tr.getStampDutyPaid());
                ps.setTimestamp(13, tr.getTransferDate() != null
                        ? Timestamp.from(tr.getTransferDate()) : Timestamp.from(Instant.now()));
                ps.setTimestamp(14, tr.getEffectiveDate() != null
                        ? Timestamp.from(tr.getEffectiveDate()) : null);
                ps.setString(15, tr.getTransferDeedNumber());
                ps.setString(16, tr.getNotaryNationalId());
                ps.setString(17, tr.getNotaryName());
                ps.setString(18, tr.getInitiatedBy());
                ps.setString(19, tr.getRemarks());
                ps.executeUpdate();

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Long id = generatedKeys.getLong(1);
                        tr.setTransferId(id);
                        LOGGER.info("Inserted TitleTransfer with ID: {}", id);
                        return id;
                    }
                }
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to insert TitleTransfer for title: {}", tr.getTitleNumber(), e);
            throw new SQLException("Insert TitleTransfer failed: " + e.getMessage(), e);
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }
    }

    // ── Find history for a title ───────────────────────────────────────────────

    public List<TitleTransfer> findByTitleNumber(String titleNumber) throws SQLException {
        Connection conn = null;
        try {
            conn = CloudDataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_TITLE)) {
                ps.setString(1, titleNumber);
                try (ResultSet rs = ps.executeQuery()) {
                    List<TitleTransfer> list = new ArrayList<>();
                    while (rs.next()) list.add(mapRow(rs));
                    LOGGER.debug("Found {} transfers for title: {}", list.size(), titleNumber);
                    return list;
                }
            }
        } catch (Exception e) {
            LOGGER.error("findByTitleNumber(transfer) failed for: {}", titleNumber, e);
            throw new SQLException("findByTitleNumber(transfer) failed: " + e.getMessage(), e);
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }
    }

    // ── Find pending approvals ─────────────────────────────────────────────────

    public List<TitleTransfer> findPendingApprovals() throws SQLException {
        Connection conn = null;
        try {
            conn = CloudDataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_PENDING);
                 ResultSet rs = ps.executeQuery()) {
                List<TitleTransfer> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                LOGGER.debug("Found {} pending transfers", list.size());
                return list;
            }
        } catch (Exception e) {
            LOGGER.error("findPendingApprovals failed", e);
            throw new SQLException("findPendingApprovals failed: " + e.getMessage(), e);
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }
    }

    // ── Approve / Reject ───────────────────────────────────────────────────────

    public int updateStatus(Long transferId, TransferStatus status,
                            String approvedBy, String rejectionReason) throws SQLException {
        Connection conn = null;
        try {
            conn = CloudDataSourceUtil.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {
                ps.setString(1, status.name());
                ps.setString(2, approvedBy);
                ps.setTimestamp(3, Timestamp.from(Instant.now()));
                ps.setString(4, rejectionReason);
                ps.setLong(5, transferId);
                int rows = ps.executeUpdate();
                LOGGER.info("Updated transfer {} status to {}, rows: {}", transferId, status, rows);
                return rows;
            }
        } catch (Exception e) {
            LOGGER.error("updateStatus(transfer) failed for ID: {}", transferId, e);
            throw new SQLException("updateStatus(transfer) failed: " + e.getMessage(), e);
        } finally {
            CloudDataSourceUtil.closeQuietly(conn);
        }
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
        if (status != null) tr.setTransferStatus(TransferStatus.valueOf(status));

        tr.setTransferPrice(rs.getBigDecimal("TRANSFER_PRICE"));
        tr.setCurrencyCode(rs.getString("CURRENCY_CODE"));
        tr.setStampDutyPaid(rs.getBigDecimal("STAMP_DUTY_PAID"));

        Timestamp td = rs.getTimestamp("TRANSFER_DATE");
        if (td != null) tr.setTransferDate(td.toInstant());

        Timestamp ed = rs.getTimestamp("EFFECTIVE_DATE");
        if (ed != null) tr.setEffectiveDate(ed.toInstant());

        tr.setTransferDeedNumber(rs.getString("DEED_NUMBER"));
        tr.setNotaryNationalId(rs.getString("NOTARY_NATIONAL_ID"));
        tr.setNotaryName(rs.getString("NOTARY_NAME"));
        tr.setInitiatedBy(rs.getString("INITIATED_BY"));
        tr.setApprovedBy(rs.getString("APPROVED_BY"));

        Timestamp ad = rs.getTimestamp("APPROVED_DATE");
        if (ad != null) tr.setApprovedDate(ad.toInstant());

        tr.setRejectionReason(rs.getString("REJECTION_REASON"));
        tr.setRemarks(rs.getString("REMARKS"));
        return tr;
    }

    private void setBigDecimal(PreparedStatement ps, int idx, BigDecimal val) throws SQLException {
        if (val != null) ps.setBigDecimal(idx, val);
        else ps.setNull(idx, Types.DECIMAL);
    }
}
