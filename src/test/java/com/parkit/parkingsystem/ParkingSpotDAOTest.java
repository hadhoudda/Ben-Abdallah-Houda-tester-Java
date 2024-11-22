package com.parkit.parkingsystem;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
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
        // Ce setup est exécuté avant chaque test pour préparer les mocks
    }

    @Test
    public void testUpdateParking_whenUpdateSucceeds() throws Exception {
        // GIVEN
        ParkingSpot parkingSpot = new ParkingSpot(101, ParkingType.CAR, false);

        when(dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // WHEN
        boolean result = parkingSpotDAO.updateParking(parkingSpot);

        // THEN
        assertTrue(result);
        verify(preparedStatement, times(1)).setBoolean(1, parkingSpot.isAvailable());
        verify(preparedStatement, times(1)).setInt(2, parkingSpot.getId());
        verify(preparedStatement, times(1)).executeUpdate();
    }

    @Test
    public void testUpdateParking_whenUpdateFails() throws Exception {
        // GIVEN
        ParkingSpot parkingSpot = new ParkingSpot(101, ParkingType.CAR, false);

        when(dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        // Simuler une mise à jour échouée
        when(preparedStatement.executeUpdate()).thenReturn(0);

        // WHEN
        boolean result = parkingSpotDAO.updateParking(parkingSpot);

        // THEN
        assertFalse(result);
        verify(preparedStatement, times(1)).setBoolean(1, parkingSpot.isAvailable());
        verify(preparedStatement, times(1)).setInt(2, parkingSpot.getId());
        verify(preparedStatement, times(1)).executeUpdate();
    }

    @Test
    public void testUpdateParking_whenSQLExceptionOccurs() throws Exception {
        // GIVEN
        ParkingSpot parkingSpot = new ParkingSpot(101, ParkingType.CAR, false);

        when(dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(any(String.class))).thenThrow(new RuntimeException("Database error"));

        // WHEN
        boolean result = parkingSpotDAO.updateParking(parkingSpot);

        // THEN
        assertFalse(result);
        verify(preparedStatement, never()).setBoolean(anyInt(), anyBoolean());
        verify(preparedStatement, never()).executeUpdate();
    }


    @Test
    public void testGetNextAvailableSlot_ValidParkingType() throws Exception {

        ParkingType parkingType = ParkingType.CAR;
        when(parkingSpotDAO.dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT)).thenReturn(preparedStatement);
        when(connection.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(5); // ID du parking

        //WHEN
        int availableSlot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);

        // THEN
        assertEquals(5, availableSlot);
        verify(preparedStatement).setString(1, parkingType.toString());
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
    }

    @Test
    public void testGetNextAvailableSlot_NoResult() throws Exception {

        ParkingType parkingType = ParkingType.CAR;
        when(parkingSpotDAO.dataBaseConfig.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // Aucun résultat

        // WHEN
        int availableSlot = parkingSpotDAO.getNextAvailableSlot(parkingType);

        // Vérification que le résultat est -1 si aucun slot disponible
        assertEquals(-1, availableSlot);
        verify(preparedStatement).setString(1, parkingType.toString());
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
    }

    @Test
    public void testGetNextAvailableSlot_Exception() throws Exception {
        // GIVEN
        when(parkingSpotDAO.dataBaseConfig.getConnection()).thenReturn(connection);

        ParkingType parkingType = ParkingType.CAR;

        // Simuler une exception SQL lors de l'exécution de la requête
        when(connection.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT)).thenThrow(new SQLException("Database error"));

        // Appel de la méthode
        int availableSlot = parkingSpotDAO.getNextAvailableSlot(parkingType);
}

}
