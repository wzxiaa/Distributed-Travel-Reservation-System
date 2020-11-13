# comp512-project

### run the RMI resource manager:

```
cd RMI/Server/
make
./run_server.sh [<rmi_name>] [<port_number>]# starts a single ResourceManager
```

To run the Middleware:
```
cd RMI/Server/
make
./run_middleware.sh Flights,[<Flights-ip>],[<Flights-port>] Cars,[<Cars-ip>],[<Cars-port>] Rooms,[<Rooms-ip>],[<Rooms-port>]
```

To run the RMI client:

```
cd RMI/Client
make
./run_client.sh [<server_hostname>] Middleware
```



### run the RMI_Performance_Analysis to execute test code:

```
cd RMI_Performance_Analysis/Server/
make
./run_server.sh [<rmi_name>] [<port_number>]# starts a single ResourceManager
```

To run the Middleware:
```
cd RMI_Performance_Analysis/Server/
make
./run_middleware.sh Flights,[<Flights-ip>],[<Flights-port>] Cars,[<Cars-ip>],[<Cars-port>] Rooms,[<Rooms-ip>],[<Rooms-port>]
```

To run the RMI client:

```
cd RMI_Performance_Analysis/Client
make
./run_client_test1.sh [<server_hostname>] Middleware
```
