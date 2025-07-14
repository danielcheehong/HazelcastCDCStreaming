# Hazelcast CDC Data Replication

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](#license)

## Table of Contents

* [Project Overview](#project-overview)
* [Features](#features)
* [Architecture](#architecture)
* [Getting Started](#getting-started)

  * [Prerequisites](#prerequisites)
  * [Installation](#installation)
* [Configuration](#configuration)
* [Usage](#usage)

  * [Starting Hazelcast Cluster](#starting-hazelcast-cluster)
  * [CDC Source Setup](#cdc-source-setup)
  * [Running the Application](#running-the-application)
* [Examples](#examples)
* [Testing](#testing)
* [Contributing](#contributing)
* [License](#license)
* [Contact](#contact)

## Project Overview

A Proof-of-Concept (POC) for data replication using Hazelcast IMDG and Change Data Capture (CDC). This project demonstrates how to capture changes from relational databases and propagate them in real time to a Hazelcast cluster.

## Features

* Real-time data capture from MySQL/PostgreSQL/SQL Server using Debezium or custom CDC
* In-memory replication to Hazelcast clusters
* Support for reliable topics with ringbuffer-backed replay
* Easy-to-configure deployment via Docker

## Architecture

```plaintext
┌────────────┐     ┌─────────────┐     ┌─────────────────┐
│  Database  │ --> │  CDC Agent   │ --> │ Hazelcast IMDG  │
└────────────┘     └─────────────┘     └─────────────────┘
```

1. **Database**: Source DB with CDC enabled
2. **CDC Agent**: Debezium connector or custom reader that publishes events
3. **Hazelcast IMDG**: Receives events on topics/queues for consumption by downstream services

## Getting Started

### Prerequisites

* Java 11+ or compatible JDK
* Docker & Docker Compose
* Maven or Gradle

### Installation

1. Clone the repo:

   ```bash
   git clone https://github.com/your-username/hazelcast-cdc-replication.git
   cd hazelcast-cdc-replication
   ```
2. Build the project:

   ```bash
   mvn clean package
   # or gradle build
   ```

## Configuration

Configuration files live in `src/main/resources/config/`:

* `hazelcast.yaml` — Hazelcast cluster settings
* `cdc-config.json` — Debezium or custom CDC connector settings
* `application.properties` — App-specific properties (DB connection, topic names)

## Usage

### Starting Hazelcast Cluster

```bash
docker-compose up -d hazelcast-node1 hazelcast-node2
```

### CDC Source Setup

1. Enable CDC on your database:

   ```sql
   -- MySQL example
   ALTER TABLE your_table
   ENABLE ROW_LEVEL_CHANGE_TRACKING;
   ```
2. Configure and start Debezium connector:

   ```bash
   curl -X POST -H "Content-Type: application/json" \
     --data @config/cdc-config.json \
     http://localhost:8083/connectors
   ```

### Running the Application

```bash
java -jar target/hazelcast-cdc-replication.jar --spring.config.location=config/
```

## Examples

Show sample usage or API calls if applicable. For example, publishing a test message:

```java
ITopic<String> topic = hazelcastInstance.getTopic("db-changes");
topic.publish("{ \"id\": 1, \"operation\": \"INSERT\" }");
```

## Testing

* Unit tests: `mvn test`
* Integration tests: ensure Docker services are running, then `mvn verify`

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/foo`)
3. Commit your changes (`git commit -m "feat: add foo feature"`)
4. Push to the branch (`git push origin feature/foo`)
5. Open a Pull Request

Please follow the [Contributor Covenant](https://www.contributor-covenant.org/) code of conduct.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

## Contact

* **Author**: Daniel Lo (danielcheehonglo@gmail.com)
* **Repo**: [[https://github.com/your-username/hazelcast-cdc-replication](https://github.com/your-username/hazelcast-cdc-replication)](https://github.com/danielcheehong/HazelcastCDCStreaming)
