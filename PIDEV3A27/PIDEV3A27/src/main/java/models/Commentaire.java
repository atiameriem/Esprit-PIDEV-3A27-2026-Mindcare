package models;

import java.time.LocalDateTime;

public class Commentaire {
    private long id;
    private long idPost;
    private int idUsers;
    private String content;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // joined from users
    private String authorNom;
    private String authorPrenom;

    // computed
    private int likesCount;

    public Commentaire() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getIdPost() { return idPost; }
    public void setIdPost(long idPost) { this.idPost = idPost; }

    public int getIdUsers() { return idUsers; }
    public void setIdUsers(int idUsers) { this.idUsers = idUsers; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getAuthorNom() { return authorNom; }
    public void setAuthorNom(String authorNom) { this.authorNom = authorNom; }

    public String getAuthorPrenom() { return authorPrenom; }
    public void setAuthorPrenom(String authorPrenom) { this.authorPrenom = authorPrenom; }

    public String getAuthorFullName() {
        String n = authorNom == null ? "" : authorNom.trim();
        String p = authorPrenom == null ? "" : authorPrenom.trim();
        String full = (n + " " + p).trim();
        return full.isEmpty() ? ("User #" + idUsers) : full;
    }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
}
