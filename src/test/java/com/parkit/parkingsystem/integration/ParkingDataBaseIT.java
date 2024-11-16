package com.parkit.parkingsystem.integration;

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
        dataBasePrepareService.clearDataBaseEntries();

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
            //assertTrue(ticketDAO.saveTicket(ticket));

            // Verifie que la place de parking est occupée (AVAILABLE = false)
            ResultSet rsParkingSpot = psParkingSpot.executeQuery();
            assertTrue(rsParkingSpot.next());


            // La place de parking devrait etre marque comme "occupee" (AVAILABLE = false)
            boolean isAvailable = rsParkingSpot.getBoolean("AVAILABLE");
            assertFalse(isAvailable);

        } catch (SQLException e) {
            e.printStackTrace();
            dataBaseTestConfig.closeConnection(con);
        } finally {
            dataBaseTestConfig.closeConnection(con);
        }
    }




    @Test
    public void testParkingLotExit() throws Exception {

        // GIVEN
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        System.out.println("/////////temps de stationnement = 45 minute//////////");
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (45 * 60 * 1000));//45 minutes parking
        Date outTime = new Date();
        //temps d'embarquement en minute
        System.out.println("temps de stationnement est : " + (outTime.getTime() - inTime.getTime())/(1000*60));
        System.out.println("///////////////////");
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        // THEN
        parkingService.processExitingVehicle();

        // WHEN
        Connection con = dataBaseTestConfig.getConnection();
        // Format des dates pour la requete SQL
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // Format des dates en chaînes
        String formattedInTime = dateFormat.format(inTime);
        String formattedOutTime = dateFormat.format(outTime.getTime());

        // Requete SQL pour mettre à jour les heures d'entrée et de sortie
        String timeSql = "UPDATE ticket  IN_TIME = ?  OUT_TIME = ? "
                + "WHERE VEHICLE_REG_NUMBER = 'ABCDEF'";
        //Requete pour recuper le prix de stationnement
        String priceSql = "SELECT PRICE FROM ticket WHERE VEHICLE_REG_NUMBER = 'ABCDEF' and PARKING_NUMBER = 1";

        try {
            // Envoi des requetes
            PreparedStatement psTime = con.prepareStatement(timeSql);

            //execute temps
            psTime.setString(1, formattedInTime);
            psTime.setString(2, formattedOutTime);

            //assertTrue(psTime.execute());

            PreparedStatement psPrice = con.prepareStatement(priceSql);
            // Voir le prix de ticket
            ResultSet rsPrice = psPrice.executeQuery();

            System.out.println("***********************");

            // Verification du prix
            if (rsPrice.next()) {
                double price = rsPrice.getDouble("PRICE");
                System.out.println("Prix du ticket : " + price);
                // Assertion : Le prix calculé doit être égal à 1.5 * 0.75
                assertEquals(1.5 * 0.75, price);
                System.out.println("************************");
            }

    } catch (SQLException e) {
            e.printStackTrace();
            dataBaseTestConfig.closeConnection(con);
        } finally {
            dataBaseTestConfig.closeConnection(con);
        }
    }

}
