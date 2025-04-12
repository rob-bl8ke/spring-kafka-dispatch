
# Run the application

Can use `mvn spring-boot:run` to watch the console for messages, but only once an `order.created` topic is created. See Kafka commands below. Might be better to debug it so one can see how the handler gets hit.

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

### Spring Boot Code

- The Kafka consumer handler is seperated from the service so as to improve the ability to test and to follow the best practice of separating concerns.
- Run the tests and see how the separation of the service from the handler makes testing easier with the help of Mockito.

# References

- [Getting started with Apache Kafka](https://www.youtube.com/playlist?list=PLa7VYi0yPIH0xeDp2Iu1q_esSYeNsIxkZ) dives into the fundamentals of Apache Kafka and is an official seires of YouTube videos compiled by Confluent.
- Useful Udemy Course - [Introduction to Kafka with Spring Boot](https://www.udemy.com/course/introduction-to-kafka-with-spring-boot/?couponCode=25BBPMXPLOYCTRL) with John Thompson et al.
- [Confluent Developer](https://developer.confluent.io/) - Learn about the fundamentals of event streaming with Kafka and Flink, and the surrounding ecosystem.
- [Your private Notion notes](https://www.notion.so/Work-192bf9f5949880f09a51cac75b0fccbf?p=196bf9f5949880c5bbe2dadd7ce12d30&pm=s)