package social;

import java.util.List;
import java.util.stream.Collectors;

public final class JsonVisitor implements Visitor<String> {
    private String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                default -> {
                    if (character < 32) {
                        result.append(String.format("\\u%04x", (int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        return result.append('"').toString();
    }

    private String container(String type, String id, List<? extends Element> children) {
        String nested = children.stream().map(child -> child.accept(this))
                .collect(Collectors.joining(","));
        return "{\"type\":" + quote(type) + ",\"id\":" + quote(id)
                + ",\"children\":[" + nested + "]}";
    }

    private String contentFields(String type, String id, String text, int likes) {
        return "\"type\":" + quote(type) + ",\"id\":" + quote(id)
                + ",\"text\":" + quote(text) + ",\"likes\":" + likes;
    }

    @Override
    public String visit(Community community) {
        return container("community", community.id(), community.children());
    }

    @Override
    public String visit(Group group) {
        return container("group", group.id(), group.children());
    }

    @Override
    public String visit(TextPost post) {
        return "{" + contentFields("post", post.id(), post.text(), post.likes()) + "}";
    }

    @Override
    public String visit(Comment comment) {
        return "{" + contentFields("comment", comment.id(), comment.text(), comment.likes()) + "}";
    }

    @Override
    public String visit(Image image) {
        return "{" + contentFields("image", image.id(), image.caption(), image.likes()) + "}";
    }

    @Override
    public String visit(Video video) {
        return "{" + contentFields("video", video.id(), video.caption(), video.likes())
                + ",\"durationSeconds\":" + video.durationSeconds() + "}";
    }
}
