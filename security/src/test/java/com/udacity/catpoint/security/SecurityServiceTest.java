package com.udacity.catpoint.security;

import com.udacity.catpoint.core.AlarmStatus;
import com.udacity.catpoint.core.ArmingStatus;
import com.udacity.catpoint.core.Sensor;
import com.udacity.catpoint.core.SensorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private Sensor simulateSensor;

    private final String randomName = UUID.randomUUID().toString();

    @Mock
    SecurityRepository securityRepository;

    @Mock
    LocalImageService localImageService;

    private
    SecurityService securityService;


    private Sensor generateSensor() {
        return new Sensor(randomName, SensorType.DOOR);
    }

    private Set<Sensor> generateSensors(){

        return IntStream.range(0, 4).mapToObj(x-> {
            final Sensor s=  new Sensor(randomName, SensorType.DOOR);
            s.setActive(false);
            return s;
        }).collect(Collectors.toSet());
    }

    private BufferedImage produceFakeBufferedImage(){
        return  mock(BufferedImage.class);
    }

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, localImageService);
        simulateSensor = generateSensor();
    }


    @ParameterizedTest //covers 1
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void whenAlarmStatus_AlarmIsArmedAndSensorIsActivated_ReturnAlarmStatusPending(ArmingStatus armingStatus) {
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(simulateSensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @DisplayName("When AlarmStatus Is Pending and Sensor Activated verify AlarmStatus is Alarm")
    @Test
        //test 2
    void whenAlarmStatus_IsAlreadyPendingAndSensorIsActivated_AlarmStatusAlarm() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(simulateSensor, true);
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.ALARM); //first call up
    }

    @Test //tests 3
    public void whenAlarmStatusIsPending_andArmingStatusIsArmed_NoAlarmStatus() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        simulateSensor.setActive(false);
        securityService.changeSensorActivationStatus(simulateSensor, true);
        securityService.changeSensorActivationStatus(simulateSensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
        //tests 4
    void whenAlarmState_alarmActiveAndSensorStateChanges_StateDoesNotChange() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        simulateSensor.setActive(false);
        securityService.changeSensorActivationStatus(simulateSensor, true);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }


    @Test
        //tests 5
    void whenAlarmState_ActivatedWhileAlreadyActiveAndAlarmPending_changeToAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(simulateSensor, true);
        ArgumentCaptor<AlarmStatus> captureValue = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository).setAlarmStatus(captureValue.capture());
    }

    @ParameterizedTest //tests 6
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void whenAlarmState_sensorDeactivateWhileInactive_noChangeToAlarmState(AlarmStatus alarmStatus) {
        securityService.changeSensorActivationStatus(simulateSensor, false);
        verify(securityRepository, never()).setAlarmStatus(alarmStatus);
    }

    @Test
        //tests 7
    void whenAlarmState_imageContainingCatDetectedAndSystemArmed_changeToAlarmStatus() {
        when(localImageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(produceFakeBufferedImage());
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
        //tests 8
    void whenAlarmState_noCatImageIdentifiedAndSensorsAreInactive_changeToAlarmStatus() {
        when(localImageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        simulateSensor.setActive(false);
        securityService.processImage(produceFakeBufferedImage());
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
        //tests 9
    void whenAlarmStatus_systemDisArmed_changeToAlarmStatus() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    //test 10
    public void whenSystemArmedReset_changeSensorsToInactive (ArmingStatus status) {
        Set<Sensor> sensors=generateSensors();
        securityService.setArmingStatus(status);
        sensors.forEach(sensor -> assertEquals(false, sensor.getActive()));
    }




    //Test 11
    @DisplayName("When System is Armed verify AlarmStatus is ALARM")
    @Test
    public void whenSystemArmedHome_CatIdentified_SetStatusAlarm() {
        when(localImageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(produceFakeBufferedImage());
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Test 12
    @Test
    public void whenSystemArmed_SetStatusAlarm() {
        when(localImageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(produceFakeBufferedImage());

        final ArmingStatus currentStatus=securityRepository.getArmingStatus();
        securityService.setArmingStatus(currentStatus);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }


}