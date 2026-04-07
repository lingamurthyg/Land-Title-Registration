#!/bin/bash
# =============================================================================
# configure-was-datasource.py  (wsadmin Jython script)
# =============================================================================
# Configures the JDBC DataSource for Land Title Registry in IBM WebSphere.
# Run via wsadmin:
#   $WAS_HOME/bin/wsadmin.sh -lang jython -f configure-was-datasource.py
#
# WAS JNDI Name bound: jdbc/LandTitleDS
# =============================================================================
# NOTE: This is a Jython script for wsadmin — rename to .py and run with wsadmin

"""
import AdminConfig
import AdminControl
import sys

print "=== Configuring Land Title Registry DataSource on WAS ==="

# ── 1. Create JDBC Provider (DB2) ─────────────────────────────────────────────
cell     = AdminControl.getCell()
node     = AdminControl.getNode()
server   = 'server1'

serverEntry = AdminConfig.getid('/Cell:%s/Node:%s/Server:%s/' % (cell, node, server))

jdbcProvider = AdminConfig.create('JDBCProvider', serverEntry, [
    ['name',                'DB2 Universal JDBC Driver Provider'],
    ['implementationClassName', 'com.ibm.db2.jcc.DB2XADataSource'],
    ['classpath',           '${DB2_JDBC_DRIVER_PATH}/db2jcc4.jar:${DB2_JDBC_DRIVER_PATH}/db2jcc_license_cu.jar'],
    ['providerType',        'DB2 Universal JDBC Driver Provider (XA)'],
    ['xa',                  'true'],
    ['description',         'IBM DB2 XA JDBC Provider for Land Title Registry']
])
print "JDBC Provider created: " + str(jdbcProvider)

# ── 2. Create DataSource ───────────────────────────────────────────────────────
dataSource = AdminConfig.create('DataSource', jdbcProvider, [
    ['name',          'LandTitleDS'],
    ['jndiName',      'jdbc/LandTitleDS'],
    ['description',   'Land Title Registry Database'],
    ['authDataAlias', cell + '/LandTitleDBAuth'],
    ['statementCacheSize', '50'],
    ['datasourceHelperClassname', 'com.ibm.websphere.rsadapter.DB2UniversalDataStoreHelper']
])
print "DataSource created: " + str(dataSource)

# ── 3. Set DB2 Connection Properties ──────────────────────────────────────────
propertySet = AdminConfig.create('J2EEResourcePropertySet', dataSource, [])
AdminConfig.create('J2EEResourceProperty', propertySet, [
    ['name', 'databaseName'], ['value', 'LTRDB'], ['type', 'java.lang.String']
])
AdminConfig.create('J2EEResourceProperty', propertySet, [
    ['name', 'serverName'],   ['value', 'db-server.lands.gov.local'], ['type', 'java.lang.String']
])
AdminConfig.create('J2EEResourceProperty', propertySet, [
    ['name', 'portNumber'],   ['value', '50000'], ['type', 'java.lang.Integer']
])
AdminConfig.create('J2EEResourceProperty', propertySet, [
    ['name', 'driverType'],   ['value', '4'], ['type', 'java.lang.Integer']
])

# ── 4. Connection Pool Settings ───────────────────────────────────────────────
pool = AdminConfig.showAttribute(dataSource, 'connectionPool')
AdminConfig.modify(pool, [
    ['minConnections',        '5'],
    ['maxConnections',        '50'],
    ['connectionTimeout',     '180'],
    ['unusedTimeout',         '1800'],
    ['reapTime',              '180'],
    ['agedTimeout',           '0'],
    ['purgePolicy',           'EntirePool']
])

# ── 5. JAAS Auth Alias for DB credentials ─────────────────────────────────────
security = AdminConfig.getid('/Cell:%s/Security:/' % cell)
AdminConfig.create('JAASAuthData', security, [
    ['alias',    cell + '/LandTitleDBAuth'],
    ['userId',   'ltr_app_user'],
    ['password', '{xor}ltr_password_encoded_here']
])

AdminConfig.save()
print "=== DataSource configuration saved successfully ==="
"""
