package com.intellij.flex.uiDesigner.io;

import com.intellij.flex.uiDesigner.LogMessageUtil;
import com.intellij.openapi.components.ServiceManager;
import gnu.trove.TObjectIntProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringRegistry {
  private final TransactionableStringIntHashMap table = new TransactionableStringIntHashMap(1024, 1);
  private StringWriter activeWriter;

  public static StringRegistry getInstance() {
    return ServiceManager.getService(StringRegistry.class);
  }
  
  private void startChange(StringWriter activeWriter) {
    table.startTransaction();
    assert this.activeWriter == null;
    this.activeWriter = activeWriter;
  }
  
  private void rollbackChange(StringWriter requestor) {
    table.rollbackTransaction();
    LogMessageUtil.LOG.assertTrue(activeWriter == null || activeWriter == requestor);
    resetAfterChange(requestor);
  }

  private void commitChange(StringWriter requestor) {
    resetAfterChange(requestor);
  }

  private void resetAfterChange(StringWriter requestor) {
    LogMessageUtil.LOG.assertTrue(activeWriter == null || activeWriter == requestor);
    activeWriter = null;
  }

  public void reset() {
    table.clear();
    assert activeWriter == null;
  }

  public boolean isEmpty() {
    return table.isEmpty();
  }
  
  public String[] toArray() {
    final String[] strings = new String[table.size()];
    table.forEachEntry(new TObjectIntProcedure<String>() {
      @Override
      public boolean execute(String v, int id) {
        strings[id - 1] = v;
        return true;
      }
    });

    return strings;
  }

  private int getNameReference(String string, StringWriter writer) {
    assert activeWriter == writer;

    int reference = table.get(string);
    if (reference == -1) {
      reference = table.size() + 1;
      table.put(string, reference);
      writer.counter++;
      writer.out.writeAmfUtf(string, false);
    }

    return reference;
  }

  public static class StringWriter {
    private final StringRegistry stringRegistry;

    private final PrimitiveAmfOutputStream out;
    private int counter;

    public StringWriter(StringRegistry stringRegistry) {
      this(stringRegistry, 1024);
    }

    public StringWriter() {
      this(StringRegistry.getInstance());
    }

    public StringWriter(int size) {
      this(StringRegistry.getInstance(), size);
    }

    public StringWriter(StringRegistry stringRegistry, int size) {
      this.stringRegistry = stringRegistry;
      out = new PrimitiveAmfOutputStream(new ByteArrayOutputStreamEx(size));
    }

    public void startChange() {
      //LogMessageUtil.LOG.info("startChange", new Exception());
      stringRegistry.startChange(this);
    }
    
    public void rollback() {
      //LogMessageUtil.LOG.info("rollback", new Exception());
      reset();
      stringRegistry.rollbackChange(this);
    }

    public void commit() {
      //LogMessageUtil.LOG.info("commit", new Exception());
      reset();
      stringRegistry.commitChange(this);
    }
    
    private void reset() {
      counter = 0;
      out.reset();
    }

    public int getReference(@NotNull String string) {
      return stringRegistry.getNameReference(string, this);
    }

    public void write(@NotNull String string, @NotNull PrimitiveAmfOutputStream out) {
      out.writeUInt29(getReference(string));
    }

    public boolean writeNullable(@Nullable String string, PrimitiveAmfOutputStream out) {
      if (string == null) {
        out.write(0);
        return false;
      }
      else {
        out.writeUInt29(getReference(string));
        return true;
      }
    }

    public int size() {
      return IOUtil.uint29SizeOf(counter) + out.size();
    }

    public void writeToIfStarted(PrimitiveAmfOutputStream to) {
      if (stringRegistry.activeWriter == null) {
        assert counter == 0;
        to.writeUInt29(0);
        return;
      }
      
      writeTo(to); 
    }

    public void writeTo(PrimitiveAmfOutputStream to) {
      to.writeUInt29(counter);
      out.writeTo(to);

      commit();
    }

    public boolean hasChanges() {
      return counter != 0;
    }
  }
}
