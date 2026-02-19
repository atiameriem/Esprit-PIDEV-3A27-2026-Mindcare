package models;

import java.time.LocalDateTime;

public class Post {
    private long id;
    private int idUsers;
    private String title;
    private String content;
    private String status;
    private String language;
    private int viewsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // computed / joined
    private String firstImagePath;
    private int likesCount;
    private int commentsCount;

    public Post() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getIdUsers() { return idUsers; }
    public void setIdUsers(int idUsers) { this.idUsers = idUsers; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public int getViewsCount() { return viewsCount; }
    public void setViewsCount(int viewsCount) { this.viewsCount = viewsCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getFirstImagePath() { return firstImagePath; }
    public void setFirstImagePath(String firstImagePath) { this.firstImagePath = firstImagePath; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }
}
