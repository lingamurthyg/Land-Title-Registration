# Land Title Registry System
### IBM WebSphere Application Server 9.x · Java 8 · Maven 3.x

A production-grade enterprise Land Title Registry System demonstrating the
full IBM WebSphere Application Server stack — purposely architected to showcase
WAS-specific APIs relevant to Concierto Modernize migration assessments.

---

## Project Structure

```
land-title-registry/
├── pom.xml                             ← Parent POM (multi-module)
├── ltr-ejb/                            ← EJB module (business logic)
│   └── src/main/
│       ├── java/com/trianz/ltr/
│       │   ├── model/
│       │   │   ├── LandTitle.java          Domain entity
│       │   │   └── TitleTransfer.java      Transfer/chain-of-title entity
│       │   ├── dao/
│       │   │   ├── LandTitleDAO.java        JDBC DAO (WAS DataSource)
│       │   │   └── TitleTransferDAO.java    Transfer history DAO
│       │   ├── ejb/
│       │   │   ├── LandTitleRegistryBean.java   @Stateless EJB (CMT)
│       │   │   ├── LandTitleRegistryLocal.java  @Local interface
│       │   │   ├── LandTitleRegistryRemote.java @Remote interface
│       │   │   └── LandTitleException.java      @ApplicationException
│       │   └── util/
│       │       ├── WASDataSourceUtil.java    WSDataSource + JNDI lookup
│       │       ├── WASTransactionUtil.java   UOWManager (WAS SPI)
│       │       └── TitleNumberGenerator.java  LTR-YYYY-REGION-SEQ format
│       └── resources/META-INF/
│           ├── ejb-jar.xml                 Standard EJB deployment descriptor
│           └── ibm-ejb-jar-bnd.xml         ⚠️ WAS-proprietary JNDI bindings
├── ltr-web/                            ← WAR module (servlets / UI)
│   └── src/main/
│       ├── java/com/trianz/ltr/servlet/
│       │   ├── TitleRegistryServlet.java   Front-controller REST servlet
│       │   └── HealthCheckServlet.java     WAS health check
│       └── webapp/
│           ├── WEB-INF/
│           │   ├── web.xml                  Standard web deployment descriptor
│           │   └── ibm-web-bnd.xml          ⚠️ WAS role→LDAP group mappings
│           ├── index.html                   API explorer / docs
│           └── pages/login.jsp              j_security_check form-based login
├── ltr-ear/                            ← EAR assembly module
│   └── src/main/application/META-INF/
│       ├── application.xml              Standard EAR descriptor
│       └── ibm-application-bnd.xml      ⚠️ WAS EAR-level security bindings
└── src/main/
    ├── sql/schema.sql                   DB2/Oracle/PostgreSQL schema + seed data
    └── scripts/configure-was-datasource.sh  wsadmin Jython DataSource script
```

---

## WAS-Specific APIs (Modernization Blockers)

| Severity | API / Artifact | Location | Open Liberty Replacement |
|----------|---------------|----------|--------------------------|
| 🔴 HIGH | `com.ibm.websphere.rsadapter.WSDataSource` | `WASDataSourceUtil.java` | `@Resource DataSource` + `server.xml <dataSource>` |
| 🔴 HIGH | `com.ibm.wsspi.uow.UOWManager` | `WASTransactionUtil.java` | `UserTransaction` / CDI `@Transactional` |
| 🔴 HIGH | `ibm-ejb-jar-bnd.xml` | `ltr-ejb/META-INF/` | `server.xml` or CDI injection |
| 🔴 HIGH | `ibm-web-bnd.xml` | `WEB-INF/` | `server.xml <application-bnd>` |
| 🔴 HIGH | `ibm-application-bnd.xml` | `ltr-ear/META-INF/` | `server.xml <application-bnd>` |
| 🟡 MED | Remote EJB over RMI-IIOP | `LandTitleRegistryRemote` | JAX-RS REST `@Path` resources |
| 🟡 MED | `SessionContext.getCallerPrincipal()` | `LandTitleRegistryBean` | MicroProfile `SecurityContext` |
| 🟡 MED | `j_security_check` FORM auth | `login.jsp` | MicroProfile JWT / OIDC |
| 🟢 LOW | `@Stateless` / CMT EJB | `LandTitleRegistryBean` | Supported in Liberty `ejb-3.2` |
| 🟢 LOW | `@RolesAllowed` | `LandTitleRegistryBean` | Supported in Liberty |

