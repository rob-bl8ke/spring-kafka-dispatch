
# Run the application

Can use `mvn spring-boot:run` to watch the console for messages, but only once you've created an `order.created` topic.

Better to debug it so you can see how the handler gets hit. The handler is seperated from the service so as to improve the ability to test.

Create two terminals, one to `docker-compose up -d` the Kafka instance and the other to send messages.

```bash
kafka-console-producer --topic order.created --bootstrap-server localhost:9092
```

Run the tests and see how the separation of the service from the handler makes testing easier with the help of Mockito.

# Kafka (in a container)

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