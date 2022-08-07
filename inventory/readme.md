# Inventory Valuation

Inventory valuation is a service that calculate valuation of in inventory transaction. This valuation will be used in a book (a book in a term of accounting system) keeping application. For example, if you run a company that by some items and resale it with a profit margin, before you can sell it, you buy it first. When you receive your purchased item, inventory valution will calculate how much you can recognize in your book.

## Installation

see database settings in configuration file `application.properties`, typical database settings are:
```
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.url=jdbc:postgresql://localhost:5432/inventory
spring.datasource.username=postgres
spring.datasource.password=postgres
```

Make adjustment if required, for example DB credential. Create database if necessary

`createdb -U postgres -h localhost inventory`

For now, only postgresql database supported. Run sql script `schema.sql` to populate database schema including it's table.

`psql -U postgres -h localhost -d inventory < schema.sql`

Compile

`mvn clean compile package -DskipTests`

Since we compile with `package` target we will get packed jar in target folder, for example `inventory-0.0.1-SNAPSHOT.jar`

## Run

Run packed jar

`java -jar inventory-0.0.1-SNAPSHOT.jar`

you will see log similar to this when service up successfully:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v2.7.1)

2022-08-06 07:47:57.016  INFO 22628 --- [           main] c.i.i.InventoryServiceApplication        : Starting InventoryServiceApplication v0.0.1-SNAPSHOT using Java 18.0.1 on Barito-HP-Pav-2047 with PID 22628 (C:\Users\zaien\Project\infinite\customservice\inventory\target\inventory-0.0.1-SNAPSHOT.jar started by zaien in C:\Users\zaien\Project\infinite\customservice\inventory\target)
2022-08-06 07:47:57.018  INFO 22628 --- [           main] c.i.i.InventoryServiceApplication        : No active profile set, falling back to 1 default profile: "default"
2022-08-06 07:47:57.864  INFO 22628 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8080 (http)
2022-08-06 07:47:57.872  INFO 22628 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2022-08-06 07:47:57.873  INFO 22628 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.64]
2022-08-06 07:47:57.940  INFO 22628 --- [           main] o.a.c.c.C.[.[.[/api/inventory]           : Initializing Spring embedded WebApplicationContext
2022-08-06 07:47:57.940  INFO 22628 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 886 ms
2022-08-06 07:47:58.029  INFO 22628 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2022-08-06 07:47:58.167  INFO 22628 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2022-08-06 07:47:58.927  INFO 22628 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path '/api/inventory'
2022-08-06 07:47:58.936  INFO 22628 --- [           main] c.i.i.InventoryServiceApplication        : Started InventoryServiceApplication in 2.251 seconds (JVM running for 2.538)
```

## Usage

API documentation available via swagger, go to `http://localhost:8080/api/inventory/swagger-ui/index.html#/`

### Posting material transaction to be calculated

Material transaction is an entity that represent a transaction inventory. It may an incoming inventory or outgoing inventory. You can find all supported movement type in `MaterialTransaction` schema, refer to `movementType` field.

Inventory valuation use following assumption: 

1. you post material transaction in **chronological order**. When you post a backdated transaction, inventory valuation service will append it to the queue, it won't bahave as your expectation related to backdated transaction
1. `costingStatus` always set to `NotCalculated`. Put other costing status will have no effect, inventory valuation still handle it as `NotCalculated`.
1. All field related to date has no time zone information. For now, inventory valuation service **does not support time zone** yet.

To post material transaction you can use `/valuation` end point

Inventory valuation use asynchronous approach to process submitted material transaction. Meaning when you post a material transaction, you will receive a message like `inventory transaction received, lenght: 25`, you will not receive calculation result immediately.

### Querying for inventory valuation

Once material transaction(s) submitted, inventory valuation will immediately do the calculation, and store it's result to DB for further query. To query calculated inventory valuation you can use `/materialtransaction` end point. Please revisite swagger ui for complete API documentation. 

### Validation rules for submitted material transaction

Some scenario make it impossible to calculate inventory valuation. Hence inventory valuation perform some validation rule to prevent further error. Those validation rule are:

1. negative quantity on hand.
1. negative movement quantity.
1. movement in that can not find matched movement out transaction.
1. physical inventory in that has no cost available nor current cost available.
1. physical inventory out that has no current cost available.
1. movement quantity is zero or negative. only positive quantity allowed. inventory valuation use movement type to identify incoming/outgoing quantity, not using negative/positive.
1. inadequate quantity for outgoing transaction. for example, if quantity on hand is 10, but customer shipment need 10, it will set costing status as `Error` with error message `Insufficient quantity`.
1. acquisition cost is zero for: 
    1. vendory receipt transaction.
    1. 1<sup>st</sup> transaction with movement type physical inventory in.

### Querying problematic material transaction

If inventory valuation detect a problematic material transaction, inventory valuation will flag this record as an error, field `costingStatus` set to `Error`, `costingErrorMessage` will contain what cause of this error, and `error` set to true. This error record will prevent inventory valuation to calculate next material transaction, since a material transaction calculation depend on successfull previous transaction.

Consider following scenario to handle problematic material transaction

we have 5 records of material transactions:

1. physical inventory in 100 pcs
1. customer shipment 10 pcs
1. customer shipment 10 pcs
1. customer shipment 10 pcs
1. customer shipment 10 pcs

We will end up with 60 pcs quantity on hand. However, if 1<sup>st</sup> transaction has no cost (`acquisitionCost` is 0.00), then inventory valuation can not determine cost of this transaction. Hence, inventory valuation will prevent to calculate next transaction (2<sup>nd</sup> transaction and beyond).

We will end up with 1<sup>st</sup> transaction is `Error`, and all next transaction still on `NotCalculated`.

To resolve that scenario, we have 2 options:

#### A. update problematic material transaction

We can update problematic material transaction using `/materialtransaction` with `PUT` method. For case above, we can update 1<sup>st</sup> transaction with:

1. `acquisitionCost` not zero
1. `costingStatus` set to `NotCalculated`
1. `costingErrorMessage` set to NULL
1. `error` set to false

Once submitted, inventory valuation will calculate 1<sup>st</sup> transaction, and this time it success, and inventory valuation wll continue to next transaction.

#### B. add a new material transaction to the top of the chain

We can add a new transaction to the top of the chain using `/valuation/addtop` end point. For case above, we can add new transaction below:

```
[
    {
        "correlationId": "E943527137A24A9E8F9209C058DF2A1D",
        "product": {
            "correlationId": "D331AACC8E5F425A9129F530002EA669",
            "valuationType": "MovingAverage"
        },
        "movementType": "VendorReceipt",
        "movementQuantity": 50,
        "acquisitionCost": 50000,
        "movementDate": "2021-08-23T11:08:00",
        "costingStatus": "NotCalculated"
    }
]
```

Those new transaction will add a vendor receipt, which increase quantity on hand and also provide a current cost, hence the problematic transaction will not problematic anymore since current cost now available.