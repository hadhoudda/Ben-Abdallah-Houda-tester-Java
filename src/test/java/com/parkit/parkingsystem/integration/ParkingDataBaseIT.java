package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.DBConstants;
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

import static junit.framework.Assert.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
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
    private static void tearDown(){

    }

    @Test
    public void testParkingACar() throws Exception {
        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);
        dataBasePrepareService.clearDataBaseEntries();

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
            assertTrue(rsTicket.next(), "Le ticket n'a pas été enregistré dans la base de données.");

            // Verifie que la place de parking est occupée (AVAILABLE = false)
            ResultSet rsParkingSpot = psParkingSpot.executeQuery();
            assertTrue(rsParkingSpot.next(), "La place de parking n'a pas été trouvée dans la base de données.");

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


//    @Test
//    public void testParkingLotExit(){
//        testParkingACar();
//        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
//        parkingService.processExitingVehicle();
//        //TODO: check that the fare generated and out time are populated correctly in the database
    //vérifier que le tarif généré et l'heure de départ sont correctement renseignés dans la base de données
//    }

}
