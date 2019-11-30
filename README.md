# LRA Axon connector

This is proof of concept application for connection the LRA coordinator with an Axon service. It offers a way how
Axon based applications can be enlisted in the active LRA as participants.

[LRA Axon connector quickstart](https://github.com/mkralik3/axon-microservices-example)

## Guide how to use LRA Axon connector

This guide describes how to use the LRA Axon connector in Axon based services in order to enlist an aggregate as a participant to the LRA context.

### How to enable LRA Axon connector

Maven dependency:
```
<dependency>
    <groupId>org.mkralik.learning.lra.axon</groupId>
    <artifactId>lra-axon-interceptors</artifactId>
    <version>1.0</version>
</dependency>
```

How to enable the connector:
```
@SpringBootApplication
@EnableLraAxonConnectorModule
public class CarApplication {
    public static void main(String[] args) {
        SpringApplication.run(CarApplication.class, args);
    }
}
```

After that, the interceptors are imported to the application context and intercept all commands and events. 
Also, the REST endpoints for the LRA coordinator are exposed on path `<host>:<port>/axonLra`. 
Through these endpoints, the LRA coordinator contacts the participants, so it is forbidden to use these endpoints.
The connector uses Spring MVC. These situations can happen:
 
* If the application doesn't contain any REST endpoints, the connector endpoints are exposed automatically but 
the application has to specify the _`server.port`_ property in the _`application.yml`_.

* If the application contains any other endpoints based on Spring MVC, the connector endpoints are exposed automatically on the same port.
 It needs to be guaranteed that endpoint `<host>:<port>/axonLra` is not occupied by any other REST endpoints.

* If the application contains any other endpoints based on JAX-RS (e.g. Jersey implementation), the connector endpoints are not exposed automatically. 
Since it is not possible to use \mbox{JAX-RS} and Spring MVC endpoints in an application at once, the connector contains the __AxonLraEndpointsJaxRS__ component 
which is prepared for the application which uses JAX-RS. It needs to add it explicitly to the Jersey configuration:
```
@Component
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(WadlResource.class);
        register(AxonLraEndpointsJaxRS.class);
    }
}
```
Also, it needs to be guaranteed that endpoint `/axonLra` is not occupied by any other REST endpoints.



By default, the connector expected the LRA coordinator on `localhost:8080`. 
To specify the LRA coordinator host and port use the system properties `lra.http.host` and `lra.http.port`.

### How to define participant

The aggregate which wants to be a participant in the LRA needs to have defined the participant functions. 
According to the specification, only compensate function is mandatory. 
The participant function is in the Axon based application specified by the command handler which handles the particular 
command from the org.mkralik.learning.lra.axon.api.command package.
So when the aggregate needs to have compensated and complete function, it will contain the command handlers which handle LRACompleteCommand and LRACompensateCommand.

When the LRA coordinator contacts the LRA Axon connector endpoints to compensate a particular participant, 
the compensate endpoint fires \textit{LRACompensateCommand} command with the particular aggregate identifier. 
Thanks to that, the command is routed to the right aggregate where is handled by the defined command handler and the compensation action can be performed.

The requirement for a user when defining a participant function in aggregate is following the MicroProfile LRA specification requirement on [return types](https://github.com/eclipse/microprofile-lra/blob/master/spec/src/main/asciidoc/microprofile-lra-spec.adoc#non-jaxrs-participant-methods).

__In case the type of return value is not valid, the application fails after the start!__

Example of participant: (For more information see quickstart)

```
@Aggregate
public class Car {

    @AggregateIdentifier
    private String id;
    private String name;
    private Booking.BookingStatus status;

    @CommandHandler
    public Car(CreateCarCmd cmd) {
        apply(new CreatedCarEvent(cmd.getId(), cmd.getName()));
    }

    @CommandHandler
    public ParticipantStatus handle(LRACompensateCommand cmd) {
        apply(new ChangedCarStateEvent(cmd.getId(), Booking.BookingStatus.CANCELLED));
        return ParticipantStatus.Compensated;
    }

    @CommandHandler
    public ParticipantStatus handle(LRACompleteCommand cmd) {
        apply(new ChangedCarStateEvent(cmd.getId(), Booking.BookingStatus.CONFIRMED));
        return ParticipantStatus.Completed;
    }

    @EventSourcingHandler
    public void on(CreatedCarEvent evt) {
        id = evt.getId();
        name = evt.getName();
        status = evt.getStatus();
    }

    @EventSourcingHandler
    public void on(ChangedCarStateEvent evt) {
        status = evt.getStatus();
    }
}
```

### How to join in the LRA context

The aggregate is enlisted to the LRA by the command which is annotated by _@JoinLRA_ annotation. The command has to contain the LRA context.
Another service usually sends the command. When that service uses Narayana LRA implementation and LRA Axon connector, the LRA context is set implicitly to the command.
These situations can happen:

* When the command is applied to the command bus from the method where exist active LRA context (\textit{Current.peek()}), the context is implicitly added to the command metadata and the aggregate is enlisted to that LRA context.
* When the command is applied to the command bus from the method where it is not active LRA context, the user has to specify the LRA context manually in the command's field, which is annotated by _@LRAContext_.
* When the command is applied to the command bus from the method where exist active LRA context, but the user specifies the LRA context manually, the user-specified LRA context is used.
* In case the method doesn't contain any LRA context and the user doesn't specify it, the application failed.
 
 Command example (For more information see quickstart)
 ```
 @JoinLRA
 public class CreateCarCmd {
 
     @TargetAggregateIdentifier
     private String id;
 //    in case the context is not available
 //    @LRAContext
 //    private URI context;
     private String name;
     ...
 }
 ```
Method which can send a command (For more information see quickstart)
```
@POST
@LRA(value = LRA.Type.REQUIRED, end = false)
public Booking bookRoom(){
    ...
    cmdGateway.sendAndWait(new CreateCarCmd(carID, "Car_name"));
    ...
}
```
