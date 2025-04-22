# Tinyflow-java

Tinyflow-java is an intelligent agent orchestration solution developed using Java. It is not a product, but a development component.
By integrating Tinyflow-java, you can make any traditional Java Web application have the ability to orchestrate AI intelligent agents.

## Features

- Lightweight (supports Java 8 and above, no restrictions on development frameworks)

- Flexible (built-in rich node types, supports synchronous or asynchronous execution)

- High performance (based on Java development, faster performance than Nodejs and Python)

## Front-end

Tinyflow-java's front-end drag component, open source address: https://github.com/tinyflow-ai/tinyflow


## Quick Start

Introduce dependencies

```xml
<dependency>
    <groupId>dev.tinyflow</groupId>
    <artifactId>tinyflow-java-core</artifactId>
    <version>1.0.0-rc.9</version>
</dependency>
```

Initialize Tinyflow

```java
String flowDataJson = "Process data passed from the front end";
Tinyflow tinyflow = new Tinyflow(flowDataJson);

Map<String, Object> variables = new HashMap<>();
variables.put("name", "Michale");
variables.put("age", 18);

tinyflow.execute(variables);
```


## Wechat Group

![](./docs/assets/images/wechat_group.jpg)