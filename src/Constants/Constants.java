package Constants;
public class Constants {
    public enum PageType {
        INDEX_INTERIOR(0x02), TABLE_INTERIOR(0x05), TABLE_LEAF(0x0A), INDEX_LEAF(0x0D), EMPTY(0x00), INVALID(0xFF);
        private final int value;
        PageType(int value) { this.value = value; }
        public int getValue() { return value; }
        public static PageType fromValue(int value) {
            for (PageType pageType : PageType.values()) {
                if (pageType.getValue() == value) {
                    return pageType;
                }
            }
            return INVALID;
        }
    }
    public enum FileType { TABLE, INDEX }
    public enum DataTypes {
        NULL, TINYINT, SMALLINT, INT, BIGINT, FLOAT, DOUBLE, UNUSED, YEAR, TIME, DATETIME, DATE, TEXT
    }

    public static final int PAGE_SIZE = 512;

}
