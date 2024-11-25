package com.parkit.parkingsystem;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static junit.framework.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingSpotDAOTest {
    @InjectMocks
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private DataBaseConfig dataBaseConfig;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private ResultSet resultSet;

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void testUpdateParking_whenUpdateSucceeds() throws Exception {
        //Given
        ParkingSpot parkingSpot = new ParkingSpot(101, ParkingType.CAR, false);
        when(dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        //When
        boolean result = parkingSpotDAO.updateParking(parkingSpot);
        //Then
        assertTrue(result);
        //Verify
        verify(preparedStatement, times(1)).setBoolean(1, parkingSpot.isAvailable());
        verify(preparedStatement, times(1)).setInt(2, parkingSpot.getId());
        verify(preparedStatement, times(1)).executeUpdate();
    }

    @Test
    public void testUpdateParking_whenUpdateFails() throws Exception {
        //Given
        ParkingSpot parkingSpot = new ParkingSpot(101, ParkingType.CAR, false);
        when(dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0);//Simulate a failed update
        //When
        boolean result = parkingSpotDAO.updateParking(parkingSpot);
        //Then
        assertFalse(result);
        //Verify
        verify(preparedStatement, times(1)).setBoolean(1, parkingSpot.isAvailable());
        verify(preparedStatement, times(1)).setInt(2, parkingSpot.getId());
        verify(preparedStatement, times(1)).executeUpdate();
    }

    @Test
    public void testUpdateParking_whenSQLExceptionOccurs() throws Exception {
        //Given
        ParkingSpot parkingSpot = new ParkingSpot(101, ParkingType.CAR, false);
        when(dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(any(String.class))).thenThrow(new RuntimeException("Database error"));
        //When
        boolean result = parkingSpotDAO.updateParking(parkingSpot);
        //Then
        assertFalse(result);
        //Verify
        verify(preparedStatement, never()).setBoolean(anyInt(), anyBoolean());
        verify(preparedStatement, never()).executeUpdate();
    }

    @Test
    public void testGetNextAvailableSlot_ValidParkingType() throws Exception {
        //Given
        ParkingType parkingType = ParkingType.CAR;
        when(parkingSpotDAO.dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT)).thenReturn(preparedStatement);
        when(connection.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(5); //Parking ID
        //When
        int availableSlot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        //Then
        assertEquals(5, availableSlot);
        //Verify
        verify(preparedStatement).setString(1, parkingType.toString());
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
    }

    @Test
    public void testGetNextAvailableSlot_NoResult() throws Exception {
        //Given
        ParkingType parkingType = ParkingType.CAR;
        when(parkingSpotDAO.dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        //When
        int availableSlot = parkingSpotDAO.getNextAvailableSlot(parkingType);
        //Then
        assertEquals(-1, availableSlot);//Checking that the result is -1 if no slots available
        //Verify
        verify(preparedStatement).setString(1, parkingType.toString());
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
    }

    @Test
    public void testGetNextAvailableSlot_Exception() throws Exception {
        // Given
        ParkingType parkingType = ParkingType.CAR;
        when(parkingSpotDAO.dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT)).thenThrow(new SQLException("Database error"));//Simulate SQL exception during query execution
        //When
        parkingSpotDAO.getNextAvailableSlot(parkingType);
    }

}