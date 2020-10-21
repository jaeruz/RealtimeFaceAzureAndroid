package com.example.mthird;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import dmax.dialog.SpotsDialog;
import edmt.dev.edmtdevcognitiveface.Contract.Face;
import edmt.dev.edmtdevcognitiveface.Contract.IdentifyResult;
import edmt.dev.edmtdevcognitiveface.Contract.Person;
import edmt.dev.edmtdevcognitiveface.Contract.TrainingStatus;
import edmt.dev.edmtdevcognitiveface.FaceServiceClient;
import edmt.dev.edmtdevcognitiveface.FaceServiceRestClient;
import edmt.dev.edmtdevcognitiveface.Rest.ClientException;
import edmt.dev.edmtdevcognitiveface.Rest.Utils;

public class AzureActivity extends AppCompatActivity {

    private final String API_KEY = "d48fdff6e7af4dfd86fbb757c44c6884";
    private final String API_LINK = "https://detectrecogdemo.cognitiveservices.azure.com/face/v1.0";

    private FaceServiceClient faceServiceClient = new FaceServiceRestClient(API_LINK, API_KEY);

    private  final String personGroupID = "randomperson";

    ImageView img_view;
    Bitmap bmp;
    Face[] faceDetected;
    boolean detectFinished=false;

    Button btn_detect, btn_identify;

    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;


    class detectTask extends AsyncTask<InputStream,String,Face[]> {

        AlertDialog alertDialog = new SpotsDialog.Builder()
                .setContext(AzureActivity.this)
                .setCancelable(false)
                .build();

        @Override
        protected void onPreExecute() {
            //alertDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //alertDialog.setMessage((values[0]));
        }

        @Override
        protected Face[] doInBackground(InputStream... inputStreams) {

            try{
                publishProgress("Detecting...");
                Face[] result = faceServiceClient.detect((inputStreams[0]),true,false,null);
                if(result==null){
                    return null;
                }else{
                    return result;
                }
            }catch (ClientException e){
                e.printStackTrace();
                Intent intent = new Intent(AzureActivity.this, MainActivity.class);
                startActivity(intent);
            }catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Face[] faces) {

            //alertDialog.dismiss();
            if(faces == null){
                Toast.makeText(AzureActivity.this, "No face detected", Toast.LENGTH_SHORT).show();
            }else {
                img_view.setImageBitmap(Utils.drawFaceRectangleOnBitmap(bmp,faces, Color.YELLOW));
                faceDetected = faces;
//                detectFinished=true;
                //btn_identify.setEnabled(true);
                try{
                    Thread.sleep(1000);
                    if (faceDetected.length > 0){
                        final UUID[] faceIds = new UUID[faceDetected.length];
                        for (int i = 0; i<faceDetected.length;i++)
                            faceIds[i] = faceDetected[i].faceId;
                        new IdentificationTask().execute(faceIds);
                    }else{
                        Toast.makeText(AzureActivity.this,"No face to detect", Toast.LENGTH_SHORT).show();
                    }
                    Thread.sleep(3000);
                    Intent intent = new Intent(AzureActivity.this, MainActivity.class);
                    startActivity(intent);
                }catch (Exception ex){
                    ex.printStackTrace();
                }

            }
        }
    }

    class IdentificationTask extends AsyncTask<UUID,String, IdentifyResult[]>{

        AlertDialog alertDialog = new SpotsDialog.Builder()
                .setContext(AzureActivity.this)
                .setCancelable(false)
                .build();

        @Override
        protected void onPreExecute() {
            //alertDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
           // alertDialog.setMessage((values[0]));
        }

