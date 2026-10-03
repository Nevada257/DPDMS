# Database export

`dpdms_database_dump.sql` is a full export (structure and data) of all seven DPDMS
databases: auth_db, flood_db, drought_db, fire_db, zoonotic_db, mining_accident_db
and alert_db. It is created by `package-submission.bat` in the project folder.

To restore it on another computer:

```
mysql -u root -p < database\export\dpdms_database_dump.sql
```

This replaces those seven databases with the exported copy, including the demo users
(password `password123`) and every incident, approval and alert log entry.
