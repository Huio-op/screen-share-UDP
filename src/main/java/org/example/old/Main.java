package org.example.old;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;

import static java.net.InetAddress.getByName;

public class Main {

  final static int X = 1090;
  final static int Y = 1920;
  final static int FRAMES_X = 10;
  final static int FRAMES_Y = 10;
  final static Robot r;
  static int PORT = 5000;
  static InetAddress ADDRESS;

  static {
    try {
      ADDRESS = getByName("localhost");
//      ADDRESS = getByName("177.44.240.246");
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  static {
    try {
      r = new Robot();
    } catch (AWTException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) throws IOException {

    while (true) {
      BufferedImage bf = r.createScreenCapture(new Rectangle(0, 0, X, Y));
      for(int y = 0; y < (Y/FRAMES_Y);y++) {
        for(int x = 0; x < (X/FRAMES_X);x++) {
          BufferedImage bfs = bf.getSubimage(x, y, X/FRAMES_X, Y/FRAMES_Y);
//          byte[] bytes = toByteArray(bfs);
          sendImage(bfs, x, y);
        }
      }
    }
  }

  public static void sendImage(BufferedImage bi, int x, int y) throws IOException {

    byte[] imgBytes = toByteArray(bi);
    byte[] message = new byte[imgBytes.length + 2];

    int count = 0;
    message[count++] = (byte) x;
    message[count++] = (byte) y;

    for (int i = 0; i < imgBytes.length; i++) {
      message[count++] = imgBytes[i];
    }

    sendImageBytes(message);
  }


  public static void sendImageByColor(BufferedImage bi, int x, int y) throws IOException {

    byte[] message = new byte[bi.getHeight() * bi.getWidth() + 2];

    int count = 0;
    message[count++] = (byte) x;
    message[count++] = (byte) y;

    for (int i = 0; i < bi.getHeight(); i++) {
      for (int j = 0; j < bi.getWidth(); j++) {
        int rgb = bi.getRGB(j, i);
        message[count++] = (byte) rgb;
      }
    }


    sendImageBytes(message);
  }

  public static void sendImageBytes(byte[] bytes) throws IOException {
    DatagramSocket socket = new DatagramSocket();

    DatagramPacket packet = new DatagramPacket(bytes, bytes.length, ADDRESS, PORT);
    socket.send(packet);

    socket.close();
  }

  public static byte[] toByteArray(BufferedImage bi) throws IOException {

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(bi, "jpg", baos);
    byte[] bytes = baos.toByteArray();
    return bytes;

  }

}