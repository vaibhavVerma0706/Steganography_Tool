package model;

public class HistoryRecord {

    private final int    id;
    private final int    userId;
    private final String action;      // "ENCODE" or "DECODE"
    private final String imageName;
    private final long   imageSize;
    private final String hiddenType;  // "TEXT" or "FILE"
    private final String hiddenName;  // filename if file was hidden
    private final String timestamp;

    // ── Constructor ──
    public HistoryRecord(int id, int userId, String action,
                         String imageName, long imageSize,
                         String hiddenType, String hiddenName,
                         String timestamp) {
        this.id         = id;
        this.userId     = userId;
        this.action     = action;
        this.imageName  = imageName;
        this.imageSize  = imageSize;
        this.hiddenType = hiddenType;
        this.hiddenName = hiddenName;
        this.timestamp  = timestamp;
    }

    // ── Getters ──
    public int    getId()         { return id;         }
    public int    getUserId()     { return userId;     }
    public String getAction()     { return action;     }
    public String getImageName()  { return imageName;  }
    public long   getImageSize()  { return imageSize;  }
    public String getHiddenType() { return hiddenType; }
    public String getHiddenName() { return hiddenName != null ? hiddenName : ""; }
    public String getTimestamp()  { return timestamp;  }
}