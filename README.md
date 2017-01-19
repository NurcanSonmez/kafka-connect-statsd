## Introduction 

This Kafka Connect connector listens for incoming StatsD data and yields Kakfa Connect records.

## Configuration

### Source Connector

| Name  | Description                     | Type   | Default | Valid Values | Importance |
|-------|---------------------------------|--------|---------|--------------|------------|
| topic | The topic to write the data to. | string |         |              | high       |
| port  | The port to listen on.          | int    | 8125    |              | high       |


```properties
name=statsd
tasks.max=1
connector.class=com.github.jcustenborder.kafka.connect.statsd.StatsDSourceConnector
topic=statsd
```


## Example Data

Below is a sample of data that was received.

```json
{
  "sender": {
    "string": "localhost"
  },
  "recipient": {
    "string": "0:0:0:0:0:0:0:0"
  },
  "name": "com.github.jcustenborder.kafka.connect.statsd.StatsDListeningServiceTest.test",
  "type": "TIMER",
  "value": 1282,
  "sampleRate": null,
  "timestamp": 1484784520284
}
```
