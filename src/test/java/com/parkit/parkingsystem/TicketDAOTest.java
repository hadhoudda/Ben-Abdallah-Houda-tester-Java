package com.parkit.parkingsystem;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;

import static com.parkit.parkingsystem.constants.ParkingType.CAR;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketDAOTest {

    @InjectMocks
    private static TicketDAO ticketDAO;
    @InjectMocks
    private static Ticket ticket;
    @Mock
    private static ParkingSpot parkingSpot;
    @Mock
    private static DataBasePrepareService dataBasePrepareService;
    @Mock
    private DataBaseConfig dataBaseConfig;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private ResultSet resultSet;

    // Initialisation de l'objet ticket pour le test
    private Ticket getTicket() {
        ParkingSpot parkingSpot = new ParkingSpot(1, CAR, false);
        ticket.setParkingSpot(parkingSpot);
        ticket.setId(parkingSpot.getId());
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setPrice(1.5);
        ticket.setInTime(new Timestamp(System.currentTimeMillis() - 6000));
        ticket.setOutTime(new Timestamp(System.currentTimeMillis()));
        return ticket;
    }

    private ParkingSpot getParkingSpot() {
        parkingSpot.setId(1);
        parkingSpot.setParkingType(ParkingType.CAR);
        parkingSpot.setAvailable(true);
        return parkingSpot;
    }

    @BeforeEach
    public void setUp() {
        try {
            dataBasePrepareService.clearDataBaseEntries();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }


    @Test
    public void saveTicketTest() throws SQLException, ClassNotFoundException {
        // GIVEN
        when(ticketDAO.dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.SAVE_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);

        //WHEN
        Ticket ticketTest = getTicket();
        ticketDAO.saveTicket(ticketTest);

        //THEN
        assertNotNull(ticketTest);
        verify(preparedStatement).setInt(1, ticketTest.getParkingSpot().getId());
        verify(preparedStatement).setString(2, ticketTest.getVehicleRegNumber());
        verify(preparedStatement).setDouble(3, ticketTest.getPrice());
        verify(preparedStatement).setTimestamp(4, new Timestamp(ticketTest.getInTime().getTime()));
        verify(preparedStatement).setTimestamp(5, new Timestamp(ticketTest.getOutTime().getTime()));
        verify(preparedStatement).execute();
    }

    @Test
    public void saveTicketFailureTest() throws SQLException, ClassNotFoundException {
        when(ticketDAO.dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.SAVE_TICKET)).thenReturn(preparedStatement);
        // Simuler l'échec de l'exécution de la requête (execute retourne false)
        when(preparedStatement.execute()).thenReturn(false);

        // WHEN
        Ticket ticketTest = getTicket();
        ticketDAO.saveTicket(ticketTest);

        // THEN
        verify(preparedStatement).execute();
    }

    @Test
    public void updateTicketTest() throws SQLException, ClassNotFoundException {
        // GIVEN
        when(ticketDAO.dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.UPDATE_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);

        //WHEN
        Ticket ticketTest = getTicket();
        ticketDAO.updateTicket(ticketTest);

//        //THEN
        assertNotNull(ticketTest);
        assertEquals("ABCDEF", ticketTest.getVehicleRegNumber());
        assertEquals(1.5, ticketTest.getPrice());
        verify(preparedStatement).execute();
    }

    @Test
    public void updateTicketFailureTest() throws SQLException, ClassNotFoundException {
        when(ticketDAO.dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.UPDATE_TICKET)).thenReturn(preparedStatement);
        // Simuler l'échec de l'exécution de la requête (execute retourne false)
        when(preparedStatement.execute()).thenReturn(false);

        // WHEN
        Ticket ticketTest = getTicket();
        ticketDAO.updateTicket(ticketTest);

        // THEN
        verify(preparedStatement).execute();
    }

    @Test
    public void getNbTicketTest() throws SQLException, ClassNotFoundException {
        // GIVEN
        when(dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_NB_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(3); // user 3 times

        int ticketCount = ticketDAO.getNbTicket("ABCDEF");


        assertEquals(3, ticketCount);


        verify(dataBaseConfig).getConnection();
        verify(connection).prepareStatement(DBConstants.GET_NB_TICKET);
        verify(preparedStatement).setString(1, "ABCDEF");
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
        verify(resultSet).getInt(1);
        System.out.println("user passes for the " + ticketCount + " time");
    }

    @Test
    public void getNbTicketFailureTest() throws SQLException, ClassNotFoundException {
        // GIVEN
        when(dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_NB_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        int ticketCount = ticketDAO.getNbTicket("ABCDEF");

        assertEquals(0, ticketCount);

    }

    @Test
    public void testGetTicket_whenTicketDoesNotExist() throws Exception {
        when(dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(false);

        // WHEN
        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        // THEN
        assertNull(ticket);
    }

    @Test
    public void testGetTicket_whenSQLExceptionOccurs() throws Exception {
        // GIVEN
        when(dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_TICKET)).thenThrow(new RuntimeException("Database error"));

        // WHEN
        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        // THEN
        assertNull(ticket);
    }


    @Test
    public void testGetTicket_whenTicketExists() throws Exception {
        dataBaseConfig = mock(DataBaseConfig.class);
        when(ticketDAO.dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        when(resultSet.getInt(1)).thenReturn(101); // PARKING_NUMBER
        //  when(resultSet.getInt(2)).thenReturn(123); // ID
        when(resultSet.getDouble(3)).thenReturn(2.5); // PRICE
        when(resultSet.getTimestamp(4)).thenReturn(Timestamp.valueOf("2024-11-22 10:00:00")); // IN_TIME
        when(resultSet.getString(6)).thenReturn("CAR"); // TYPE

        // Appel de la méthode
        Ticket ticket = ticketDAO.getTicket("AKLF69");

        // Vérification des résultats
        assertNotNull(ticket);
        assertEquals(101, ticket.getParkingSpot().getId());
        assertEquals("AKLF69", ticket.getVehicleRegNumber());
        assertEquals(2.5, ticket.getPrice());
        assertNotNull(ticket.getInTime());
        assertEquals(CAR, ticket.getParkingSpot().getParkingType());
    }

}


