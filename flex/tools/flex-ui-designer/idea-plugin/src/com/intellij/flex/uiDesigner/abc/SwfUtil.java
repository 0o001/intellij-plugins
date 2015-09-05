package com.intellij.flex.uiDesigner.abc;

import com.intellij.flex.uiDesigner.io.AbstractByteArrayOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public final class SwfUtil {
  // FWS, Version 11
  private static final byte[] SWF_HEADER_P1 = {0x46, 0x57, 0x53, 0x0b};
  private static final byte[] SWF_HEADER_P2 = {0x78, 0x00, 0x05, 0x5f, 0x00, 0x00, 0x0f, (byte)0xa0, 0x00,
      // size [Rect 0 0 8000 6000]
      0x00, 0x0c, 0x01, 0x00, // 16bit le frame rate 12, 16bit be frame count 1
      0x44, 0x11, // Tag type=69 (FileAttributes), length=4
      0x08, 0x00, 0x00, 0x00};

  private static final byte[] SWF_FOOTER = {0x40, 0x00, 0x00, 0x00};

  public static int getWrapLength() {
    return getWrapHeaderLength() + SWF_FOOTER.length;
  }

  public static int getWrapFooterLength() {
    return SWF_FOOTER.length;
  }

  public static int getWrapHeaderLength() {
    return SWF_HEADER_P1.length + 4 + SWF_HEADER_P2.length;
  }

  public static void header(int length, OutputStream out) throws IOException {
    out.write(SWF_HEADER_P1);

    // write length, littleEndian
    out.write(0xFF & length);
    out.write(0xFF & (length >> 8));
    out.write(0xFF & (length >> 16));
    out.write(0xFF & (length >> 24));

    out.write(SWF_HEADER_P2);
  }

  public static void header(FileChannel channel, ByteBuffer buffer) throws IOException {
    buffer.clear();
    buffer.put(SWF_HEADER_P1);
    buffer.putInt((int)channel.position());
    buffer.put(SWF_HEADER_P2);
    buffer.flip();
    channel.write(buffer, 0);
  }

  public static void header(int length, AbstractByteArrayOutputStream out, ByteBuffer buffer, int position) throws IOException {
    buffer.clear();
    buffer.put(SWF_HEADER_P1);
    buffer.putInt(length);
    buffer.put(SWF_HEADER_P2);
    buffer.flip();
    out.write(buffer, position);
  }

  public static void footer(OutputStream out) throws IOException {
    out.write(SWF_FOOTER);
  }

  public static void footer(ByteBuffer byteBuffer) {
    byteBuffer.put(SWF_FOOTER);
  }

  public static Encoder mergeDoAbc(List<Decoder> decoders) {
    final Encoder encoder = new Encoder();
    encoder.configure(decoders, null);
    mergeDoAbc(decoders, encoder);
    return encoder;
  }

  public static void mergeDoAbc(List<Decoder> decoders, Encoder encoder) {
    //final long time = System.currentTimeMillis();

    encoder.enablePeepHole();
    for (int i = 0, n = decoders.size(); i < n; i++) {
      Decoder decoder = decoders.get(i);
      if (decoder == null) {
        continue;
      }

      encoder.useDecoder(i, decoder);
      decoder.methodInfo.decodeAll(encoder, decoder.in);
      decoder.metadataInfo.decodeAll(encoder, decoder.in);
      decoder.classInfo.decodeInstances(encoder, decoder.in);
      decoder.classInfo.decodeClasses(encoder, decoder.in);
      decoder.scriptInfo.decodeAll(encoder, decoder.in);
      decoder.methodBodies.decodeAll(encoder, decoder.in);

      encoder.endDecoder(decoder);
    }

    //final long l = System.currentTimeMillis() - time;
    //System.out.print("\nmerge: ");
    //System.out.print(l);
  }
}