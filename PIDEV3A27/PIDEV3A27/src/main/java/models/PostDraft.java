package models;

/**
 * Résultat simple du chatbot pour aider à rédiger un post.
 */
public class PostDraft {
    private final String title;
    private final String content;

    public PostDraft(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }
}
