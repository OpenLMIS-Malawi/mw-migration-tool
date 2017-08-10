
# SCMgr Migration Tool
This repository holds the files for the SCMgr Migration tool. The tool helps to convert data from SCMgr application to OpenLMIS 3.0 system.

## Prerequisites
* Java 8
* Maven 3

## Quick start
1. Fork/clone this repository from GitHub.

```shell
git clone https://github.com/OpenLMIS-Malawi/mw-migration-tool.git
```
2. Build the application by using maven. After the build steps finish, you should see 'BUILD SUCCESS'.

```shell
mvn clean install
```
3. Execute the generated jar file from the target directory.

```shell
java -jar target/scm-migration-tool.jar
```
