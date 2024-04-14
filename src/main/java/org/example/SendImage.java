package org.example;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import javax.imageio.ImageIO;

import static java.net.InetAddress.getByName;

public class SendImage {

  static int MAX_DATAGRAM_SIZE = 65507;
  static int PORT = 5000;
  static InetAddress ADDRESS;
  static int BYTE_OVERHEAD = 3;

  static int SCREEN_WIDTH = 1920;
  static int SCREEN_HEIGHT = 1080;

  static {
    try {
      ADDRESS = getByName("localhost");
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    try {
      Random rand = new Random();
      Robot robot = new Robot();
      while (true) {
        BufferedImage bi = robot.createScreenCapture(new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT));

        byte[] imageData = imageToBytes(bi);

        byte[] idArr = new byte[1];
        rand.nextBytes(idArr);
        byte id = idArr[0];

        DatagramSocket socket = new DatagramSocket();

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
        }

        socket.close();

        System.out.println("Screenshot capturado e enviado com sucesso!");
        Thread.sleep(200);
      }
    } catch (AWTException | IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static byte[] imageToBytes(BufferedImage bi) throws IOException {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(bi, "jpg", baos);
    return baos.toByteArray();

  }

}