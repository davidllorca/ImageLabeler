package com.example.imagelabeler

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import java.io.IOException
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import android.support.annotation.NonNull
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabel
import com.google.firebase.ml.vision.label.FirebaseVisionLabel
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers


class MainActivity : AppCompatActivity() {

    data class Product(val title: String, val subtitle: String, val url: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //val uri = Uri.parse("android.resource://" + packageName + "/" + R.raw.model)
        //image = FirebaseVisionImage.fromFilePath(this, uri)

        var products = mutableListOf<Product>()

        val string = resources.openRawResource(R.raw.she_new).bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(string)
        for (i in 0 until jsonArray.length()) {
            with(jsonArray.getJSONObject(i)) {
                products.add(Product(this.getString("name"), this.getString("id"), this.getString("image")))
            }
        }

//        for (product in products) {

        Single.fromCallable {
            val bitmap = Picasso.get().load(products[0].url).get()
            val image = FirebaseVisionImage.fromBitmap(bitmap)
            runLabelerTask(image)
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()

    }

    private fun runLabelerTask(image: FirebaseVisionImage) {
        getLabelDetectorTask(image)
                .continueWith { getFaceDetectorTask(image) }
    }

    private fun getFaceDetectorTask(image: FirebaseVisionImage): Task<MutableList<FirebaseVisionFace>> {
        val options = FirebaseVisionFaceDetectorOptions.Builder()
                .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setMinFaceSize(0.15f)
                .setTrackingEnabled(true)
                .build()

        val detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options)


        return detector.detectInImage(image)
                .addOnSuccessListener {
                    // Task completed successfully
                    tv_labeler_log.append("==================\n")
                    tv_labeler_log.append("FACES\n")
                    tv_labeler_log.append("==================\n")
                    for (face in it) {
                        val faceResult = "smile=${face.smilingProbability}," +
                                "lefteye=${face.leftEyeOpenProbability}" +
                                "righteye=${face.rightEyeOpenProbability}" +
                                "angley=${face.headEulerAngleY}" +
                                "anglez=${face.headEulerAngleZ} \n"

                        tv_labeler_log.append(faceResult)
                        Log.i("Result Faces", faceResult)
                    }
                }
                .addOnFailureListener(
                        object : OnFailureListener {
                            override fun onFailure(e: Exception) {
                                // Task failed with an exception
                                Log.i("Result Faces", e.toString())
                            }
                        })
    }

    private fun getLabelDetectorTask(image: FirebaseVisionImage): Task<MutableList<FirebaseVisionCloudLabel>> {

        val labelDetector = FirebaseVision.getInstance()
                .visionCloudLabelDetector

        //val detector = FirebaseVision.getInstance()
        //      .visionCloudLabelDetector
        // Or, to change the default settings:
        // FirebaseVisionCloudLabelDetector detector = FirebaseVision.getInstance()
        //         .getVisionCloudLabelDetector(options);


        return labelDetector.detectInImage(image)
                .addOnSuccessListener {
                    // Task completed successfully
                    tv_labeler_log.append("==================\n")
                    tv_labeler_log.append("LABELS\n")
                    tv_labeler_log.append("==================\n")
                    for (label in it) {
                        val text = label.getLabel()
                        val entityId = label.getEntityId()
                        val confidence = label.getConfidence()
                        tv_labeler_log.append("label:[$text], entityId=[$entityId], confidence=[$confidence]")
                        tv_labeler_log.append("\n")
                        Log.i("Result Labels", "label:[$text], entityId=[$entityId], confidence=[$confidence]")
                    }
                }
                .addOnFailureListener(
                        object : OnFailureListener {
                            override fun onFailure(e: Exception) {
                                // Task failed with an exception
                                Log.i("Result Labels", e.toString())
                            }
                        })
    }
}
