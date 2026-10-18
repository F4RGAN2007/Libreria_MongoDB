package com.noSLQ.clase.java.tools;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


public final class DenormalizationSync {

    private static final List<String> COLLECTIONS =
            List.of("book", "author", "editorial", "genre", "user", "loan", "review", "favorite");


    private static final String ACTIVE = "ACTIVE";
    private static final String RETURNED = "RETURNED";


    private static final int MAX_RECENT_LOANS = 5;


    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int PREVIEW_LINES = 15;
    private static final int BATCH_SIZE = 500;


    private static final Map<String, String> LEGACY_FIELDS = buildLegacyFields();
    private static final String TOTAL_RESENAS_N = "totalRese\u00F1as"; // totalResenas con enie

    private DenormalizationSync() {
    }

    private static Map<String, String> buildLegacyFields() {
        Map<String, String> m = new HashMap<>();
        m.put("tittle", "title");
        m.put("titulo", "title");
        m.put("bookTittle", "title");
        m.put("bookTitulo", "title");
        m.put("sinopsis", "synopsis");
        m.put("descripcion", "synopsis");
        m.put("autor", "author");
        m.put("nombre", "name");
        m.put("telefono", "phone");
        m.put("direccion", "address");
        m.put("usuarioId", "userId");
        m.put("libroId", "bookId");
        m.put("prestamosId", "loanId");
        m.put("fechaPrestamo", "loanDate");
        m.put("fechaDevolucionEsperada", "expectedReturnDate");
        m.put("fechaDevolucionReal", "actualReturnDate");
        m.put("estado", "status");
        m.put("calificacion", "rating");
        m.put("valoracion", "rating");
        m.put("comentario", "comment");
        m.put("comentarios", "comment");
        m.put("disponible", "available");
        m.put("prestado", "borrowed");
        m.put("promedioCalificaciones", "averageRating");
        m.put("totalResenas", "totalReviews");
        m.put(TOTAL_RESENAS_N, "totalReviews");
        m.put("fechaAgregado", "addedAt");
        m.put("nombreUsuario", "userName");
        m.put("libros", "books");
        m.put("anioPublicacion", "publishYear");
        m.put("a\u00F1oPublicacion", "publishYear");
        m.put("anio", "publishYear");
        m.put("nacionalidad", "nationality");
        m.put("biografia", "biography");
        m.put("pais", "country");
        m.put("paginaWeb", "website");
        m.put("sitioWeb", "website");
        m.put("correo", "email");
        m.put("correoElectronico", "email");
        return m;
    }

    // =====================================================================
    // MAIN
    // =====================================================================
    public static void main(String[] args) throws Exception {
        Set<String> flags = new HashSet<>(List.of(args));
        boolean dryRun = flags.contains("--dry-run");
        boolean autoYes = flags.contains("--yes");
        boolean verbose = flags.contains("--verbose");

        String[] conn = loadConnection();
        System.out.println("Base de datos: " + conn[1] + "  (solo MongoDB, Redis no se toca)");

        try (MongoClient client = MongoClients.create(conn[0])) {
            MongoDatabase db = client.getDatabase(conn[1]);

            System.out.println("Escaneando colecciones...");
            Plan plan = analyze(load(db));
            printReport(plan, verbose);

            if (plan.docCount() == 0) {
                System.out.println("\nNo hay datos desactualizados. Nada que actualizar.");
                return;
            }
            if (dryRun) {
                System.out.println("\n--dry-run: no se modifico nada.");
                return;
            }

            Set<String> approved = autoYes ? new LinkedHashSet<>(plan.fixes.keySet()) : askApproval(plan);
            if (approved.isEmpty()) {
                System.out.println("Cancelado. No se modifico nada.");
                return;
            }

            // Se vuelve a escanear justo antes de escribir para trabajar con datos frescos.
            Plan fresh = analyze(load(db));
            int updated = apply(db, fresh, approved);
            System.out.println("\nDocumentos actualizados: " + updated);

            Plan after = analyze(load(db));
            System.out.println("Documentos que siguen desactualizados: " + after.docCount()
                    + (after.docCount() == 0 ? "  (todo sincronizado)" : "  (colecciones no aprobadas o cambios recientes)"));
        }
    }

