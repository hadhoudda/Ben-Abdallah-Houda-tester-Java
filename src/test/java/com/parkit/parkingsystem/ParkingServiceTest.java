package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    @InjectMocks
    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;



    private Ticket getTicket() {
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date());
        ticket.setOutTime(new Date());
        ticket.setPrice(0);
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        return ticket;
    }

    @BeforeEach
    public void setUpPerTest() {
        try {
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processExitingVehicleTest() throws Exception {
        //GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getNbTicket(anyString())).thenReturn(1);
        when(ticketDAO.getTicket(anyString())).thenReturn(getTicket());
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        //WHEN
        parkingService.processExitingVehicle();

        //THEN
        verify(inputReaderUtil, Mockito.times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));

    }

    @Test
    public void testProcessIncomingVehicle() throws Exception {
        //GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(1);

        //WHEN
        parkingService.processIncomingVehicle();

        //THEN
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
    }

    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {
        //GIVEN
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getNbTicket(anyString())).thenReturn(1);
        when(ticketDAO.getTicket(anyString())).thenReturn(getTicket());
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        //WHEN
        parkingService.processExitingVehicle();

        //THEN
        verify(inputReaderUtil, Mockito.times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
    }

    @Test
    public void testGetNextParkingNumberIfAvailable() {
        //GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);

        //THEN
        ParkingSpot result = new ParkingSpot(1, ParkingType.CAR, true);
        parkingService.getNextParkingNumberIfAvailable();

        //WHEN
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
        verifyNoMoreInteractions(parkingSpotDAO);
        assertThat(parkingService.getNextParkingNumberIfAvailable()).isEqualTo(result);
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        //GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);

        //THEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        //WHEN
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);
        assertNull(parkingSpot);

    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() throws Exception {
        //GIVEN
        //3 : valeur incorrect
        when(inputReaderUtil.readSelection()).thenReturn(3);

        //THEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        //WHEN
        assertNull(parkingSpot);

        // Verifie que aucune interaction avec parkingSpotDAO ne devrait avoir lieu
        verifyZeroInteractions(parkingSpotDAO);
    }

}
