package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @AfterAll
    private static void tearDown() {
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar() throws Exception {
        //Given
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);
        //When
        parkingService.processIncomingVehicle();
        //Given
        Connection con = dataBaseTestConfig.getConnection();
        String ticketSql = "SELECT * FROM ticket WHERE VEHICLE_REG_NUMBER = 'ABCDEF'";
        String parkingSpotSql = "SELECT AVAILABLE FROM parking WHERE PARKING_NUMBER = 1";
        try {
            PreparedStatement psTicket = con.prepareStatement(ticketSql);
            PreparedStatement psParkingSpot = con.prepareStatement(parkingSpotSql);
            ResultSet rsTicket = psTicket.executeQuery();
            //Then
            assertTrue(rsTicket.next());
            ResultSet rsParkingSpot = psParkingSpot.executeQuery();
            assertTrue(rsParkingSpot.next());
            boolean isAvailable = rsParkingSpot.getBoolean("AVAILABLE");
            assertFalse(isAvailable);
            System.out.println("The place " + parkingSpot.getId() + " is occupied");
            assertTrue(parkingSpotDAO.updateParking(parkingSpot));
        } catch (SQLException e) {
            e.printStackTrace();
            dataBaseTestConfig.closeConnection(con);
        } finally {
            dataBaseTestConfig.closeConnection(con);
        }
    }

    @Test
    public void testParkingLotExit() throws Exception {
        //Given
        Connection con = dataBaseTestConfig.getConnection();
        try {
            ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);
            //enter car in parking
            testParkingACar();
            Ticket ticket = ticketDAO.getTicket("ABCDEF");
            Date inTime = new Date();
            inTime.setTime(inTime.getTime() - (45 * 60 * 1000));//45 minute
            System.out.println("Incoming vehicle ticket found: " + ticket.getId());
            System.out.println("In-time: " + inTime);
            //entry time update
            try {
                String timeSql = "UPDATE ticket SET IN_TIME = ? WHERE ID = ?";
                PreparedStatement psTime = con.prepareStatement(timeSql);
                Timestamp timestamp = new Timestamp(inTime.getTime());
                psTime.setTimestamp(1, timestamp);
                psTime.setInt(2, ticket.getId());
                psTime.executeUpdate();
                psTime.close();
            } catch (Exception er) {
                er.printStackTrace();
                throw new RuntimeException("Failed to update test ticket with earlier inTime value");
            }
            //When
            parkingService.processExitingVehicle();
            // Assert
            Ticket exitingVehicleTicket = ticketDAO.getTicket("ABCDEF");
            assertNotNull(exitingVehicleTicket);
            System.out.println("Exiting vehicle ticket found: " + exitingVehicleTicket.getId());
            System.out.println("In-time: " + exitingVehicleTicket.getInTime());
            System.out.println("Out-time: " + exitingVehicleTicket.getOutTime());
            System.out.println("Price: " + exitingVehicleTicket.getPrice());
            System.out.println(parkingSpot.isAvailable());
            //place becomes available
            assertTrue(parkingSpot.isAvailable());
            double timeTest = exitingVehicleTicket.getOutTime().getTime() - exitingVehicleTicket.getInTime().getTime();
            double priceTest = (Fare.CAR_RATE_PER_HOUR * timeTest) / (60 * 60 * 1000);
            assertEquals(priceTest, exitingVehicleTicket.getPrice(), 0.01);
        } catch (SQLException e) {
            e.printStackTrace();
            dataBaseTestConfig.closeConnection(con);
        } finally {
            dataBaseTestConfig.closeConnection(con);
        }
    }

    @Test
    public void testParkingLotExitRecurringUser() throws Exception {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        try {
            //first time entering and exiting vehicle
            testParkingLotExit();
            //second time entering vehicle
            parkingService.processIncomingVehicle();
            //second time exiting vehicle
            parkingService.processExitingVehicle();
            double priceTest = 0.75 * 1.5 * 0.95;
            double priceticket = ticketDAO.getTicket("ABCDEF").getPrice();
            assertEquals(priceTest, priceticket, 0.01);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}