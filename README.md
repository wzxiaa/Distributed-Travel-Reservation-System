# Distributed Travel Reservation System

## Description

A cohesive multi-client, multi-server implementation of a Travel Reservation system based on RMI for communication. Customers can reserve flights, cars and rooms for their vacation. There  are three types of resources:

- Flights: flight number, price, and the number of seats
- Cars: location, price, and the amount available
- Rooms: location, price, and the amount available

Distributed transaction and concurrency control are ensured. The distributed transaction is implemented based on strict 2PL (1-phase commit assuming nothing can go wrong after commit). 

The architecture overview:
![alt text](https://github.com/wzxiaa/Distributed-Travel-Reservation-System/blob/master/resource/architecture.png?raw=true)


