package org.example.old;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;

import static java.net.InetAddress.getByName;

public class RecieveScreen {

  static int PORT = 5000;
  static InetAddress ADDRESS;

  static {
    try {
      ADDRESS = getByName("localhost");
//      ADDRESS = getByName("10.3.20.19");
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) throws IOException {

    BufferedImage bi = null;
    Screen screen = new Screen();
    screen.setVisible(true);
    while (true) {
      DatagramSocket socket = new DatagramSocket(PORT);

      byte[] resp = new byte[65507];
      DatagramPacket resPacket = new DatagramPacket(resp, resp.length);
      socket.receive(resPacket);

      byte[] data = resPacket.getData();

      System.out.println(resPacket);

      int x = data[0];
      int y = data[1];

      byte[] imgBytes = new byte[50000];
      for (int i = 0; i < 50000; i++) {
        imgBytes[i] = data[i + 2];
      }

      bi = toBufferedImage(imgBytes);
      if (bi != null) {
        screen.addImage(bi, x,y);
        screen.repaint();
      }
      socket.close();
    }

  }

  public static BufferedImage toBufferedImage(byte[] bytes)
    throws IOException {

    InputStream is = new ByteArrayInputStream(bytes);
    BufferedImage bi = ImageIO.read(is);
    return bi;

  }

}
