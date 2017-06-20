import java.util.*;

import jade.core.*;
import jade.domain.*;
import jade.wrapper.*;
import jade.lang.acl.*;
import jade.core.behaviours.*;
import jade.domain.FIPAAgentManagement.*;

import org.json.JSONObject;

public class CarpoolAgent extends Agent {

    public static String LOGGING_SERVICE_NAME = "LoggingService";
    public static String PASSENGER_SERVICE_NAME = "PassengerService";
    public static String DRIVER_SERVICE_NAME = "DriverService";

    public static ServiceDescription LOGGING_SERVICE;
    public static ServiceDescription PASSENGER_SERVICE;
    public static ServiceDescription DRIVER_SERVICE;

    static {
        LOGGING_SERVICE = new ServiceDescription();
        LOGGING_SERVICE.setName(LOGGING_SERVICE_NAME);
        LOGGING_SERVICE.setType("carpooling-services");

        PASSENGER_SERVICE = new ServiceDescription();
        PASSENGER_SERVICE.setName(PASSENGER_SERVICE_NAME);
        PASSENGER_SERVICE.setType("carpooling-services");

        DRIVER_SERVICE = new ServiceDescription();
        DRIVER_SERVICE.setName(DRIVER_SERVICE_NAME);
        DRIVER_SERVICE.setType("carpooling-services");
    }

    private MapModel map;
    private CarpoolView view;

    private ArrayList<PassengerAgent> passengers;
    private ArrayList<DriverAgent> drivers;

    protected void setup() {
        try {
            map = MapModel.generate(20);

            passengers = generatePassengers(8, map);
            drivers = generateVehicles(4, map);

            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            dfd.addServices(LOGGING_SERVICE);
            DFService.register(this, dfd);

            for (DriverAgent vehicle : drivers) {
                registerVehicle(getContainerController(), vehicle);
            }

            for (PassengerAgent passenger : passengers) {
                registerPassenger(getContainerController(), passenger);
            }

//            view = new CarpoolView(map);
//            view.drawPassengers(passengers);

            addBehaviour(new Behaviour() {
                private Set<AID> driversReady = new HashSet<>();
                private Set<AID> passengersReady = new HashSet<>();

                @Override
                public void action() {
                    ACLMessage msg = getAgent().blockingReceive(
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                    );
                    AID sender = msg.getSender();
                    JSONObject content = new JSONObject(msg.getContent());
                    String senderType = content.getString("sender-type");
                    if (senderType.equals("passenger")) {
                        assert !passengersReady.contains(sender);
                        System.out.printf("%s ready to go!\n", sender.getLocalName());
                        passengersReady.add(sender);
                    } else if (senderType.equals("driver")) {
                        assert !driversReady.contains(sender);
                        System.out.printf("%s ready to go!\n", sender.getLocalName());
                        driversReady.add(sender);
                    }
                }

                @Override
                public boolean done() {
                    return driversReady.containsAll(drivers) && passengersReady.containsAll(passengers);
                }
            });
        } catch (Exception e) {
            System.out.println("Error: " + e);
            e.printStackTrace(System.out);
            System.exit(1); // ???
        }
    }

    private static void registerPassenger(ContainerController container, PassengerAgent agent)
            throws StaleProxyException, FIPAException {
        AgentController ac = container.acceptNewAgent(agent.toString(), agent);
        ac.start();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(agent.getAID());
        dfd.addServices(PASSENGER_SERVICE);
        DFService.register(agent, dfd);
    }

    private static void registerVehicle(ContainerController container, DriverAgent agent)
            throws StaleProxyException, FIPAException {
        AgentController ac = container.acceptNewAgent(agent.toString(), agent);
        ac.start();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(agent.getAID());
        dfd.addServices(DRIVER_SERVICE);
        DFService.register(agent, dfd);
    }

    private static ArrayList<PassengerAgent> generatePassengers(int n, MapModel map) {
        ArrayList<PassengerAgent> passengers = new ArrayList<>(n);
        ArrayList<MapModel.Node> nodes = new ArrayList<>(map.getGraph().vertexSet());
        Random rnd = new Random();
        for (int i = 0; i < n; ++i) {
            MapModel.Node from = nodes.get(rnd.nextInt(nodes.size()));
            MapModel.Node to = nodes.get(rnd.nextInt(nodes.size()));
            while (to == from) {
                to = nodes.get(rnd.nextInt(nodes.size()));
            }
            PassengerAgent passenger = new PassengerAgent(new MapModel.Intention(from, to));
            passengers.add(passenger);
        }
        return passengers;
    }

    private static ArrayList<DriverAgent> generateVehicles(int n, MapModel map) {
        ArrayList<DriverAgent> vehicles = new ArrayList<>(n);
        ArrayList<MapModel.Node> nodes = new ArrayList<>(map.getGraph().vertexSet());
        Random rnd = new Random();
        for (int i = 0; i < n; ++i) {
            MapModel.Node from = nodes.get(rnd.nextInt(nodes.size()));
            MapModel.Node to = nodes.get(rnd.nextInt(nodes.size()));
            while (to == from) {
                to = nodes.get(rnd.nextInt(nodes.size()));
            }
            DriverAgent vehicle = new DriverAgent(new MapModel.Intention(from, to), map);
            vehicles.add(vehicle);
        }
        return vehicles;
    }
}