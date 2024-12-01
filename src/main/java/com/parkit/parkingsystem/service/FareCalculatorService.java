package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    /**
     * the method that to calculi the price of a ticket
     * @param ticket
     * @param discount
     */
    public void calculateFare(Ticket ticket, boolean discount) {
        double inHour = ticket.getInTime().getTime();
        double outHour = ticket.getOutTime().getTime();
        double duration = outHour - inHour; //time in ms
        final double DURATION_LIMIT = 30 * 60 * 1_000;//free station duration 30 minutes

        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }
        //a price of 0 if the duration in the car park is less than 30 minutes
        if (duration < DURATION_LIMIT) {
            ticket.setPrice(0);
            return;
        }
        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR: {
                    //standard price car
                    ticket.setPrice((duration/ (1000 * 60 * 60)) * Fare.CAR_RATE_PER_HOUR);
                break;
            }
            case BIKE: {
                    //standard price bike
                    ticket.setPrice((duration/ (1000 * 60 * 60)) * Fare.BIKE_RATE_PER_HOUR );
                break;
            }
            default:
                throw new IllegalArgumentException("Unkown Parking Type");
        }
        //price minus 5% for recurring users
        if (discount) {
            ticket.setPrice(ticket.getPrice() * 0.95);
        }
    }

    public void calculateFare(Ticket ticket) {
        FareCalculatorService fareCalculatorService = new FareCalculatorService();
        fareCalculatorService.calculateFare(ticket, false);
    }
}