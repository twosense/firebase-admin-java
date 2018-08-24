/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.admin.database.integration;

import com.google.firebase.admin.FirebaseApp;
import com.google.firebase.admin.FirebaseOptions;
import com.google.firebase.admin.database.DataSnapshot;
import com.google.firebase.admin.database.DatabaseError;
import com.google.firebase.admin.database.DatabaseReference;
import com.google.firebase.admin.database.FirebaseDatabase;
import com.google.firebase.admin.database.ValueEventListener;
import java.util.concurrent.Semaphore;

public class ShutdownExample {

  public static void main(String[] args) {
    final Semaphore shutdownLatch = new Semaphore(0);

    FirebaseApp app =
        FirebaseApp.initializeApp(
            new FirebaseOptions.Builder()
                .setDatabaseUrl("https://admin-java-sdk.firebaseio.com")
                .build());

    FirebaseDatabase db = FirebaseDatabase.getInstance(app);
    DatabaseReference ref = db.getReference();

    ValueEventListener listener =
        ref.child("shutdown")
            .addValueEventListener(
                new ValueEventListener() {
                  @Override
                  public void onDataChange(DataSnapshot snapshot) {
                    Boolean shouldShutdown = snapshot.getValue(Boolean.class);
                    if (shouldShutdown != null && shouldShutdown) {
                      System.out.println("Should shut down");
                      shutdownLatch.release(1);
                    } else {
                      System.out.println("Not shutting down: " + shouldShutdown);
                    }
                  }

                  @Override
                  public void onCancelled(DatabaseError error) {
                    System.err.println("Shouldn't happen");
                  }
                });

    try {
      // Keeps us running until we receive the notification to shut down
      shutdownLatch.acquire(1);
      ref.child("shutdown").removeEventListener(listener);
      db.goOffline();
      System.out.println("Done, should exit");
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}