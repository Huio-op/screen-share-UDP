package org.example;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Random;

import static java.net.InetAddress.getByName;

public class SendImage {

  static int MAX_DATAGRAM_SIZE = 65507;
  static int PORT = 8080;
  static int MOUSE_EVENT_PORT = 5001;
  static int AUDIO_PORT = 5555;
  static InetAddress ADDRESS;
  static int BYTE_OVERHEAD = 3;

  static int SCREEN_WIDTH = 1920;
  static int SCREEN_HEIGHT = 1080;
  static Robot robot;


  static {
    try {
      robot = new Robot();
      ADDRESS = getByName("localhost");
    } catch (UnknownHostException | AWTException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {

    new Thread(() -> {
      try {
        receiveMouseEvent();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).start();

    new Thread(() -> {
      try {
        sendImageEvent();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).start();

    new Thread(() -> {
      try {
        sendAudioEvent();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).start();

  }

  public static void sendImageEvent() throws SocketException {
    Random rand = new Random();
    DatagramSocket socket = new DatagramSocket();

    try {
      while (true) {
        BufferedImage bi = robot.createScreenCapture(new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT));

        byte[] imageData = imageToBytes(bi);

        byte[] idArr = new byte[1];
        rand.nextBytes(idArr);
        byte id = idArr[0];


        int numPackets = (int) Math.ceil((double) (imageData.length + BYTE_OVERHEAD) / MAX_DATAGRAM_SIZE);
        for (int i = 0; i < numPackets; i++) {
          int offset = i * (MAX_DATAGRAM_SIZE - BYTE_OVERHEAD);
          int length = Math.min(imageData.length - offset, MAX_DATAGRAM_SIZE);
          byte[] packetData = new byte[length];
          packetData[0] = (byte) i;
          packetData[1] = (byte) (numPackets - 1);
          packetData[2] = id;
          System.arraycopy(imageData, offset, packetData, BYTE_OVERHEAD, length - BYTE_OVERHEAD);
          DatagramPacket packet = new DatagramPacket(packetData, packetData.length, ADDRESS, PORT);
          socket.send(packet);
          System.out.println(id + ": Enviado pacote " + (i + 1) + " de " + numPackets + " contendo: " + packet.getLength() + " bytes");
          Thread.sleep(100);
        }

        System.out.println("Screenshot capturado e enviado com sucesso!");

        Thread.sleep(10);
      }
      //      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static void sendAudioEvent() {
    AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, true);
    TargetDataLine microphone;
    SourceDataLine speakers;
    try {
      microphone = AudioSystem.getTargetDataLine(format);

      DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
      microphone = (TargetDataLine) AudioSystem.getLine(info);
      microphone.open(format);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      int numBytesRead;
      int CHUNK_SIZE = 1024;
      byte[] data = new byte[microphone.getBufferSize() / 5];
      microphone.start();


      DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
      speakers = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
      speakers.open(format);
      speakers.start();

      DatagramSocket socket = new DatagramSocket();
      for(;;) {
        numBytesRead = microphone.read(data, 0, CHUNK_SIZE);
        //  bytesRead += numBytesRead;
        // write the mic data to a stream for use later
        out.write(data, 0, numBytesRead);
        // write mic data to stream for immediate playback
//        speakers.write(data, 0, numBytesRead);
        DatagramPacket request = new DatagramPacket(data,numBytesRead, ADDRESS, AUDIO_PORT);
        socket.send(request);

      }

    } catch (LineUnavailableException e) {
      e.printStackTrace();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void receiveMouseEvent() throws Exception {
    DatagramSocket socket = new DatagramSocket(MOUSE_EVENT_PORT);
    byte[] buffer = new byte[MAX_DATAGRAM_SIZE];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    while (true) {
      socket.receive(packet);
      if (packet.getLength() == 0) {
        return;
      }
      final ByteArrayInputStream byteIn = new ByteArrayInputStream(buffer);
      final DataInputStream dataIn = new DataInputStream(byteIn);
      final int x = dataIn.readInt();
      final int y = dataIn.readInt();

      System.out.println("Recebeu evento do mouse! X=" + x + " Y=" + y);

      robot.mouseMove(x, y);
      robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      Thread.sleep(15);
      robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

    }
  }

  public static byte[] imageToBytes(BufferedImage bi) throws IOException {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(bi, "jpg", baos);
    return baos.toByteArray();

  }

}