package com.example.androidcicd.movie;

import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class MovieProvider {
    private static MovieProvider movieProvider;
    private final ArrayList<Movie> movies;
    private final CollectionReference movieCollection;

    private MovieProvider(FirebaseFirestore firestore) {
        movies = new ArrayList<>();
        movieCollection = firestore.collection("movies");
    }

    public interface DataStatus {
        void onDataUpdated();
        void onError(String error);
    }

    public void listenForUpdates(final DataStatus dataStatus) {
        movieCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                dataStatus.onError(error.getMessage());
                return;
            }
            movies.clear();
            if (snapshot != null) {
                for (QueryDocumentSnapshot item : snapshot) {
                    movies.add(item.toObject(Movie.class));
                }
                dataStatus.onDataUpdated();
            }
        });
    }

    public static MovieProvider getInstance(FirebaseFirestore firestore) {
        if (movieProvider == null)
            movieProvider = new MovieProvider(firestore);
        return movieProvider;
    }

    public static void setInstanceForTesting(FirebaseFirestore firestore) {
        movieProvider = new MovieProvider(firestore);
    }

    public ArrayList<Movie> getMovies() {
        return movies;
    }

    public void updateMovie(Movie movie, String title, String genre, int year) {
        movie.setTitle(title);
        movie.setGenre(genre);
        movie.setYear(year);
        DocumentReference docRef = movieCollection.document(movie.getId());
        if (validMovie(movie, docRef)) {
            docRef.set(movie);
        } else {
            throw new IllegalArgumentException("Invalid Movie!");
        }
    }

    public void addMovie(Movie movie) {
        DocumentReference docRef = movieCollection.document();
        movie.setId(docRef.getId());
        if (validMovie(movie, docRef)) {
            docRef.set(movie);
        } else {
            throw new IllegalArgumentException("Invalid Movie!");
        }
    }

    public void deleteMovie(Movie movie) {
        DocumentReference docRef = movieCollection.document(movie.getId());
        docRef.delete();
    }

    public Task<Boolean> movieExists(String title) {
        return movieCollection.whereEqualTo("title", title)
                .get()
                .continueWith(queryDocumentSnapshot -> queryDocumentSnapshot.isSuccessful() && !queryDocumentSnapshot.getResult().isEmpty());
    }

    public boolean validMovie(Movie movie, DocumentReference docRef) {
        return movie.getId().equals(docRef.getId()) && !movie.getTitle().isEmpty() && !movie.getGenre().isEmpty() && movie.getYear() > 0;
    }
}
