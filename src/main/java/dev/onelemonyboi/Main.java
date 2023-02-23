package dev.onelemonyboi;

import com.fazecast.jSerialComm.SerialPort;
import edu.wpi.first.cscore.CameraServerJNI;
import edu.wpi.first.math.WPIMathJNI;
import edu.wpi.first.networktables.*;
import edu.wpi.first.util.CombinedRuntimeLoader;
import edu.wpi.first.util.WPIUtilJNI;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Main {
    public static void main(String[] args) throws IOException {
        NetworkTablesJNI.Helper.setExtractOnStaticLoad(false);
        WPIUtilJNI.Helper.setExtractOnStaticLoad(false);
        WPIMathJNI.Helper.setExtractOnStaticLoad(false);
        CameraServerJNI.Helper.setExtractOnStaticLoad(false);
        CombinedRuntimeLoader.loadLibraries(Main.class, "wpiutiljni", "wpimathjni", "ntcorejni", "cscorejnicvstatic");
        new Main().run();
    }

    public void run() {
        NetworkTableInstance inst = NetworkTableInstance.getDefault();
        NetworkTable table = inst.getTable("Serial");
        // O - No Item
        // 1 - Cone
        // 2 - Cube
        IntegerSubscriber pickupType = table.getIntegerTopic("PickupType").subscribe(0);
        IntegerPublisher buttonsPressed = table.getIntegerTopic("ButtonsPressed").publish(PubSubOption.sendAll(true));
        inst.startClient4("ControlBoardClient");
        inst.setServer("10.60.59.2");
        inst.startDSClient();

        // Don't continue until Serial Port available
        while (SerialPort.getCommPorts().length == 0) {}

        SerialPort arduinoPort = SerialPort.getCommPorts()[0];
        // Forces read/write to occur before moving on
        arduinoPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);
        arduinoPort.openPort();

        while (true) {
            if (arduinoPort.bytesAwaitingWrite() < Long.BYTES) continue;

            // Send to Arduino
            ByteBuffer bufferSend = ByteBuffer.allocate(Long.BYTES);
            bufferSend.putLong(pickupType.get());
            arduinoPort.writeBytes(bufferSend.array(), Long.BYTES);

            // Receive from Arduino
            byte[] bufferGet = new byte[Long.BYTES];
            arduinoPort.readBytes(bufferGet, Long.BYTES);
            buttonsPressed.accept(ByteBuffer.wrap(bufferGet).getLong());

            while (arduinoPort.flushIOBuffers()) {}
        }
    }
}