    // =====================================================================
    // ANALISIS (migracion + desnormalizacion, logica pura sobre los documentos)
    // =====================================================================
    static Plan analyze(Map<String, List<Document>> original) {
        Plan plan = new Plan();

        Map<String, List<Document>> data = new LinkedHashMap<>();
        for (String c : COLLECTIONS) {
            List<Document> canonical = new ArrayList<>();
            for (Document doc : original.getOrDefault(c, List.of())) {
                canonical.add(canonicalizeRoot(c, doc));
            }
            data.put(c, canonical);
            plan.scanned.put(c, canonical.size());
        }

        syncDenormalized(data, plan);

        for (String c : COLLECTIONS) {
            List<Document> before = original.getOrDefault(c, List.of());
            List<Document> after = data.get(c);
            for (int i = 0; i < after.size(); i++) {
                Document updated = after.get(i);
                Document old = before.size() > i ? before.get(i) : null;
                if (old == null || !old.equals(updated)) {
                    plan.add(c, updated.get("_id"), updated, old == null ? List.of() : diff(old, updated));
                }
            }
        }
        return plan;
    }

    // ---------------------------------------------------------------------
    // Fase 1: migracion de campos heredados al esquema canonico en ingles
    // ---------------------------------------------------------------------

    private static Document canonicalize(Document in) {
        Document out = new Document();
        for (Map.Entry<String, Object> entry : in.entrySet()) {
            String canonical = LEGACY_FIELDS.getOrDefault(entry.getKey(), entry.getKey());
            Object value = canonicalizeValue(entry.getValue());
            if (out.containsKey(canonical)) {
                Object existing = out.get(canonical);
                if (isEmpty(existing) && !isEmpty(value)) {
                    out.put(canonical, value);
                }
            } else {
                out.put(canonical, value);
            }
        }

        // user.ultimosPrestamos es el duplicado en espanol de user.recentActivity.lastLoans.
        if (out.containsKey("ultimosPrestamos")) {
            Object legacyLoans = out.remove("ultimosPrestamos");
            if (legacyLoans instanceof List<?> loans && !loans.isEmpty()) {
                Document activity = asDoc(out.get("recentActivity"));
                if (activity == null) {
                    activity = new Document();
                    out.put("recentActivity", activity);
                }
                if (isEmpty(activity.get("lastLoans"))) {
                    activity.put("lastLoans", new ArrayList<Object>(loans));
                }
            }
        }
        return out;
    }

    private static Document canonicalizeRoot(String collection, Document in) {
        Document doc = canonicalize(in);
        switch (collection) {
            case "review" -> promote(doc, List.of("fecha", "fechaCreacion"), "createdAt");
            case "user" -> promote(doc, List.of("fecha", "fechaCreacion", "fechaRegistro"), "registrationDate");
            case "genre" -> {

                promote(doc, List.of("synopsis"), "description");
            }
            default -> {

            }
        }
        return doc;
    }


    private static void promote(Document doc, List<String> legacyFields, String canonical) {
        for (String legacy : legacyFields) {
            if (!doc.containsKey(legacy)) {
                continue;
            }
            Object value = doc.remove(legacy);
            if (isEmpty(doc.get(canonical)) && !isEmpty(value)) {
                doc.put(canonical, value);
            }
        }
    }

