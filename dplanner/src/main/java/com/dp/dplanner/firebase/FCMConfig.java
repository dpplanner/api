package com.dp.dplanner.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
public class FCMConfig {
    @Bean
    FirebaseMessaging firebaseMessaging() throws IOException {
        ClassPathResource resource = new ClassPathResource("dplanner-c2b9a-firebase-adminsdk-drga5-5c3beeb1bb.json");
        InputStream serviceAccount = resource.getInputStream();

        FirebaseApp firebaseApp = null;
        List<FirebaseApp> firebaseAppList = FirebaseApp.getApps();

        if (firebaseAppList != null && !firebaseAppList.isEmpty()) {
            for (FirebaseApp app : firebaseAppList) {
                if (app.getName().equals(FirebaseApp.DEFAULT_APP_NAME)) {
                    firebaseApp = app;
                }
            }
        }else{
            FirebaseOptions options =  new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            firebaseApp = FirebaseApp.initializeApp(options);

        }

        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