---

## Build

### Prerequisites
- Java 8 JDK
- Maven 3.6+
- IBM WebSphere 9.x installation (for `was_public.jar`)

### Install WAS Dependency
```bash
mvn install:install-file \
  -Dfile=/opt/WebSphere/AppServer/dev/was_public.jar \
  -DpomFile=/opt/WebSphere/AppServer/dev/was_public-9.0.0.pom
```

### Build the EAR
```bash
mvn clean package
# Output: ltr-ear/target/land-title-registry.ear
```

### Run Tests
```bash
mvn test
```

---

## Database Setup

```bash
# Create schema and seed data
db2 -f src/main/sql/schema.sql
```

---

## WAS DataSource Configuration

Run the wsadmin Jython script to configure the DB2 DataSource:
```bash
$WAS_HOME/bin/wsadmin.sh -lang jython \
  -f src/main/scripts/configure-was-datasource.sh
```

Or configure manually in the WAS Admin Console:
- Resources → JDBC → JDBC Providers → New (DB2 XA)
- Resources → JDBC → Data Sources → New
  - Name: `LandTitleDS`
  - JNDI: `jdbc/LandTitleDS`
  - Database: `LTRDB`

---

## Deploy to WAS

```bash
# Via wsadmin
$WAS_HOME/bin/wsadmin.sh -lang jython -c \
  "AdminApp.install('ltr-ear/target/land-title-registry.ear', \
   ['-appname', 'land-title-registry', '-contextroot', '/ltr'])"
```

Or drag-drop the EAR via WAS Admin Console:
- Applications → New Enterprise Application → Upload EAR

---

## API Reference

| Method | URL | Description | Required Role |
|--------|-----|-------------|---------------|
| `POST` | `/ltr/api/titles` | Register new title | OFFICER+ |
| `GET` | `/ltr/api/titles/{titleNumber}` | Get by title number | OFFICER+ |
| `GET` | `/ltr/api/titles?owner={nationalId}` | Get by owner | OFFICER+ |
| `GET` | `/ltr/api/titles?status=ACTIVE` | Filter by status | OFFICER+ |
| `GET` | `/ltr/api/titles?q={keyword}` | Full-text search | OFFICER+ |
| `PUT` | `/ltr/api/titles/{titleNumber}` | Update title | OFFICER+ |
| `POST` | `/ltr/api/transfers` | Initiate transfer | OFFICER+ |
| `GET` | `/ltr/api/transfers?title={num}` | Transfer history | OFFICER+ |
| `GET` | `/ltr/api/transfers/pending` | Pending approvals | SUPERVISOR+ |
| `POST` | `/ltr/api/transfers/{id}/approve` | Approve transfer | SUPERVISOR+ |
| `POST` | `/ltr/api/transfers/{id}/reject` | Reject transfer | SUPERVISOR+ |
| `GET` | `/ltr/health` | Health check | Public |

---

## Security Architecture

```
Browser / Client
       ↓  HTTPS (TLS enforced by web.xml transport-guarantee)
WAS HTTP Server
       ↓  j_security_check → WAS JAAS Login Module
LDAP Server  (cn=registry-officers,ou=groups,dc=lands,dc=gov,dc=local)
       ↓  Principal propagated to EJB container
LandTitleRegistryBean
       @RolesAllowed  ←→  SessionContext.getCallerPrincipal()
       ContainerManagedTransaction (XA via DB2 JDBC)
       ↓
DB2 Database (LTRDB)
```

---

*Built by Trianz — Concierto Modernize reference application*
