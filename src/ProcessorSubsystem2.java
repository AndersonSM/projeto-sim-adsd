import eduni.simjava.*;
import eduni.simjava.distributions.*;

class Client extends Sim_entity {
    private Sim_port out;
    private Sim_port in;
    private Sim_negexp_obj delay;

    Client(String name, double mean) {
        super(name);
        in = new Sim_port("In");
        out = new Sim_port("Out");
        add_port(out);
        add_port(in);
        delay = new Sim_negexp_obj("Delay", mean);
        add_generator(delay);
    }

    public void body() {
        while (Sim_system.running()) {
            sim_trace(1, "Client sending request.");
            sim_schedule(out, 0.0, 0);
            sim_pause(delay.sample());
            Sim_event e = new Sim_event();
            sim_get_next(e);
            sim_completed(e);
            sim_trace(1, "Client received response.");
        }
    }
}

class CoreServer extends Sim_entity {
    private Sim_port in, apiServerIn, database, apiServerOut, client;
    private Sim_normal_obj delay;
    private Sim_random_obj prob;
    private Sim_stat stat;

    CoreServer(String name, double mean, double var) {
        super(name);
        in = new Sim_port("In");
        database = new Sim_port("Database");
        apiServerOut = new Sim_port("ApiServerOut");
        client = new Sim_port("Client");
        apiServerIn = new Sim_port("ApiServerIn");
        add_port(in);
        add_port(database);
        add_port(apiServerOut);
        add_port(apiServerIn);
        add_port(client);
        delay = new Sim_normal_obj("Delay", mean, var);
        add_generator(delay);
        stat = new Sim_stat();
        stat.add_measure(Sim_stat.THROUGHPUT);
        stat.add_measure(Sim_stat.RESIDENCE_TIME);
        set_stat(stat);
    }

    public void body() {
        while (Sim_system.running()) {
            Sim_event e = new Sim_event();
            sim_get_next(e);
            if (e.get_src() == Sim_system.get_entity_id("ApiServer")) {
                sim_schedule(client, 0.0, 1);
            } else {
                sim_process(delay.sample());
                sim_trace(1, "CoreServer finished processing.");
                sim_trace(1, "CoreServer saving to database and sending request to ApiServer.");
                sim_schedule(database, 0.0, 1);
                sim_schedule(apiServerOut, 0.0, 1);
            }
            sim_completed(e);
        }
    }
}

class ApiServer extends Sim_entity {
    private Sim_port in, companyIn, database, gol, latam, azul, avianca, coreServer;
    private Sim_normal_obj delayLatam, delayGol, delayAzul, delayAvianca;
    private Sim_random_obj prob;
    private Sim_stat stat;

    ApiServer(String name, double mean, double var) {
        super(name);
        in = new Sim_port("In");
        companyIn = new Sim_port("CompanyIn");
        database = new Sim_port("Database");
        coreServer = new Sim_port("CoreServer");
        latam = new Sim_port("Latam");
        gol = new Sim_port("Gol");
        azul = new Sim_port("Azul");
        avianca = new Sim_port("Avianca");
        add_port(in);
        add_port(companyIn);
        add_port(database);
        add_port(coreServer);
        add_port(latam);
        add_port(gol);
        add_port(azul);
        add_port(avianca);
        delayLatam = new Sim_normal_obj("DelayLatam", 6, 0.5);
        delayGol = new Sim_normal_obj("DelayGol", 10, 1);
        delayAzul = new Sim_normal_obj("DelayAzul", 7, 0.8);
        delayAvianca = new Sim_normal_obj("DelayAvianca", 13, 1.5);
        add_generator(delayLatam);
        add_generator(delayGol);
        add_generator(delayAzul);
        add_generator(delayAvianca);
        prob = new Sim_random_obj("Probability");
        stat = new Sim_stat();
        stat.add_measure(Sim_stat.THROUGHPUT);
        stat.add_measure(Sim_stat.RESIDENCE_TIME);
        set_stat(stat);
    }

    public void body() {
        while (Sim_system.running()) {
            Sim_event e = new Sim_event();
            sim_get_next(e);
            if (e.get_src() == Sim_system.get_entity_id("Latam")) {
                sim_trace(1, "ApiServer got response from Latam");
                sim_process(delayLatam.sample());
                sendResponseToCoreServer("Latam");
            } else if (e.get_src() == Sim_system.get_entity_id("Gol")) {
                sim_trace(1, "ApiServer got response from Gol");
                sim_process(delayGol.sample());
                sendResponseToCoreServer("Gol");
            } else if (e.get_src() == Sim_system.get_entity_id("Azul")) {
                sim_trace(1, "ApiServer got response from Azul");
                sim_process(delayAzul.sample());
                sendResponseToCoreServer("Azul");
            } else if (e.get_src() == Sim_system.get_entity_id("Avianca")) {
                sim_trace(1, "ApiServer got response from Avianca");
                sim_process(delayAvianca.sample());
                sendResponseToCoreServer("Avianca");
            } else {
                sim_trace(1, "ApiServer sending requests to CompanyServer");
                sim_schedule(gol, 0.0, 1);
                sim_schedule(azul, 0.0, 1);
                sim_schedule(latam, 0.0, 1);
                sim_schedule(avianca, 0.0, 1);
            }
            sim_completed(e);
        }
    }

