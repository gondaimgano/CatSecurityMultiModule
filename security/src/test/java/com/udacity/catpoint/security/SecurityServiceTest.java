package com.udacity.catpoint.security;

import com.udacity.catpoint.core.AlarmStatus;
import com.udacity.catpoint.core.ArmingStatus;
import com.udacity.catpoint.core.Sensor;
import com.udacity.catpoint.core.SensorType;
import com.udacity.catpoint.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private Sensor simulatorSensor;

    private final String randomString = UUID.randomUUID().toString();

    @Mock
    SecurityRepository securityRepository;

   @Mock
    ImageService imageService;

    private
    SecurityService securityService;


    private Sensor generateSensor() {
        return new Sensor(randomString, SensorType.DOOR);
    }

    Set<Sensor> generateSensors(int count) {
        if(count<0)
            return new HashSet<>();
        return IntStream.range(0,count).mapToObj(i->
                generateSensor()).collect(Collectors.toSet());
    }

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository, imageService);
        simulatorSensor = generateSensor();
    }


    @ParameterizedTest //covers 1
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void whenAlarmStatus_AlarmArmedAndSensorActivated_ReturnAlarmStatusPending(ArmingStatus armingStatus) {
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(simulatorSensor, true);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.PENDING_ALARM);
    }

    @Test
        //covers 2
    void whenAlarmStatus_AlarmAlreadyPendingAndSensorActivated_ReturnAlarmStatusAlarm() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(simulatorSensor, true);
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.ALARM); //first call up
    }

    @Test //tests 3
    public void whenPendingAlarmStatus_andArmingStatusArmed_ReturnNoAlarmStatus() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        simulatorSensor.setActive(false);
        securityService.changeSensorActivationStatus(simulatorSensor, true);
        securityService.changeSensorActivationStatus(simulatorSensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


    @Test //tests 3
    void changeAlarmStatus_alarmPendingAndAllSensorsInactive_changeToNoAlarm(){
        Set<Sensor> allSensors = generateSensors(4);
        Sensor last = allSensors.iterator().next();
        last.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(last, false);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }
    @Test
        //tests 4
    void changeAlarmState_alarmActiveAndSensorStateChanges_stateNotAffected() {
        simulatorSensor.setActive(false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(simulatorSensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
        ArgumentCaptor<Sensor> captor = ArgumentCaptor.forClass(Sensor.class);
        verify(securityRepository, atMostOnce()).updateSensor(captor.capture());
        assertEquals(captor.getValue(), simulatorSensor);
        simulatorSensor.setActive(true);
        securityService.changeSensorActivationStatus(simulatorSensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
        verify(securityRepository, atMost(2)).updateSensor(captor.capture());
        assertEquals(captor.getValue(), simulatorSensor);
    }


    @Test
        //tests 5
    void whenAlarmState_systemActivatedWhileAlreadyActiveAndAlarmPending_changeToAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(simulatorSensor, true);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }

    @ParameterizedTest //tests 6
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void whenAlarmState_sensorDeactivateWhileInactive_noChangeToAlarmState(AlarmStatus alarmStatus) {
        securityService.changeSensorActivationStatus(simulatorSensor, false);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test
        //tests 7
    void whenAlarmState_imageContainingCatDetectedAndSystemArmed_changeToAlarmStatus() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
        //tests 8
    void whenAlarmState_noCatImageIdentifiedAndSensorsAreInactive_changeToAlarmStatus() {
      //  when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        simulatorSensor.setActive(false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
        //tests 9
    void whenAlarmStatus_systemDisArmed_changeToAlarmStatus() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


    @ParameterizedTest //tests 10
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    void whenSensors_systemArmed_deactivateAllSensors(ArmingStatus armingStatus){
        Set<Sensor> sensors = generateSensors(4);
        securityService.setArmingStatus(armingStatus);
        sensors.forEach(it -> assertEquals(it.getActive(), false));
    }





    //Test 11
    @Test
    public void whenSystemArmed_HomeCatIdentified_setStatusAlarm() {
       when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test 12
    @Test
    public void whenSystemArmed_setStatusAlarm() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(securityService.getArmingStatus());
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }


}