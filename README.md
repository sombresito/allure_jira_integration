Allure Portal (Allure Report Server)
=================================
![Build / Test / Check](https://github.com/kochetkov-ma/allure-server/workflows/Build%20/%20Test%20/%20Check/badge.svg?branch=master)

[![jdk11](https://camo.githubusercontent.com/f3886a668d85acf93f6fec0beadcbb40a5446014/68747470733a2f2f696d672e736869656c64732e696f2f62616467652f6a646b2d31312d7265642e737667)](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
[![gradle](https://camo.githubusercontent.com/f7b6b0146f2ee4c36d3da9fa18d709301d91f811/68747470733a2f2f696d672e736869656c64732e696f2f62616467652f746f6f6c2d677261646c652d626c75652e737667)](https://gradle.org/)
[![junit](https://camo.githubusercontent.com/d2ba89c41121d7c6223c1ad926380235cf95ef82/68747470733a2f2f696d672e736869656c64732e696f2f62616467652f6a756e69742d706c6174666f726d2d627269676874677265656e2e737667)](https://github.com/junit-team/junit4/blob/master/doc/ReleaseNotes4.13.md)

[![checkstyle](https://img.shields.io/badge/checkstyle-google-blue)](https://github.com/checkstyle/checkstyle)
[![pmd](https://img.shields.io/badge/pmd-passed-green)](https://github.com/pmd/pmd)
[![spotbugs](https://img.shields.io/badge/spotbugs-passed-green)](https://github.com/spotbugs/spotbugs)

## About

Allure server for store / aggregate / manage Allure results and generate / manage Allure Reports.

There is simple API with Swagger(OpenAPI) Description.

Just use Spring Boot Jar from Release Page.


Only allure2 supported  
Make some allure results and create `zip` archive with these results, for example `allure-results.zip` in your root dir

```shell
curl -X POST 'http://localhost:8080/api/result' \
-H  "accept: */*" \
-H  "Content-Type: multipart/form-data" \
-F "allureResults=@allure-results.zip;type=application/x-zip-compressed"
```

Response:

```
{
    "fileName": "allure-results.zip",
    "uuid": "1037f8be-68fb-4756-98b6-779637aa4670"
}
```

Save `uuid`  
Don't forget specify form item Content type as `application/zip`. Server works with `zip` archives only!

### Generate report

For generate new report execute `POST` request with `json` body:

```shell
curl --location --request POST 'http://localhost:8080/api/report' \
--header 'Content-Type: application/json' \
--data-raw '{
  "reportSpec": {
    "path": [
      "master",
      "666"
    ],
    "executorInfo": {
      "buildName": "#666"
    }
  },
  "results": [
    "1037f8be-68fb-4756-98b6-779637aa4670"
  ],
  "deleteResults": false
}'
```

Response:

```
{
    "uuid": "c994654d-6d6a-433c-b8e3-90c77d0e8163"
    "path": "master/666",
    "url": "http://localhost:8080/allure/reports/c994654d-6d6a-433c-b8e3-90c77d0e8163/",
    "latest": "http://localhost:8080/reports/master/666",
}
```

Memorize `url`

> :warning: **Generated Reports, and their History are grouping by `path` key. This key means something like `project` or `job` or `branch`. The latest report with the same `path` will be active**: It is not a real path - it's a logical path. The same situation with `path` column in GUI!

### Access to generated reports

After generating you can access the latest report by `http://localhost:8080/allure/reports/master/666/index.html`

You may get all reports

```shell
curl --location --request GET 'http://localhost:8080/api/report'
```

Or by path as branch name `master`

```shell
curl --location --request GET 'http://localhost:8080/api/report?path=master'
```

You may get all uploaded results:

```shell
curl --location --request GET 'http://localhost:8080/api/result'
```

You can clear all results or reports:

```shell
curl --location --request DELETE 'http://localhost:8080/api/result'
curl --location --request DELETE 'http://localhost:8080/api/report'
```

Or clear reports older than date (in epoch seconds):

```shell
curl --location --request DELETE 'http://localhost:8080/api/report?seconds=1604693740'
```

### Cleanup features (since 1.10.0)

Once per day the scheduler started and remove old reports with age better then `allure.clean.ageDays`.

Besides, if specified `allure.clean.paths` items with fields `path` and `ageDays`
all reports with path = `allure.clean.paths[].path` will be removed based on separate max age
from `allure.clean.paths[].ageDays`

**_Example:_**

```yaml
allure:
  clean:
    dryRun: false
    time: "00:00"
    ageDays: 90
    paths:
      - path: "manual_uploaded"
        ageDays: 30
      - path: "service/production-job"
        ageDays: 10
```

- Report with path=`test` and age=`100d` will be removed at today MIDNIGHT
- Report with path=`test` and age=`99d` will **NOT** be removed at today MIDNIGHT
- Report with path=`manual_uploaded` and age=`30d` will be removed at today MIDNIGHT
- Report with path=`manual_uploaded` and age=`29d` will **NOT** be removed at today MIDNIGHT
- Report with path=`service/production-job` and age=`10d` will be removed at today MIDNIGHT
- Report with path=`service/production-job` and age=`9d` will **NOT** be removed at today MIDNIGHT

### Special options



- system vars (JVM option) `-Dallure.support.old.format=true`
- environment vars `export allure.support.old.format=true`
- in docker environment vars `-e allure.support.old.format=true`

**ENV**                      | **TYPE**                 | **DEFAULT**              | **DESCRIPTION**
------------------------------|--------------------------|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------
spring.datasource.url        | string                   | jdbc:h2:file:./allure/db | H2 jdbc connection string. By default DB file will be created/read on startup. Postgres driver supported!
PORT                         | int                      | 8080                     | Tomcat http port
allure.resultsDir            | string                   | allure/results/          | Unzipped results store
allure.reports.dir           | string                   | allure/reports/          | Generated results store
allure.reports.path          | string                   | reports/                 | Url path (after base url) to acccess to reports
allure.reports.history-level | int                      | 20                       | Number of reports in history
allure.support-old-format    | boolean                  | false                    | Auto-convert old format reports to new and add to db
JAVA_OPTS                    | string                   | -Xms256m -Xmx2048m       | Java memory options for container
allure.date-format           | string                   | yy/MM/dd HH:mm:ss        | Date Time format in grid
allure.server-base-url       | string                   |                          | Define custom base url for results. If your server behind the proxy or other troubles to get server external hostname. Don't forget about '/' at the end
basic.auth.enable            | boolean                  | false                    | Enable Basic Authentication
basic.auth.username          | string                   | admin                    | Username for basic auth
basic.auth.password          | string                   | admin                    | Password for basic auth
jira.apiUrl                  | string                   | https://jira.bcc.kz
   | Base URL of Jira REST API
jira.apiToken                | string                   |
   | Bearer token for Jira API
allure.clean.dryRun          | boolean                  | false                    | Don't delete but print logs. For testing
allure.clean.time            | LocalTime "HH[:mm][:ss]" | 00:00                    | Time to check reports age/ Scheduler start once per day
allure.clean.ageDays         | int                      | 90                       | Max age for all reports. But exclude specified paths in 'allure.clean.paths'
allure.clean.paths[].path    | String                   | manual_uploaded          | Report path
allure.clean.paths[].ageDays | int                      | 30                       | Max age for reports with this path

### Jira integration
Allure server automatically posts a comment to Jira for every issue link found in test results. Configure `jira.apiUrl` and `jira.apiToken` to enable this.
> Every spring boot setting can be passed through ENV variables with a little changes according to [spring boot cfg docs](https://docs.spring.io/spring-boot/docs/1.5.5.RELEASE/reference/html/boot-features-external-config.html)
> For example: `allure.report.host` transform to `ALLURE_REPORT_HOST`

> Postgres database supported!

> You can mount external jars to `/ext` folder in the container, and they will be available in app classpath.  
> For example you may add new jdbc drivers

```shell
    volumes:
      - ./ext:/ext:rw
```

#
Example:

```
    - name: Send Results and Generate Allure Report
      uses: Xotabu4/send-to-allure-server-action@1
      # always() needed because we want report for failed tests as well
      if: ${{ always() }}
      with:
        allure-server-url: 'http://my-allure-server.com:5001/'
```

![alt text](github-action.png)

### GUI

##### See example on [allure.iopump.ru](http://allure.iopump.ru/)

Allure Server provide WEB UI to access to reports and results.  
By default WEB UI is available on path `/ui` and there is redirection from `/` to `/ui`   
Example: `http://localhost:8080/ui`  
WEB UI provides the same functions as a REST API  
WEB UI is implemented with [Vaadin 14](https://vaadin.com/start/v14)

> :warning: **Generated Reports, and their History are grouping by `path` key. This key means something like `project` or `job` or `branch`. The latest report with the same `path` will be active**: It is not a real path - it's a logical path. The same situation with `path` column in GUI!

> *Main Page*
![alt text](ui-example.png)  

### Logging

Logging properties are located in `[application.yaml](src%2Fmain%2Fresources%2Fapplication.yaml)`

```
logging:
  level:
    root: INFO
    org.atmosphere: WARN # Vaadin (GUI) Server
    org.springframework: INFO
    org.springframework.core: WARN
    org.springframework.beans.factory.support: WARN
    ru.iopump.qa:allure: INFO # Allure Server Logs
```

You may override it by Environment Variables, for example enable `DEBUG` for allure server:
```
    export LOGGING_LEVEL_RU_IOPUMP_QA_ALLURE=DEBUG
```

Or switch all logs to `DEBUG`:
```
    export LOGGING_LEVEL_ROOT=DEBUG
```

