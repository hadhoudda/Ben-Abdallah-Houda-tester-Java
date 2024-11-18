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

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
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

    @BeforeEach
    private void setUpPerTest() throws Exception {
        // Simule la lecture de la voiture et l'attribution de la place
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown() {
    }

    @Test
    public void testParkingACar() throws Exception {
        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        Connection con = dataBaseTestConfig.getConnection();
        //requetes
        String ticketSql = "SELECT * FROM ticket WHERE VEHICLE_REG_NUMBER = 'ABCDEF'";
        String parkingSpotSql = "SELECT AVAILABLE FROM parking WHERE PARKING_NUMBER = 1";

        try {
            // Envoi des requetes
            PreparedStatement psTicket = con.prepareStatement(ticketSql);
            PreparedStatement psParkingSpot = con.prepareStatement(parkingSpotSql);

            // Verifie que le ticket a enregistre dans la bdd
            ResultSet rsTicket = psTicket.executeQuery();
            assertTrue(rsTicket.next());

            // Verifie que la place de parking est occupée (AVAILABLE = false)
            ResultSet rsParkingSpot = psParkingSpot.executeQuery();
            assertTrue(rsParkingSpot.next());
            boolean isAvailable = rsParkingSpot.getBoolean("AVAILABLE");
            assertFalse(isAvailable);
            System.out.println("The place " + parkingSpot.getId() +" is occupied");
            //Verifier mise à jour parking
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
        Connection con = dataBaseTestConfig.getConnection();
        try {
            ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);
            //Entre Voiture
            testParkingACar();
            Ticket ticket = ticketDAO.getTicket("ABCDEF");
            System.out.println("/////////temps de stationnement = 45 minute//////////");
            Date inTime = new Date();
            inTime.setTime(inTime.getTime() - (45 * 60 * 1000));
            System.out.println("+++++++++++++++++");
            System.out.println("Incoming vehicle ticket found: " + ticket.getId());
            System.out.println("In-time: " + inTime);
            assertNotNull(ticket);

            // mise à jour heure d'entre
            try {
                String timeSql = "UPDATE ticket SET IN_TIME = ? WHERE ID = ?";
                PreparedStatement psTime = con.prepareStatement(timeSql);

                // Convertir java.util.Date en java.sql.Timestamp
                Timestamp timestamp = new Timestamp(inTime.getTime());
                psTime.setTimestamp(1, timestamp);
                psTime.setInt(2, ticket.getId());
                psTime.executeUpdate();
                psTime.close();

            } catch (Exception er) {
                er.printStackTrace();
                throw new RuntimeException("Failed to update test ticket with earlier inTime value");
            }

            // sortir Vehicle
            parkingService.processExitingVehicle();

            // Assert: Check the exiting ticket
            Ticket exitingVehicleTicket = ticketDAO.getTicket("ABCDEF");
            assertNotNull(exitingVehicleTicket);
            System.out.println("Exiting vehicle ticket found: " + exitingVehicleTicket.getId());
            System.out.println("In-time: " + exitingVehicleTicket.getInTime());
            System.out.println("Out-time: " + exitingVehicleTicket.getOutTime());
            System.out.println("Price: " + exitingVehicleTicket.getPrice());
            System.out.println(parkingSpot.isAvailable());
            //place devient disponible
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
           //premier fois d'entre et sortie de vehicule
           testParkingLotExit();
           //Rentre
            parkingService.processIncomingVehicle();
           //sortir
            parkingService.processExitingVehicle();
            double priceTest = 0.75* 1.5 * 0.95;
            double priceticket =    ticketDAO.getTicket("ABCDEF").getPrice();
           assertEquals(priceTest, priceticket, 0.01);
        }catch (Exception e) {
           e.printStackTrace();
       }

    }

}