
# Run the application

Launch the application with `mvn spring-boot:run`.

To debug in Visual Studio Code, one can use this configuration in one's `launch.json`.

```json
{
    "configurations": [
        {
            "type": "java",
            "name": "Dispatch",
            "request": "launch",
            "mainClass": "com.cyg.dispatch.DispatchApplication",
            "projectName": "dispatch"
        },
        {
            "type": "java",
            "name": "Dispatch (Debug)",
            "request": "launch",
            "cwd": "${workspaceFolder}",
            "mainClass": "com.cyg.dispatch.DispatchApplication",
            "projectName": "dispatch",
            "args": "",
            // "env": {
            //     "SPRING_PROFILES_ACTIVE": "dev"
            // }
        }
    ]
}
```

Create two terminals, one to `docker-compose up -d` the Kafka instance and the other to send messages.

```bash
kafka-console-producer --topic order.created --bootstrap-server localhost:9092
```

## Kafka (in a container)

The [approach used by this page](https://dev.to/deeshath/apache-kafka-kraft-mode-setup-5nj) for the `docker-compose.yml` file does not work and Kafka will exit 1. Issue is discussed on [this Stack Exchange discussion thread](https://stackoverflow.com/questions/79392483/kafka-integration-with-wireguard-nonroutable-meta-address-0-0-0-0) and the fix/workaround is that this:

```
KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
```

must change to:
```
KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
```

Use `docker compose up -d` to get Kafka up and running. Once up and running, use `docker exec -it kafka bash` to get to the container terminal and reach Kafka.

Now, one can start running the Kafka commands.

### Topic Management

Here are some useful commands for topic management.

```bash
# Navigate to Kafka
cd /usr/bin/
ls

# Start the server 
kafka-server-start config/kraft/server.properties

# Create a topic and describe it (partitions, replication, leader information etc.)
kafka-topics --bootstrap-server localhost:9092 --create --topic my.new.topic
kafka-topics --bootstrap-server localhost:9092 --describe

# Set the partitions (a topic name cannot be changed)
kafka-topics --bootstrap-server localhost:9092 --alter --topic my.new.topic --partitions 3

# Delete the topic
kafka-topics --bootstrap-server localhost:9092 --delete --topic my.new.topic
kafka-topics --bootstrap-server localhost:9092 --describe --topic my.new.topic
kafka-topics --bootstrap-server localhost:9092 --list
```

### Consumer Group Management

Here are some useful commands for consumer group management

```bash
# Create a topic with 5 partitions
kafka-topics --bootstrap-server localhost:9092 --create --topic cg.demo.topic --partitions 5

# Find out some info
kafka-topics --bootstrap-server localhost:9092 --describe --topic cg.demo.topic

# In two other terminal windows repeat the following commands to create two new consumers on the same topic.
kafka-topics --bootstrap-server localhost:9092 --describe --topic cg.demo.topic

# Notice how the CONSUMER-ID shows how the rebalancing of consumers per partition has taken place.
kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group my.new.group
kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Check the state of your consumer group (State, Members, assignment strategy etc.)
#Check the health of your consumer group
kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group my.new.group --state

# Find out which consumers are in a group
kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group my.new.group --members
```

# Design Considerations

### Docker Compose File

This [original](https://dev.to/deeshath/apache-kafka-kraft-mode-setup-5nj) `KAFKA_LISTENERS` line has been modified.
```
KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
```

It's been modified to this based on a StackExchange [thread discussion](https://stackoverflow.com/questions/79392483/kafka-integration-with-wireguard-nonroutable-meta-address-0-0-0-0) that addresses the problems experienced trying to initially run the file:

```
KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
```
This simply appears to be because Kafka cannot use 0.0.0.0 as it is a non-routable meta-address.

The `volumes` can be swapped between permanent and transient storage.

```yaml
# Store Kafka logs on your local machine (persisting them)
- ./data:/var/lib/kafka/data
```

```yaml
# or, store temporary logs in a Docker volume.
- /var/lib/kafka/data
```

On Windows OS, Docker Desktop uses a Linux-based virtual machine (VM) to run containers. When you specify volume mounts in your `docker-compose.yaml` file, Docker maps the paths between the Windows host and the Linux VM. this data folder refers to a relative path on your Windows host. It resolves to a data folder in the same directory as your `docker-compose.yaml` file.

On your MacBook, you will probably run into file permission issues and to solve these its best to go with temporary logs. The other option is to remap your data folder to a folder that the `docker-compose.yaml` file can run in. A folder you create yourself:

```bash
mkdir -p ./data
chmod -R 777 ./data
```
Unfortunately, the mount requires an absolute path and if you're not comfortable with this you're left with the first option:

```yaml
 - /Users/your-username/path-to-project/data:/var/lib/kafka/data
```

If the above steps don't work, you can try running docker-compose with elevated privileges (though this is not recommended for security reasons):

```bash
sudo docker-compose up
```

### The consumer handler

- The Kafka consumer handler is seperated from the service so as to improve the ability to test and to follow the best practice of separating concerns.
- Run the tests and see how the separation of the service from the handler makes testing easier with the help of Mockito.

### [Avoiding the Poison Pill](https://www.udemy.com/course/introduction-to-kafka-with-spring-boot/learn/lecture/38115908#notes)

If an exception is thrown while deserializing a message, bad things will happen because continuous polling will keep throwing the exception.

So this message will be fine...

```json
{"orderId":"0e9631fd-fab4-41a6-8b1d-63e4b2602658","item":"item-1"}
```

With this message (as an example), all hell breaks loose...

```json
{"orderId":"0e9631fd-fab4-41a6-8b1d-63e4b2602658"}
```

To be clear: the message is polled again and again resulting in the same deserialization exception, effectively blocking the topic  and preventing other messages from being processed.

However, by using the `org.springframework.kafka.support.serializer.ErrorHandlingDeserializer` the problem is resolved/

This configuration ensures that deserialization errors are handled gracefully by the `ErrorHandlingDeserializer`, preventing poison pills from causing continuous polling and failure. Instead, the problematic message is skipped or handled according to your error-handling strategy, allowing the consumer to continue processing other messages.

This deserializer wraps the actual deserializer (in this case, JsonDeserializer) and handles deserialization errors gracefully. If a message cannot be deserialized (e.g., due to invalid JSON or mismatched types), the ErrorHandlingDeserializer catches the exception and returns a null value or an error record instead of throwing an exception.

When a deserialization error occurs, the `ErrorHandlingDeserializer` prevents the exception from propagating to the Kafka consumer. Spring Kafka's error handling mechanisms (e.g., `SeekToCurrentErrorHandler`) can then skip the problematic message and move on to the next one, avoiding an infinite loop of polling and failure.

You can configure Spring Kafka to log the error, send the problematic message to a dead-letter topic, or take other actions, ensuring the poison pill does not disrupt normal processing.

### [Configuring Deserialization in Spring Beans](https://www.udemy.com/course/introduction-to-kafka-with-spring-boot/learn/lecture/38164902#notes)

The `DispatchConfiguration` class configures the Kafka consumer for the application.

-  A consumer factory defines the strategy to create the consumer instance. It uses `JsonDeserializer` to deserialize messages into the `OrderCreated` class


- The Listener Container Factory provides a `ConcurrentKafkaListenerContainerFactory` to manage Kafka listeners with the configured consumer factory.

This class centralizes Kafka consumer configuration, ensuring error resilience and type-safe message processing. And this has the advantage of a being able to more easily define multiple beans for different scenarios, eg. different listeners may require different timeouts periods.

It gives the application compile time confirmation that the classes are correctly defined and on the application class path.




# References

- [Getting started with Apache Kafka](https://www.youtube.com/playlist?list=PLa7VYi0yPIH0xeDp2Iu1q_esSYeNsIxkZ) dives into the fundamentals of Apache Kafka and is an official seires of YouTube videos compiled by Confluent.
- Useful Udemy Course - [Introduction to Kafka with Spring Boot](https://www.udemy.com/course/introduction-to-kafka-with-spring-boot/?couponCode=25BBPMXPLOYCTRL) with John Thompson et al.
- [Confluent Developer](https://developer.confluent.io/) - Learn about the fundamentals of event streaming with Kafka and Flink, and the surrounding ecosystem.
- [Download a local copy of Apache Kafka](https://kafka.apache.org/downloads). Later versions of Kafka come with Docker options.
- [Install Kafka on wsl2](https://www.udemy.com/course/introduction-to-kafka-with-spring-boot/learn/lecture/41291818#notes). Requires Udemy course
- [Your private Notion notes](https://www.notion.so/Work-192bf9f5949880f09a51cac75b0fccbf?p=196bf9f5949880c5bbe2dadd7ce12d30&pm=s)