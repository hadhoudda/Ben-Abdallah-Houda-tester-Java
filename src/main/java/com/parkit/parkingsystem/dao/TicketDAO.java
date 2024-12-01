package com.parkit.parkingsystem.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class TicketDAO {

    private static final Logger logger = LogManager.getLogger("TicketDAO");
    public DataBaseConfig dataBaseConfig = new DataBaseConfig();
    /**
     * A methode that to save the ticket to the database
     *
     * @param ticket
     * @return true or false
     */
    public boolean saveTicket(Ticket ticket) {
        Connection con = null;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.SAVE_TICKET);
            //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
            //ps.setInt(1,ticket.getId());
            ps.setInt(1, ticket.getParkingSpot().getId());
            ps.setString(2, ticket.getVehicleRegNumber());
            ps.setDouble(3, ticket.getPrice());
            ps.setTimestamp(4, new Timestamp(ticket.getInTime().getTime()));
            ps.setTimestamp(5, (ticket.getOutTime() == null) ? null : (new Timestamp(ticket.getOutTime().getTime())));
            return ps.execute();
        } catch (Exception ex) {
            logger.error("Error fetching next available slot", ex);
        } finally {
            dataBaseConfig.closeConnection(con);
            return false;
        }
    }

    /**
     * A method that retrieves a Ticket object from a given vehicle registration number
     *
     * @param vehicleRegNumber
     * @return ticket
     */
    public Ticket getTicket(String vehicleRegNumber) {
        Connection con = null;
        Ticket ticket = null;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.GET_TICKET);
            //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
            ps.setString(1, vehicleRegNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ticket = new Ticket();
                ParkingSpot parkingSpot = new ParkingSpot(rs.getInt(1), ParkingType.valueOf(rs.getString(6)), false);
                ticket.setParkingSpot(parkingSpot);
                ticket.setId(rs.getInt(2));
                ticket.setVehicleRegNumber(vehicleRegNumber);
                ticket.setPrice(rs.getDouble(3));
                ticket.setInTime(rs.getTimestamp(4));
                ticket.setOutTime(rs.getTimestamp(5));
            }
            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);
        } catch (Exception ex) {
            logger.error("Error fetching next available slot", ex);
        } finally {
            dataBaseConfig.closeConnection(con);
            return ticket;
        }
    }

    /**
     * A method takes a Ticket object as a parameter and returns a boolean,
     * indicating whether the update was successful or not.
     *
     * @param ticket
     * @return true or false
     */
    public boolean updateTicket(Ticket ticket) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = dataBaseConfig.getConnection();
            // Step 1: Retrieve the last ticket ID
            ps = con.prepareStatement(DBConstants.GET_LAST_TICKET_ID_QUERY);
            ps.setString(1, ticket.getVehicleRegNumber());
            rs = ps.executeQuery();
            if (rs.next()) {
                int lastTicketId = rs.getInt("LAST_TICKET_ID");
                // Check if a ticket was found
                if (lastTicketId > 0) {
                    //Step 2: Update the latest ticket
                    ps = con.prepareStatement(DBConstants.UPDATE_TICKET);
                    ps.setDouble(1, ticket.getPrice());
                    ps.setTimestamp(2, new Timestamp(ticket.getOutTime().getTime()));
                    ps.setInt(3, lastTicketId);  //Last ticket ID
                    int affectedRows = ps.executeUpdate();
                    if (affectedRows > 0) {
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error updating ticket info", ex);
        } finally {
            dataBaseConfig.closeConnection(con);
        }
        return false;
    }

    /**
     * count how many tickets are recorded for a vehicle
     *
     * @param vehicleRegNumber
     * @return countTicket
     */
    public int getNbTicket(String vehicleRegNumber) {
        int countTicket = 0;
        Connection con = null;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.GET_NB_TICKET);
            ps.setString(1, vehicleRegNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                countTicket = rs.getInt(1);
            }
            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);
        } catch (Exception ex) {
            logger.error("Error counting tickets for vehicle: " + vehicleRegNumber, ex);
        } finally {
            dataBaseConfig.closeConnection(con);
        }
        return countTicket;
    }
}