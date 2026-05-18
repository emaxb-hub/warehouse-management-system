# warehouse-management-system
Developed a comprehensive Warehouse and Inventory Management System using Java to efficiently manage stock records, product information, and warehouse operations. Designed an interactive graphical user interface using JavaFX/Swing and integrated the application with SQL databases through JDBC for real-time data handling and persistent storage
WarehouseWare is a Java-based warehouse management system with a layered architecture (domain, application, persistence, ui).

Tech Stack
Java
Maven
SQLite (local database)
JavaFX/CSS UI resources
Project Structure
src/main/java/com/warehouseware/domain - core domain models
src/main/java/com/warehouseware/application - business logic and services
src/main/java/com/warehouseware/persistence - database and repository layer
src/main/java/com/warehouseware/ui - application UI
src/main/resources - styles and fonts
Prerequisites
JDK 17+ (or the version set in pom.xml)
Maven 3.8+
Build
mvn clean package
Run
mvn exec:java -Dexec.mainClass="com.warehouseware.Main"

The project includes a local SQLite database file: warehouseware.db.
IntelliJ setup guidance is available in INTELLIJ_SETUP.md.
