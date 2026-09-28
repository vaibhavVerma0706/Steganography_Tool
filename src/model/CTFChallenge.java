package model;

public class CTFChallenge {

    private final int    id;
    private final int    userId;
    private final String name;
    private final String imageName;
    private String status;      // "UNSOLVED" / "IN PROGRESS" / "SOLVED"
    private String flagFound;
    private String notes;
    private final String createdAt;
    private String solvedAt;

    // ── Constructor ──
    public CTFChallenge(int id, int userId, String name,
                        String imageName, String status,
                        String flagFound, String notes,
                        String createdAt, String solvedAt) {
        this.id        = id;
        this.userId    = userId;
        this.name      = name;
        this.imageName = imageName != null ? imageName : "";
        this.status    = status    != null ? status    : "UNSOLVED";
        this.flagFound = flagFound != null ? flagFound : "";
        this.notes     = notes     != null ? notes     : "";
        this.createdAt = createdAt != null ? createdAt : "";
        this.solvedAt  = solvedAt  != null ? solvedAt  : "";
    }

    // ── Getters ──
    public int    getId()        { return id;        }
    public int    getUserId()    { return userId;    }
    public String getName()      { return name;      }
    public String getImageName() { return imageName; }
    public String getStatus()    { return status;    }
    public String getFlagFound() { return flagFound; }
    public String getNotes()     { return notes;     }
    public String getCreatedAt() { return createdAt; }
    public String getSolvedAt()  { return solvedAt;  }

    // ── Setters ──
    public void setStatus(String status)     { this.status    = status;    }
    public void setFlagFound(String flag)    { this.flagFound = flag != null ? flag : ""; }
    public void setNotes(String notes)       { this.notes     = notes != null ? notes : ""; }
    public void setSolvedAt(String solvedAt) { this.solvedAt  = solvedAt != null ? solvedAt : ""; }

    @Override
    public String toString() { return name + " [" + status + "]"; }
}