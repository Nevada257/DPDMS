=====================================================================
 DPDMS - Rushinga Provincial Disaster Monitoring and Management System
 HCS201 / HCC201 / HAI201 Object Oriented Programming - Group Project
=====================================================================

This file explains how to get the system running on a Windows computer
and lists every login. Full technical details are in README.md and
docs\DPDMS_System_Assignment.docx.


---------------------------------------------------------------------
1. WHAT YOU NEED INSTALLED
---------------------------------------------------------------------
  - Java JDK 21 or newer
  - MySQL Server 8 (running, user "root")
  - Node.js 20 or newer
  - Windows Terminal (optional: puts every service in one window as tabs)

No internet is needed after the first start, except for email and
WhatsApp alerts.


---------------------------------------------------------------------
2. FIRST-TIME SETUP (once per computer)
---------------------------------------------------------------------
Open Command Prompt in this project folder (the folder with this file).

Step 1 - Create the settings file:

    copy dpdms.env.example dpdms.env
    notepad dpdms.env

Step 2 - In dpdms.env fill in at least these two lines and save:

    DB_PASSWORD=your MySQL root password
    JWT_SECRET=any random text of at least 32 characters

    Example JWT_SECRET (any long text works):
    JWT_SECRET=Rushinga-DPDMS-2026-group-project-jwt-signing-key-0123456789abc

    Rules: one setting per line, no spaces after "=", and each setting
    only once (if a setting appears twice, the lower line wins).

Step 3 - Load the database. EITHER

  a) the full exported database (all data the group entered):

       mysql -u root -p < database\export\dpdms_database_dump.sql

  OR

  b) the clean demo data (about 60 sample incidents):

       powershell -ExecutionPolicy Bypass -File scripts\load-seed-data.ps1

  If "mysql" is not recognised, use option (b): the script finds
  MySQL by itself.


---------------------------------------------------------------------
3. STARTING AND STOPPING
---------------------------------------------------------------------
  Start everything ...... double-click start-dpdms.bat
                          (wait about 3-5 minutes the first time)
  Stop everything ....... double-click stop-dpdms.bat

  Open the application .. http://localhost:5173
  Check services ........ http://localhost:8761  (all 10 must say UP)

  Run only some hazards (Eureka, auth, gateway and front end always start):

    powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1 -Only flood
    powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1 -Only flood,fire -WithExtras


---------------------------------------------------------------------
4. LOGINS (all demo accounts use the password:  password123 )
---------------------------------------------------------------------
  Username              Role         Hazard     Ward     Can do
  --------------------  -----------  ---------  -------  ------------------------------
  flood_recorder        Recorder     Flood      Ward 1   Capture/edit own flood records
  flood_recorder_w2     Recorder     Flood      Ward 2   Same, Ward 2 (ward scoping)
  drought_recorder      Recorder     Drought    Ward 1   Capture drought records
  fire_recorder         Recorder     Fire       Ward 1   Capture fire records
  zoonotic_recorder     Recorder     Zoonotic   Ward 1   Capture zoonotic records
  mining_recorder       Recorder     Mining     Ward 1   Capture mining records

  flood_supervisor      Supervisor   Flood      any      Approve / reject / return floods
  drought_supervisor    Supervisor   Drought    any      Same for drought
  fire_supervisor       Supervisor   Fire       any      Same for fire
  zoonotic_supervisor   Supervisor   Zoonotic   any      Same for zoonotic
  mining_supervisor     Supervisor   Mining     any      Same for mining

  provincial_admin      Admin        All        any      Read-only, sees pending records,
                                                         "Send test alert" button
  national_user         National     All        any      Read-only, approved records,
                                                         dashboard, map and reports

  Note: a recorder's Ward box is locked to their own ward on purpose
  (ward-level scoping). Use flood_recorder_w2 to record in Ward 2.

  Other passwords:
  - MySQL root ........... the one you set when installing MySQL
                           (put it in DB_PASSWORD in dpdms.env)
  - Email / WhatsApp keys .. not included in this submission on purpose:
                           they are private keys of group members.
                           See section 5 to use your own.


---------------------------------------------------------------------
5. EMAIL AND WHATSAPP ALERTS (optional)
---------------------------------------------------------------------
Without these settings the system still works: alerts are checked and
shown in "Recent alerts sent" as SIMULATED.

Email (Gmail) - add to dpdms.env:
    MAIL_HOST=smtp.gmail.com
    MAIL_PORT=587
    MAIL_USERNAME=your.address@gmail.com
    MAIL_PASSWORD=16-letter Gmail App Password, no spaces
    MAIL_FROM=your.address@gmail.com
  (Google Account > Security > 2-Step Verification on > App passwords)

WhatsApp (Green API, free) - add to dpdms.env:
    GREENAPI_API_URL=apiUrl from console.green-api.com
    GREENAPI_ID_INSTANCE=idInstance
    GREENAPI_API_TOKEN=apiTokenInstance
  (create an instance, scan its QR code with WhatsApp > Linked devices)

Who receives the alerts:
    ALERT_DEMO_EMAIL=receiver@gmail.com
    ALERT_DEMO_PHONE=+263XXXXXXXXX   (country code, no leading 0)

Restart the system, then test both channels:
    powershell -ExecutionPolicy Bypass -File scripts\test-alert.ps1
Both lines should say SENT.


---------------------------------------------------------------------
6. QUICK DEMO
---------------------------------------------------------------------
  1. flood_recorder: capture a flood in Ward 1 with peak water level 3.5
     -> saved as PENDING, alert email + WhatsApp sent.
  2. national_user: the flood is not visible yet.
  3. fire_supervisor: trying to open floods is refused (403).
  4. flood_supervisor: approve it.
  5. national_user: it now appears on the map, trend and alert log;
     download a PDF report.


---------------------------------------------------------------------
7. COMMON PROBLEMS
---------------------------------------------------------------------
  WeakKeyException ................. JWT_SECRET shorter than 32 characters
  "Access denied" for MySQL ......... wrong DB_PASSWORD in dpdms.env
  A service missing in Eureka ....... look at its window/tab for the red error
  "Forbidden" when saving ........... recorder used another ward/hazard
  Alerts FAILED ..................... check the reason in brackets
  Blank page at localhost:5173 ...... wait for the frontend to say "ready", press F5
  Test script cannot log in ......... system not started yet; wait, check :8761