    private static Object canonicalizeValue(Object value) {
        if (value instanceof Document d) {
            return canonicalize(d);
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(canonicalizeValue(item));
            }
            return out;
        }
        return value;
    }

    // ---------------------------------------------------------------------
    // Fase 2: desnormalizacion (copias embebidas y campos derivados)
    // ---------------------------------------------------------------------
    private static void syncDenormalized(Map<String, List<Document>> data, Plan plan) {
        List<Document> books = data.getOrDefault("book", List.of());
        List<Document> users = data.getOrDefault("user", List.of());
        List<Document> loans = data.getOrDefault("loan", List.of());
        List<Document> reviews = data.getOrDefault("review", List.of());
        List<Document> favorites = data.getOrDefault("favorite", List.of());

        Map<String, Document> bookById = indexById(books);
        Map<String, Document> userById = indexById(users);
        Map<String, Document> loanById = indexById(loans);

        Map<String, List<Document>> loansByBook = new LinkedHashMap<>();
        Map<String, List<Document>> loansByUser = new LinkedHashMap<>();
        for (Document l : loans) {
            String bookId = str(l.get("bookId"));
            if (bookId != null) {
                loansByBook.computeIfAbsent(bookId, k -> new ArrayList<>()).add(l);
            }
            String userId = str(l.get("userId"));
            if (userId != null) {
                loansByUser.computeIfAbsent(userId, k -> new ArrayList<>()).add(l);
            }
        }
        Map<String, List<Document>> reviewsByBook = new LinkedHashMap<>();
        for (Document r : reviews) {
            String bookId = str(r.get("bookId"));
            if (bookId != null) {
                reviewsByBook.computeIfAbsent(bookId, k -> new ArrayList<>()).add(r);
            }
        }

        Set<String> authorNames = new HashSet<>();
        for (Document a : data.getOrDefault("author", List.of())) {
            authorNames.add(norm(str(a.get("name"))));
        }
        Set<String> genreNames = new HashSet<>();
        for (Document g : data.getOrDefault("genre", List.of())) {
            genreNames.add(norm(str(g.get("name"))));
        }

        // ---------------- BOOK ----------------
        for (Document b : books) {
            List<Document> bookLoans = loansByBook.getOrDefault(idStr(b), List.of());
            boolean borrowed = bookLoans.stream().anyMatch(l -> ACTIVE.equalsIgnoreCase(str(l.get("status"))));
            setValue(b, "borrowed", borrowed);
            setValue(b, "available", !borrowed);

            List<Document> bookReviews = reviewsByBook.getOrDefault(idStr(b), List.of());
            double sum = 0;
            for (Document r : bookReviews) {
                Object v = r.get("rating");
                sum += v instanceof Number n ? n.doubleValue() : 0;
            }
            double avg = bookReviews.isEmpty() ? 0.0 : Math.round(sum / bookReviews.size() * 100.0) / 100.0;
            setValue(b, "averageRating", avg);
            setValue(b, "totalReviews", bookReviews.size());

            String author = str(b.get("author"));
            if (author != null && !authorNames.contains(norm(author))) {
                plan.warn("book " + idStr(b) + " (" + show(b.get("title")) + "): el autor " + show(author)
                        + " no existe en la coleccion author");
            }
            String category = str(b.get("category"));
            if (category != null && !genreNames.contains(norm(category))) {
                plan.warn("book " + idStr(b) + " (" + show(b.get("title")) + "): la categoria "
                        + show(category) + " no existe en la coleccion genre");
            }
        }

        // ---------------- USER ----------------
        for (Document u : users) {
            Document activity = asDoc(u.get("recentActivity"));
            if (activity == null) {
                activity = new Document("lastLoans", new ArrayList<>());
                u.put("recentActivity", activity);
            }

            Date latest = null;
            for (Document l : loansByUser.getOrDefault(idStr(u), List.of())) {
                Object d = l.get("loanDate");
                if (d instanceof Date date && (latest == null || date.after(latest))) {
                    latest = date;
                }
            }
            if (latest != null) {
                setValue(activity, "lastLoanDate", latest);
            }
            activity.put("lastLoans", syncLoanSummaries(plan, u, activity.get("lastLoans"), loanById, bookById));
        }

        // ---------------- LOAN ----------------
        for (Document l : loans) {
            String status = str(l.get("status"));
            if (!ACTIVE.equalsIgnoreCase(status) && !RETURNED.equalsIgnoreCase(status)) {
                plan.warn("loan " + idStr(l) + ": estado invalido (" + show(status)
                        + "); los valores validos son " + ACTIVE + " y " + RETURNED);
            }
            Document book = bookById.get(str(l.get("bookId")));
            if (book == null) {
                plan.warn("loan " + idStr(l) + ": apunta a un libro inexistente (" + str(l.get("bookId")) + ")");
            } else {
                syncBookSummary(l, "book", book);
            }
            Document user = userById.get(str(l.get("userId")));
            if (user == null) {
                plan.warn("loan " + idStr(l) + ": apunta a un usuario inexistente (" + str(l.get("userId")) + ")");
            } else {
                syncUserSummary(l, "user", user);
            }
        }

        // ---------------- REVIEW ----------------
        for (Document r : reviews) {
            Object ratingValue = r.get("rating");
            if (ratingValue == null) {
                plan.warn("review " + idStr(r) + ": no tiene rating");
            } else if (ratingValue instanceof Number n && (n.intValue() < MIN_RATING || n.intValue() > MAX_RATING)) {
                plan.warn("review " + idStr(r) + ": rating fuera de rango (" + n.intValue()
                        + "); el rango valido es " + MIN_RATING + "-" + MAX_RATING);
            }
            Document book = bookById.get(str(r.get("bookId")));
            if (book == null) {
                plan.warn("review " + idStr(r) + ": apunta a un libro inexistente (" + str(r.get("bookId")) + ")");
            } else {
                syncBookSummary(r, "book", book);
            }
            Document user = userById.get(str(r.get("userId")));
            if (user == null) {
                plan.warn("review " + idStr(r) + ": apunta a un usuario inexistente (" + str(r.get("userId")) + ")");
            } else {
                syncUserSummary(r, "user", user);
            }
        }

        // ---------------- FAVORITE ----------------
        for (Document f : favorites) {
            Document user = userById.get(str(f.get("userId")));
            if (user == null) {
                plan.warn("favorite " + idStr(f) + ": apunta a un usuario inexistente (" + str(f.get("userId")) + ")");
            } else {
                setValue(f, "userName", user.get("name"));
            }

            List<Object> list = asList(f.get("books"));
            for (int i = 0; i < list.size(); i++) {
                Document e = asDoc(list.get(i));
                if (e == null) {
                    continue;
                }
                String bookId = str(e.get("bookId"));
                Document book = bookById.get(bookId);
                if (book == null) {
                    plan.warn("favorite " + idStr(f) + " (books[" + i + "]): libro inexistente (" + bookId + ")");
                    continue;
                }
                setValue(e, "bookId", idStr(book));
                setValue(e, "title", book.get("title"));
                setValue(e, "author", book.get("author"));
                setValue(e, "isbn", book.get("isbn"));
            }
        }
    }

    /** Copia embebida del libro (BookSummary) dentro de loan/review. */
    private static void syncBookSummary(Document owner, String path, Document book) {
        Document summary = asDoc(owner.get(path));
        if (summary == null) {
            summary = new Document();
            owner.put(path, summary);
        }
        setValue(summary, "bookId", idStr(book));
        setValue(summary, "title", book.get("title"));
        setValue(summary, "author", book.get("author"));
        setValue(summary, "isbn", book.get("isbn"));
        setValue(summary, "category", book.get("category"));
    }


    private static void syncUserSummary(Document owner, String path, Document user) {
        Document summary = asDoc(owner.get(path));
        if (summary == null) {
            summary = new Document();
            owner.put(path, summary);
        }
        setValue(summary, "userId", idStr(user));
        setValue(summary, "name", user.get("name"));
        setValue(summary, "email", user.get("email"));
    }


    private static List<Object> syncLoanSummaries(Plan plan, Document user, Object listObj,
                                                  Map<String, Document> loanById, Map<String, Document> bookById) {
        List<Object> list = asList(listObj);
        List<Document> kept = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Document e = asDoc(list.get(i));
            if (e == null) {
                continue;
            }
            String loanId = str(e.get("loanId"));
            Document loan = loanById.get(loanId);
            if (loan == null) {
                plan.warn("user " + idStr(user) + " (lastLoans[" + i + "]): el prestamo " + loanId
                        + " ya no existe; se elimina de recentActivity");
                continue;
            }
            String bookId = str(loan.get("bookId"));
            setValue(e, "bookId", bookId);
            Document book = bookById.get(bookId);
            if (book != null) {
                setValue(e, "title", book.get("title"));
                setValue(e, "author", book.get("author"));
            }
            setValue(e, "loanDate", loan.get("loanDate"));
            setValue(e, "expectedReturnDate", loan.get("expectedReturnDate"));
            setValue(e, "status", loan.get("status"));
            kept.add(e);
        }
        kept.sort((a, b) -> {
            if (a.get("loanDate") instanceof Date x && b.get("loanDate") instanceof Date y) {
                return y.compareTo(x);
            }
            return 0;
        });
        if (kept.size() > MAX_RECENT_LOANS) {
            plan.warn("user " + idStr(user) + ": recentActivity.lastLoans tenia " + kept.size()
                    + " entradas; se recorta a las " + MAX_RECENT_LOANS + " mas recientes");
            kept = kept.subList(0, MAX_RECENT_LOANS);
        }
        return new ArrayList<>(kept);
    }

    // ---------------------------------------------------------------------
    // Utilidades de comparacion / correccion
    // ---------------------------------------------------------------------

    private static void setValue(Document target, String field, Object value) {
        if (!same(target.get(field), value)) {
            target.put(field, value);
        }
    }

    private static List<String> diff(Document old, Document updated) {
        List<String> lines = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(old.keySet());
        keys.addAll(updated.keySet());
        for (String key : keys) {
            Object a = old.get(key);
            Object b = updated.get(key);
            if (!same(a, b)) {
                lines.add(key + ": " + show(a) + "  ->  " + show(b));
            }
        }
        return lines;
    }

    private static boolean isEmpty(Object v) {
        if (v == null) {
            return true;
        }
        if (v instanceof String s) {
            return s.isBlank();
        }
        if (v instanceof Collection<?> c) {
            return c.isEmpty();
        }
        if (v instanceof Map<?, ?> m) {
            return m.isEmpty();
        }
        return false;
    }

    private static boolean same(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a instanceof Number x && b instanceof Number y) {
            return Math.abs(x.doubleValue() - y.doubleValue()) < 1e-9;
        }
        return a.equals(b);
    }

    private static Map<String, Document> indexById(List<Document> docs) {
        Map<String, Document> map = new LinkedHashMap<>();
        for (Document d : docs) {
            map.put(idStr(d), d);
        }
        return map;
    }


    private static String idStr(Document d) {
        return String.valueOf(d.get("_id"));
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static Document asDoc(Object o) {
        return o instanceof Document d ? d : null;
    }

    private static List<Object> asList(Object o) {
        return o instanceof List<?> l ? new ArrayList<Object>(l) : List.of();
    }

    private static String norm(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String show(Object o) {
        if (o == null) {
            return "(vacio)";
        }
        if (o instanceof String s) {
            return "\"" + (s.length() > 50 ? s.substring(0, 47) + "..." : s) + "\"";
        }
        if (o instanceof Document) {
            return "{...}";
        }
        if (o instanceof List) {
            return "[...]";
        }
        return String.valueOf(o);
    }

    // =====================================================================
    // MODELO
    // =====================================================================

    /** Cambios pendientes de UN documento. */
    static final class Fix {
        final String collection;
        final Object id;
        final Document updated;
        final List<String> lines;

        Fix(String collection, Object id, Document updated, List<String> lines) {
            this.collection = collection;
            this.id = id;
            this.updated = updated;
            this.lines = lines;
        }
    }


    static final class Plan {
        final Map<String, List<Fix>> fixes = new LinkedHashMap<>();
        final Map<String, Integer> scanned = new LinkedHashMap<>();
        final List<String> warnings = new ArrayList<>();

        void add(String collection, Object id, Document updated, List<String> lines) {
            fixes.computeIfAbsent(collection, k -> new ArrayList<>()).add(new Fix(collection, id, updated, lines));
        }

        void warn(String message) {
            warnings.add(message);
        }

        int docCount() {
            return fixes.values().stream().mapToInt(List::size).sum();
        }
    }

    // =====================================================================
    // REPORTE Y APROBACION
    // =====================================================================
    private static void printReport(Plan plan, boolean verbose) {
        System.out.println("\n================ REPORTE DE DATOS DESACTUALIZADOS ================");
        for (String c : COLLECTIONS) {
            List<Fix> fixes = plan.fixes.getOrDefault(c, List.of());
            int fields = fixes.stream().mapToInt(f -> f.lines.size()).sum();
            System.out.printf("%n[%s] %d documentos revisados, %d desactualizados (%d campos)%n",
                    c, plan.scanned.getOrDefault(c, 0), fixes.size(), fields);

            int printed = 0;
            outer:
            for (Fix fix : fixes) {
                for (String line : fix.lines) {
                    if (!verbose && printed >= PREVIEW_LINES) {
                        break outer;
                    }
                    System.out.println("   " + fix.id + "  " + line);
                    printed++;
                }
            }
            if (printed < fields) {
                System.out.println("   ... y " + (fields - printed) + " cambios mas (use --verbose para verlos todos)");
            }
        }

        if (!plan.warnings.isEmpty()) {
            System.out.println("\n---- Avisos que NO se corrigen automaticamente (" + plan.warnings.size() + ") ----");
            int shown = 0;
            for (String w : plan.warnings) {
                if (!verbose && shown >= PREVIEW_LINES) {
                    System.out.println("   ... y " + (plan.warnings.size() - shown) + " avisos mas (--verbose)");
                    break;
                }
                System.out.println("   ! " + w);
                shown++;
            }
        }
        System.out.println("\nTotal: " + plan.docCount() + " documentos con datos desactualizados.");
    }

    private static Set<String> askApproval(Plan plan) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        Set<String> all = new LinkedHashSet<>(plan.fixes.keySet());
        while (true) {
            System.out.println("\nAPROBACION DEL ADMINISTRADOR:");
            System.out.println("  ENTER -> aplicar todos los cambios en MongoDB");
            System.out.println("  c     -> elegir coleccion por coleccion");
            System.out.println("  n     -> cancelar");
            System.out.print("> ");
            String answer = in.readLine();
            if (answer == null) {
                return Set.of();
            }
            answer = answer.trim().toLowerCase(Locale.ROOT);
            if (answer.isEmpty() || answer.equals("s") || answer.equals("si") || answer.equals("y")) {
                return all;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return Set.of();
            }
            if (answer.equals("c")) {
                Set<String> chosen = new LinkedHashSet<>();
                for (String c : all) {
                    System.out.print("Aplicar a '" + c + "' (" + plan.fixes.get(c).size()
                            + " documentos)? [ENTER = si / n = no] > ");
                    String a = in.readLine();
                    if (a == null) {
                        return Set.of();
                    }
                    if (!a.trim().toLowerCase(Locale.ROOT).startsWith("n")) {
                        chosen.add(c);
                    }
                }
                return chosen;
            }
        }
    }

    // =====================================================================
    // MONGODB (lectura / escritura)
    // =====================================================================
    private static Map<String, List<Document>> load(MongoDatabase db) {
        Map<String, List<Document>> data = new LinkedHashMap<>();
        for (String name : COLLECTIONS) {
            data.put(name, db.getCollection(name).find().into(new ArrayList<>()));
        }
        return data;
    }

    private static int apply(MongoDatabase db, Plan plan, Set<String> approved) {
        int updated = 0;
        for (String collection : COLLECTIONS) {
            if (!approved.contains(collection)) {
                continue;
            }
            List<Fix> fixes = plan.fixes.get(collection);
            if (fixes == null || fixes.isEmpty()) {
                continue;
            }
            MongoCollection<Document> col = db.getCollection(collection);
            List<WriteModel<Document>> ops = new ArrayList<>();
            int modified = 0;
            for (Fix fix : fixes) {
                ops.add(new ReplaceOneModel<>(Filters.eq("_id", fix.id), fix.updated));
                if (ops.size() >= BATCH_SIZE) {
                    modified += flush(col, ops);
                }
            }
            modified += flush(col, ops);
            System.out.println("  " + collection + ": " + modified + " documentos actualizados");
            updated += modified;
        }
        return updated;
    }

    private static int flush(MongoCollection<Document> col, List<WriteModel<Document>> ops) {
        if (ops.isEmpty()) {
            return 0;
        }
        BulkWriteResult result = col.bulkWrite(ops, new BulkWriteOptions().ordered(false));
        ops.clear();
        return result.getModifiedCount();
    }

    // =====================================================================
    // CONFIGURACION (sin credenciales hardcodeadas)
    // =====================================================================
    private static String[] loadConnection() throws IOException {
        String uri = System.getenv("MONGODB_URI");
        String database = System.getenv("MONGODB_DATABASE");

        if (uri == null || database == null) {
            Properties props = new Properties();
            try (InputStream in = openApplicationProperties()) {
                if (in != null) {
                    props.load(in);
                }
            }
            if (uri == null) {
                uri = props.getProperty("spring.mongodb.uri", props.getProperty("spring.data.mongodb.uri"));
            }
            if (database == null) {
                database = props.getProperty("spring.mongodb.database", props.getProperty("spring.data.mongodb.database"));
            }
        }
        if (uri == null || database == null) {
            throw new IllegalStateException("No se encontro la conexion. Defina MONGODB_URI y MONGODB_DATABASE "
                    + "o spring.mongodb.uri / spring.mongodb.database en application.properties");
        }
        return new String[]{uri.trim(), database.trim()};
    }

    private static InputStream openApplicationProperties() throws IOException {
        InputStream cp = DenormalizationSync.class.getResourceAsStream("/application.properties");
        if (cp != null) {
            return cp;
        }
        File file = new File("src/main/resources/application.properties");
        return file.exists() ? new FileInputStream(file) : null;
    }
}
