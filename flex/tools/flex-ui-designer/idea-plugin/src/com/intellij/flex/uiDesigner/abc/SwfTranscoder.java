package com.intellij.flex.uiDesigner.abc;

import com.intellij.openapi.util.io.FileUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

abstract class SwfTranscoder extends AbcEncoder {
  private static final ThreadLocal<Inflater> INFLATER = ThreadLocal.withInitial(Inflater::new);

  protected static final int PARTIAL_HEADER_LENGTH = 8;

  private final byte[] partialHeader = new byte[PARTIAL_HEADER_LENGTH];

  protected FileOutputStream readSourceAndCreateFileOut(InputStream inputStream, long inputLength, File outFile) throws IOException {
    readSource(inputStream, inputLength);
    return new FileOutputStream(outFile);
  }

  // in will be closed
  protected void readSource(InputStream in, long inputLength) throws IOException {
    final int uncompressedBodyLength;
    final boolean compressed;
    byte[] data;
    try {
      int n = in.read(partialHeader);
      assert n == PARTIAL_HEADER_LENGTH;
      uncompressedBodyLength = (partialHeader[4] & 0xFF | (partialHeader[5] & 0xFF) << 8 |
                                (partialHeader[6] & 0xFF) << 16 | partialHeader[7] << 24) - PARTIAL_HEADER_LENGTH;
      compressed = partialHeader[0] == 0x43;
      data = FileUtil.loadBytes(in, compressed ? (int)inputLength - PARTIAL_HEADER_LENGTH : uncompressedBodyLength);
    }
    finally {
      in.close();
    }

    if (compressed) {
      final Inflater inflater = INFLATER.get();
      try {
        inflater.setInput(data);
        byte[] uncompressedData = new byte[uncompressedBodyLength];
        try {
          inflater.inflate(uncompressedData);
        }
        catch (DataFormatException e) {
          throw new ZipException(e.getMessage() != null ? e.getMessage() : "Invalid ZLIB data format");
        }
        data = uncompressedData;
      }
      finally {
        inflater.reset();
      }
    }

    buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

    readFrameSizeFrameRateAndFrameCount(data[0]);
  }

  protected void readFrameSizeFrameRateAndFrameCount(byte b) throws IOException {
    // skip FrameSize, FrameRate, FrameCount
    buffer.position((int)Math.ceil((float)(5 + ((b & 0xFF) >> -(5 - 8)) * 4) / 8) + 2 + 2);
  }

  protected void writePartialHeader(int fileLength) {
    partialHeader[0] = 0x46; // write as uncompressed
    buffer.put(partialHeader, 0, 4);
    buffer.putInt(fileLength);
  }

  protected void writePartialHeader(OutputStream out, int fileLength) throws IOException {
    partialHeader[0] = 0x46; // write as uncompressed

    // fileLength int as little endian
    partialHeader[4] = (byte)(0xff & fileLength);
    partialHeader[5] = (byte)(0xff & fileLength >> 8);
    partialHeader[6] = (byte)(0xff & fileLength >> 16);
    partialHeader[7] = (byte)(0xff & fileLength >> 24);

    out.write(partialHeader);
  }

  protected static class TagPositionInfo {
    public final int start;
    public final int end;

    protected TagPositionInfo(int start, int end) {
      this.start = start;
      this.end = end;
    }
    
    public int length() {
      return end - start;
    }
  }

  protected int skipAbcName(final int start) {
    int end = start;
    byte[] array = buffer.array();
    //noinspection StatementWithEmptyBody
    while (array[++end] != 0) {
    }

    return end - start;
  }
}