        @Override
        protected IdentifyResult[] doInBackground(UUID... uuids) {
            try{
                publishProgress("Getting person group status");
                TrainingStatus trainingStatus = faceServiceClient.getPersonGroupTrainingStatus(personGroupID);

                if (trainingStatus.status != TrainingStatus.Status.Succeeded){
                    //Log.d("ERROR","Person Group Training status is "+trainingStatus.status);
                    return  null;
                }

                publishProgress("identifying");
                IdentifyResult[] result = faceServiceClient.identity(personGroupID,uuids,2); //max number of candidates

                //Log.d("Hello",String.valueOf(result.length));
                if (result.length > 0){
                    //Log.d("Hello","not null");
                    return result;
                }else{
                    //Log.d("Hello","null ");
                    return null;
                }

            } catch (ClientException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(IdentifyResult[] identifyResults) {
            //alertDialog.dismiss();
            //Log.d("152",String.valueOf(identifyResults.length));
            if (identifyResults != null){
                for(IdentifyResult identifyResult:identifyResults)
//                    Log.d("155",identifyResult.candidates.toString());

                    try {
                        new PersonDetectionTask().execute(identifyResult.candidates.get(0).personId);
                    }catch (Exception ex){
                        //Toast.makeText(MainActivity.this, "Unknown", Toast.LENGTH_SHORT).show();
                        ex.printStackTrace();
                    }



            }
        }
    }

    class PersonDetectionTask extends AsyncTask<UUID, String, edmt.dev.edmtdevcognitiveface.Contract.Person> {

        AlertDialog alertDialog = new SpotsDialog.Builder()
                .setContext(AzureActivity.this)
                .setCancelable(false)
                .build();

        @Override
        protected void onPreExecute() {
            //alertDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            //alertDialog.setMessage((values[0]));
        }

        @Override
        protected Person doInBackground(UUID... uuids) {
            try {
//                String s = faceServiceClient.getPerson(personGroupID,uuids[0]).toString();
//                Log.d("190",s);
                if(uuids.length<1 || uuids==null){
                    return null;
                }else{
                    return faceServiceClient.getPerson(personGroupID,uuids[0]);
                }

            } catch (ClientException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Person person) {
            //alertDialog.dismiss();

            if(person!=null){
//                img_view.setImageBitmap(Utils.drawFaceRectangleWithTextOnBitmap(bmp,faceDetected,person.name,Color.YELLOW,100));
                Toast.makeText(AzureActivity.this, person.name, Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(AzureActivity.this, "Undefined Face", Toast.LENGTH_SHORT).show();
            }

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //orig
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_azure);

        Bundle extras = getIntent().getExtras();
        byte[] byteArray = extras.getByteArray("picture");

        bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        img_view = (ImageView) findViewById(R.id.imageView1);

        img_view.setImageBitmap(bmp);

        //orig
//        btn_detect =(Button)findViewById(R.id.btn_detect);
//        btn_identify =(Button)findViewById(R.id.btn_identify);


////        if (img_view.getDrawable()!=null){
//            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                new detectTask().execute(inputStream);
//
////                if(detectFinished){
////                    Toast.makeText(AzureActivity.this, "not null", Toast.LENGTH_SHORT).show();
////                    if (faceDetected.length > 0){
////                        final UUID[] faceIds = new UUID[faceDetected.length];
////                        for (int i = 0; i<faceDetected.length;i++)
////                            faceIds[i] = faceDetected[i].faceId;
////                        new IdentificationTask().execute(faceIds);
////                    }else{
////                        Toast.makeText(AzureActivity.this,"No face to detect", Toast.LENGTH_SHORT).show();
////                    }
////                }else{
////                    Toast.makeText(AzureActivity.this, "null", Toast.LENGTH_SHORT).show();
////                }
//
//            }catch (Exception ex){
//                ex.printStackTrace();
//            }
////        }




//        btn_detect.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                bmp.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
//                ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
//                new detectTask().execute(inputStream);
//            }
//        });
//        btn_identify.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (faceDetected.length > 0){
//                    final UUID[] faceIds = new UUID[faceDetected.length];
//                    for (int i = 0; i<faceDetected.length;i++)
//                        faceIds[i] = faceDetected[i].faceId;
//                    new IdentificationTask().execute(faceIds);
//                }else{
//                    Toast.makeText(AzureActivity.this,"No face to detect", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
        //

    }
}