# Key-Value store
A simple key-value store written in Scala

## Requirements
To build or run this project, you must first have the following things installed

- Java 11
- Scala 2.12.12
- Sbt 1.4.9

## Running
After the above dependencies are satisfied, the project can be built with
```shell
sbt clean compile
```

The project can then be run with
```shell
sbt run
```

After seeing `Startup complete, server awaiting.` in the logs, the server is ready to accept connections on port `:4000`
