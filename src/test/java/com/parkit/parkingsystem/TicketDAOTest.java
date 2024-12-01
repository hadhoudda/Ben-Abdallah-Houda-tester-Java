package com.parkit.parkingsystem;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;

import static com.parkit.parkingsystem.constants.ParkingType.CAR;
import static junit.framework.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TicketDAOTest {

    @InjectMocks
    private static TicketDAO ticketDAO;
    @InjectMocks
    private static Ticket ticket;
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
        //Given
        when(ticketDAO.dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.SAVE_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(true);
        //When
        Ticket ticketTest = getTicket();
        ticketDAO.saveTicket(ticketTest);
        //Then
        assertNotNull(ticketTest);
        //Verify
        verify(preparedStatement).setInt(1, ticketTest.getParkingSpot().getId());
        verify(preparedStatement).setString(2, ticketTest.getVehicleRegNumber());
        verify(preparedStatement).setDouble(3, ticketTest.getPrice());
        verify(preparedStatement).setTimestamp(4, new Timestamp(ticketTest.getInTime().getTime()));
        verify(preparedStatement).setTimestamp(5, new Timestamp(ticketTest.getOutTime().getTime()));
        verify(preparedStatement).execute();
    }

    @Test
    public void saveTicketFailureTest() throws SQLException, ClassNotFoundException {
        //Given
        when(ticketDAO.dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.SAVE_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.execute()).thenReturn(false);//Simulate query execution failure (execute returns false)
        //When
        Ticket ticketTest = getTicket();
        ticketDAO.saveTicket(ticketTest);
        //Verify
        verify(preparedStatement).execute();
    }

    @Test
    public void updateTicketTest() throws SQLException, ClassNotFoundException {
        MockitoAnnotations.openMocks(this); //Initializing mocks
        when(ticketDAO.dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_LAST_TICKET_ID_QUERY)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("LAST_TICKET_ID")).thenReturn(3); //Return an ID from the last ticket exemple: 3
        when(connection.prepareStatement(DBConstants.UPDATE_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        // Given
        Ticket ticketTest = getTicket();
        // When
        boolean updateResult = ticketDAO.updateTicket(ticketTest); // Appel de la méthode updateTicket()
        // Then
        assertNotNull(ticketTest); // Vérifie que le ticket n'est pas nul
        assertEquals("ABCDEF", ticketTest.getVehicleRegNumber()); // Vérifie l'immatriculation
        assertEquals(1.5, ticketTest.getPrice(), 0.01); // Vérifie le prix du ticket avec une tolérance
        assertTrue(updateResult);
        //verify
        verify(preparedStatement).executeUpdate();
    }

    @Test
    public void getNbTicketTest() throws SQLException, ClassNotFoundException {
        // Given
        when(dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_NB_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(3); // user 3 times
        //When
        int ticketCount = ticketDAO.getNbTicket("ABCDEF");
        //Then
        assertEquals(3, ticketCount);
        //Verify
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
        // Given
        when(dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_NB_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        //When
        int ticketCount = ticketDAO.getNbTicket("ABCDEF");
        //Then
        assertEquals(0, ticketCount);
    }

    @Test
    public void testGetTicket_whenTicketDoesNotExist() throws Exception {
        //Given
        when(dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        //When
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        //Then
        assertNull(ticket);
    }

    @Test
    public void testGetTicket_whenSQLExceptionOccurs() throws Exception {
        //Given
        when(dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_TICKET)).thenThrow(new RuntimeException("Database error"));
        //When
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        //Then
        assertNull(ticket);
    }

    @Test
    public void testGetTicket_whenTicketExists() throws Exception {
        //Given
        when(ticketDAO.dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_TICKET)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(101); //PARKING_NUMBER
        when(resultSet.getDouble(3)).thenReturn(2.5); //PRICE
        when(resultSet.getTimestamp(4)).thenReturn(Timestamp.valueOf("2024-11-22 10:00:00")); //IN_TIME
        when(resultSet.getString(6)).thenReturn("CAR"); //TYPE
        //When
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        //Then
        assertNotNull(ticket);
        assertEquals(101, ticket.getParkingSpot().getId());
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertEquals(2.5, ticket.getPrice());
        assertNotNull(ticket.getInTime());
        assertEquals(CAR, ticket.getParkingSpot().getParkingType());
    }
}