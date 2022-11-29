package MaxwellBase;

import Constants.Constants;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Random;

public class TableFile extends DatabaseFile{
    String tableName;
    public TableFile(String name, String mode) throws IOException {
        super(name, mode);
        this.tableName = name;
        this.fileType = Constants.FileType.TABLE;
        createPage(0xFFFFFFFF);
    }

    public static void main(String[] args) {
        try (TableFile tableFile = new TableFile("test.tbl", "rw")) {
            tableFile.seek(0);
            System.out.println("Page type: " + tableFile.readByte());
            tableFile.readByte();
            System.out.println("Number of records: " + tableFile.readShort());
            System.out.println("Start of content: " + tableFile.readShort());
            System.out.println("Right page pointer: " + tableFile.readInt());
            System.out.println("Parent page pointer: " + tableFile.readInt());
            ArrayList<Constants.DataTypes> testcol = new ArrayList<>();
            testcol.add(Constants.DataTypes.INT);
            testcol.add(Constants.DataTypes.TEXT);
            testcol.add(Constants.DataTypes.DOUBLE);
            for (int i = 0; i < 10; i++) {
                Random rand = new Random();
                ArrayList<Object> testrow = new ArrayList<>();
                System.out.println("Inserting row " + i);
                int col1 = rand.nextInt(100);
                System.out.println("\tcol1: " + col1);
                String col2 = "test" + (char)(rand.nextInt(26) + 'a');
                System.out.println("\tcol2: " + col2);
                double col3 = rand.nextDouble();
                System.out.println("\tcol3: " + col3);
                testrow.add(col1);
                testrow.add(col2);
                testrow.add(col3);
                Record record = new Record(testcol, testrow, i);
                System.out.println("\tRecord size: " + record.getRecordSize());
                tableFile.writeRecord(record, 0);
            }
            int numRecords = tableFile.getNumberOfCells(0);
            for (int i = 0; i < numRecords; i++) {
                tableFile.seek(i * 2 + 0x10);
                int offset = tableFile.readShort();
                Record record = tableFile.readRecord(0, offset);
                System.out.println(record);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public int getMaxRowId(int page) throws IOException {
        if (getNumberOfCells(page) <= 0){
            throw new IOException("Page is empty");
        }
        short contentStart = getContentStart(page);
        this.seek((long) page * pageSize + contentStart);
        return this.readInt();
    }

    public void SplitPage(int pageNumber) throws IOException {
        int parentPage = getParentPage(pageNumber);
        int newPage = createPage(parentPage);
        int numRecords = getNumberOfCells(pageNumber);
    }

    public void writePagePointer(int page, int pointer) throws IOException {
        short cellSize = 8;
        int rowId = getMaxRowId(pointer);
        short contentStart = setContentStart(page, cellSize);
        if (contentStart + cellSize > pageSize) {
            SplitPage(page);
        }
        this.seek((long) page * pageSize + contentStart);
        this.writeInt(rowId);
        this.writeInt(pointer);
    }

    public void writeRecord(Record record, int page) throws IOException {

        short recordSize = record.getRecordSize();
        short cellSize = (short) (recordSize + 6);
        short contentStart = this.setContentStart(page, cellSize);
        if (contentStart + cellSize > pageSize) {
            SplitPage(page);
        }
        short numberOfCells = incrementNumberOfCells(page);
        this.seek((long) pageSize * page + 0x0E + 2 * numberOfCells);
        this.writeShort(contentStart);
        this.seek(contentStart);
        ArrayList<Constants.DataTypes> columns = record.getColumns();
        ArrayList<Object> values = record.getValues();
        this.writeShort(recordSize);
        this.writeInt(record.getRowId());
        byte[] header = record.getHeader();
        this.write(header);
        for (int i = 0; i < columns.size(); i++){
            switch (columns.get(i)) {
                case TINYINT, YEAR -> this.writeByte((byte) values.get(i));
                case SMALLINT -> this.writeShort((short) values.get(i));
                case INT, TIME -> this.writeInt((int) values.get(i));
                case BIGINT, DATE, DATETIME -> this.writeLong((long) values.get(i));
                case FLOAT -> this.writeFloat((float) values.get(i));
                case DOUBLE -> this.writeDouble((double) values.get(i));
                case TEXT -> this.writeBytes((String) values.get(i));
            }
        }
    }

    private Record readRecord(int page, int offset) throws IOException {
        this.seek((long) page * pageSize + offset);
        short recordSize = this.readShort();
        int rowId = this.readInt();
        byte numColumns = this.readByte();
        byte[] columns = new byte[numColumns];
        for (int i = 0; i < numColumns; i++) {
            columns[i] = this.readByte();
        }
        ArrayList<Object> values = new ArrayList<>();
        ArrayList<Constants.DataTypes> columnTypes = new ArrayList<>();
        for (byte b : columns) {
            Constants.DataTypes dataType;
            if (b > 0x0C){
                dataType = Constants.DataTypes.TEXT;
            }
            else {
                dataType = Constants.DataTypes.values()[b];
            }
            columnTypes.add(dataType);
            switch (dataType) {
                case TINYINT, YEAR -> values.add(this.readByte());
                case SMALLINT -> values.add(this.readShort());
                case INT, TIME -> values.add(this.readInt());
                case BIGINT, DATE, DATETIME -> values.add(this.readLong());
                case FLOAT -> values.add(this.readFloat());
                case DOUBLE -> values.add(this.readDouble());
                case TEXT -> {
                    int textLength = b - 0x0C;
                    byte[] text = new byte[textLength];
                    this.readFully(text);
                    values.add(new String(text));
                }
            }
        }
        return new Record(columnTypes, values, rowId);
    }
}
