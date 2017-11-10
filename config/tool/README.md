# Configuration file
All available settings are located in the [application.yml](application.yml) file. The file in this directory should be used as a template and appropriate values should be populated into a custom file.

## How to use custom file
* Ensure that the custom configuration file is in the same directory as the final jar file
```
cd target
ls
application.yml  migration-tool.jar
```
* Set path to the custom configuration file as a parameter in the following way:
```
-Dspring.config.location=config/tool/application.yml
```
## spring section
```
spring:
    main:
        banner-mode: (OFF|CONSOLE|LOG)
    batch:
        job:
            enabled: boolean
```
In this section you can provide standard spring properties that could be read by [Spring](https://spring.io/) library. Currently the template sets two things:
* the Spring banner will be written only to log file. In other words when a user run the application there will be no Spring banner on default output. There will be addictional file **log** in which there will be more details messages.
* By default we disabled a Spring Batch job that would execute ETL process. For now the job is manually executed by the tool and this property should be set to **false**
## tool section
```
tool:
    tableNames:
    parameters:
    configuration:
    mapping:
    exclude:
```
This section is used only by the tool and it is devided into smaller sections
* tableNames - set appropriate table name for tables that wre used by the tool.
* parameters - parameters that would be used during ETL process.
* configuration - this section contains details about SCM/OpenLMIS databases and additionally Spring Batch settins
* mapping - in this section a user can change the way SCM data will be handled. For example if SCM facility code need to be changed before it will be used then the user should add single line which will say that the given facility code should changed to another facility code.
* exclude - set what data should be excluded by the migration tool.

#### parameters section
```
tool:
    parameters:
        startDate: YYYY-MM-DD
        endDate: YYYY-MM-DD
        creator: string
        requestedQuantityExplanation: string
        timeZone: string
        updateExisting: boolean
        orderNumberConfiguration:
            orderNumberPrefix: string
            includeOrderNumberPrefix: boolean
            includeProgramCode: boolean
            includeTypeSuffix: boolean
        requisitionTemplates:
            - program: string
              numberOfPeriodsToAverage: integer
              columns:
                  string:
                      name: string
                      label: string
                      source: (USER_INPUT|CALCULATED|REFERENCE_DATA)
                      displayed: boolean
                      definition: string
                      displayOrder: integer
```
The following list explains all settings in the *parameters* section:
* start/end date - set from what period data should be migrated into OpenLMIS system. By default the start date is set to today date minus 5 years and with first day of month. The end date is set to today date with last day of month. The format for dates is the following: YYYY-MM-DD
* creator - when a requisition is created we need to set who create/submit/authorize/approve and so on. This field should contain a username.
* requestedQuantityExplanation - the value of this field will be populated into *Requested Quantity Explanation* column in each requisition.
* timeZone - set time zone for dates
* updateExisting - determine whether the tool should update or skip a requisition if it exists in the database 
* orderNumberConfiguration - set how order code should be generated. By default a order code has the following pattern ```O${requsition_id}```.
* requisitionTemplates - set requisition template for the given program. If the property is not set, there must be templates in database.
  * program: define program code or ```__ALL_PROGRAMS__``` if all programs should have the same template.
  * numberOfPeriodsToAverage - set number of periods to calculate value for average consumption in the requisition template
  * columns - define columns in the given template. Each column have to have name, label, source type, define if it displayed, column definition ID and display order.

#### configuration section
```
tool:
    configuration:
        accessFile: string
        password: string
        olmis:
            dialect: string
            showSql: boolean
            dataSource:
                connectionProperties:
                    string: string
                driverClass: string
                host: string
                port: integer
                database: string
                username: string
                password: string
        batch:
            chunk: integer
            skipPolicy: string
            migration: boolean
            skipPeriods: boolean
```
Before the tool can migrate data from supply manager into OpenLMIS system, it needs to know where SCM database file is located and where is a OpenLMIS database. This section also contains addictional Spring Batch settings.

* accessFile - define where is SCMgr database file. You can set absolute path like ```/home/user/data.mdb``` or relate path like ```../../data.mdb```.
* password - define password for SCMgr database file. Can be empty.
* olmis - provide details about OpenLMIS database
  * dialect - select what dialect should by used by Hibernate
  * showSql - define if SQL statements should be visible on output
  * dataSource - define settings of OpenLMIS database like address, port, name of database and so on.
* batch - this small configuration sections contains addictional Spring Batch settings
  * chunk - after how much requisitions the Spring Batch should execute the commit command.
  * skipPolicy - set the skip policy. By default if there will be any issue with migrating a single Product Tracking form the appropriate error message will be displayed but the tool will continue the work. The class must implement the ```org.springframework.batch.core.step.skip.SkipPolicy``` interface.
  * migration - set if the tool should migrate SCM forms into OpenLMIS.
  * skipPeriods - set whether the tool should insert skipped requisitions if there is a gap between periods. For example for the given facility and program there are requisitions for Jan 2012 and March 2012, the tool will create skipped requisition for Feb 2012 to avoid problems.
  
#### mapping section
```
tool:
    mapping:
        programs:
            - code: string
              categories:
                  - string
                  - string
                  - ...
              warehouses:
                  - geographicZone: string
                    code: string
        facilities:
            string: string
        products:
            string: string
        stockAdjustmentReasons:
            string: string
        categoryProductJoins:
            integer: integer
```
The following list presents available mapping:

* programs - define a program code and what categories are related with this program. Also what warehouse should be selected when a requisition is converted into an order. Addictional if there is a value in the ```geographicZone``` field, warehouse will be selected based by the geographic zone value. Otherwise the geographic zone of facility will be ignored.
* facilities - define a key-value pair for each SCM facility code to find a proper OpenLMIS facility.
* products - define a key-value pair for each SCM product name/code to find a proper OpenLMSI product by code.
* stockAdjustmentReasons - define a key-value pair for each SCM stock adjustment reason to find a proper OpenLMIS stock adjustment reason.
* categoryProductJoins - define a key-value pair of Category Product Join IDs. This mapping will be used only if the tool will not be able to find correct CPJ instance based on data from the Item table. If the CPJ exists, the tool will not use the related mapping.

#### exclude section
```
tool:
    exclude:
        forms:
            - facility: string
              period: string
              program: string
        products:
            - string
            - string
            - ...
```
The following list presents available excludes:

* forms - define for which facility, period and program requisition should not be created. It is required to define at least one field. If the given field is empty then it is equal to match all values for this field. For example if program field is empty then there will be no requisitions for the given facility in the given period for all available programs.
* products - define a list of product codes that should be excluded.
