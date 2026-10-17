package az.buta.common.text;

public final class Slugs {

    private Slugs() {
    }

    public static String from(String text) {
        String slug = SearchKeys.fold(text).replace(' ', '-');
        return slug.length() > 80 ? slug.substring(0, 80) : slug;
    }
}
