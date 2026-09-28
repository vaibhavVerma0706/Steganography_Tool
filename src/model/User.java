package model;

public class User {

    private final int    id;
    private final String username;
    private final String createdAt;

    // ── Constructor ──
    public User(int id, String username, String createdAt) {
        this.id        = id;
        this.username  = username;
        this.createdAt = createdAt;
    }

    // ── Getters ──
    public int    getId()        { return id;        }
    public String getUsername()  { return username;  }
    public String getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "'}";
    }
}