    public void sendResponseToCoreServer(String company) {
        sim_trace(1, "ApiServer saving to database");
        sim_schedule(database, 0.0, 1);
        sim_trace(1, "ApiServer finished processing " + company + " response");
        sim_trace(1, "ApiServer sending response to CoreServer");
        sim_schedule(coreServer, 0.0, 1);
    }
}

class CompanyServer extends Sim_entity {
    private Sim_port in, out;
    private Sim_negexp_obj delay;
    private Sim_stat stat;

    CompanyServer(String name, double mean) {
        super(name);
        in = new Sim_port("In");
        out = new Sim_port("Out");
        add_port(in);
        add_port(out);
        delay = new Sim_negexp_obj("Delay", mean);
        add_generator(delay);
        stat = new Sim_stat();
        stat.add_measure(Sim_stat.THROUGHPUT);
        stat.add_measure(Sim_stat.RESIDENCE_TIME);
        set_stat(stat);
    }

    public void body() {
        while (Sim_system.running()) {
            Sim_event e = new Sim_event();
            sim_get_next(e);
            sim_process(delay.sample());
            sim_trace(1, "CompanyServer (" + this.get_name() + ") sent response.");
            sim_completed(e);
            sim_schedule(out, 0.0, 1);
        }
    }
}

class Database extends Sim_entity {
    private Sim_port in;
    private Sim_negexp_obj delay;
    private Sim_stat stat;

    Database(String name, double mean) {
        super(name);
        in = new Sim_port("In");
        add_port(in);
        delay = new Sim_negexp_obj("Delay", mean);
        add_generator(delay);
        stat = new Sim_stat();
        stat.add_measure(Sim_stat.UTILISATION);
        set_stat(stat);
    }

    public void body() {
        while (Sim_system.running()) {
            Sim_event e = new Sim_event();
            sim_get_next(e);
            sim_process(delay.sample());
            sim_completed(e);
            sim_trace(1, "Database (" + this.get_name() + ") finished processing.");
        }
    }
}

public class ProcessorSubsystem2 {
    public static void main(String[] args) {
        Sim_system.initialise();
        Client client = new Client("Client", 80.0);
        CoreServer coreServer = new CoreServer("CoreServer", 0.5, 0.01);
        ApiServer apiServer = new ApiServer("ApiServer", 2, 0.1);
        CompanyServer latam = new CompanyServer("Latam", 2.2);
        CompanyServer gol = new CompanyServer("Gol", 2);
        CompanyServer azul = new CompanyServer("Azul", 2.5);
        CompanyServer avianca = new CompanyServer("Avianca", 4);
        Database dbCore = new Database("DatabaseCore", 0.1);
        Database dbApi = new Database("DatabaseApi", 0.2);
        Sim_system.link_ports("Client", "Out", "CoreServer", "In");
        Sim_system.link_ports("CoreServer", "Client", "Client", "In");
        Sim_system.link_ports("CoreServer", "Database", "DatabaseCore", "In");
        Sim_system.link_ports("CoreServer", "ApiServerOut", "ApiServer", "In");
        Sim_system.link_ports("ApiServer", "Database", "DatabaseApi", "In");
        Sim_system.link_ports("ApiServer", "CoreServer", "CoreServer", "ApiServerIn");
        Sim_system.link_ports("ApiServer", "Latam", "Latam", "In");
        Sim_system.link_ports("ApiServer", "Gol", "Gol", "In");
        Sim_system.link_ports("ApiServer", "Azul", "Azul", "In");
        Sim_system.link_ports("ApiServer", "Avianca", "Avianca", "In");
        Sim_system.link_ports("Latam", "Out", "ApiServer", "CompanyIn");
        Sim_system.link_ports("Gol", "Out", "ApiServer", "CompanyIn");
        Sim_system.link_ports("Azul", "Out", "ApiServer", "CompanyIn");
        Sim_system.link_ports("Avianca", "Out", "ApiServer", "CompanyIn");
        Sim_system.set_trace_detail(false, true, false);
        Sim_system.set_termination_condition(Sim_system.EVENTS_COMPLETED,"CoreServer", 0, 1000, false);
        Sim_system.run();
        System.out.println(Sim_system.get_entity_id("Client"));
        System.out.println(Sim_system.get_entity_id("CoreServer"));
        System.out.println(Sim_system.get_entity_id("ApiServer"));
        System.out.println(Sim_system.get_entity_id("Latam"));
    }
}
