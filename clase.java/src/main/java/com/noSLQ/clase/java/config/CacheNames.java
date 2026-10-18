package com.noSLQ.clase.java.config;

/**
 * Nombres logicos de las caches almacenadas en Redis. Se centralizan aqui para
 * reutilizarlos en las anotaciones @Cacheable / @CacheEvict de los servicios.
 */
public final class CacheNames {

    private CacheNames() {
    }

    public static final String BOOKS = "books";
    public static final String BOOKS_AVAILABLE = "books-available";
    public static final String BOOKS_BORROWED = "books-borrowed";
    public static final String BOOKS_BEST_RATED = "books-best-rated";
    public static final String BOOKS_POPULAR = "books-popular";
    public static final String BOOK_DETAIL = "book-detail";
    public static final String BOOKS_BY_CATEGORY = "books-by-category";
    public static final String BOOKS_BY_AUTHOR = "books-by-author";

    public static final String REVIEWS = "reviews";
    public static final String LOANS = "loans";
    public static final String USERS = "users";
    public static final String FAVORITES = "favorites";
    public static final String AUTHORS = "authors";
    public static final String GENRES = "genres";
    public static final String EDITORIALS = "editorials";

    public static final String[] ALL = {
            BOOKS, BOOKS_AVAILABLE, BOOKS_BORROWED, BOOKS_BEST_RATED, BOOKS_POPULAR,
            BOOK_DETAIL, BOOKS_BY_CATEGORY, BOOKS_BY_AUTHOR,
            REVIEWS, LOANS, USERS, FAVORITES, AUTHORS, GENRES, EDITORIALS
    };
}
