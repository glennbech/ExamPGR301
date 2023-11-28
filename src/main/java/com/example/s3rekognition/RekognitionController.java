package com.example.s3rekognition;

import com.example.s3rekognition.MedicalSupplies;
import com.example.s3rekognition.MetricsConfig;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Clock;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.example.s3rekognition.PPEClassificationResponse;
import com.example.s3rekognition.PPEResponse;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.DependsOn;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import static java.util.Optional.ofNullable;


@RestController
public class RekognitionController implements ApplicationListener<ApplicationReadyEvent> {

    private final AmazonS3 s3Client;
    private final AmazonRekognition rekognitionClient;
    private MeterRegistry meterRegistry;

    private final Map<String, MedicalSupplies> medicalSuppliesInventory = new HashMap<>();

    private static final Logger logger = Logger.getLogger(RekognitionController.class.getName());

    @Autowired
    public RekognitionController(MeterRegistry meterRegistry) {
        this.s3Client = AmazonS3ClientBuilder.standard().build();
        this.rekognitionClient = AmazonRekognitionClientBuilder.standard().build();
        this.meterRegistry = meterRegistry;
    }
    
    
    
    private MedicalSupplies getOrCreateSupply(String supplyId) {
        if (medicalSuppliesInventory.get(supplyId) == null) {
            MedicalSupplies m = new MedicalSupplies();
            m.setId(supplyId);
            medicalSuppliesInventory.put(supplyId, m);
        }
        return medicalSuppliesInventory.get(supplyId);
    }


    //gets the medical drug you are looking for based on id
    @Timed("candaidate2043drugget")
    @GetMapping(path = "/medical-supply/{supplyId}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<MedicalSupplies> balance(@PathVariable String supplyId) {
        meterRegistry.counter("supply_balance").increment();
        MedicalSupplies drug = ofNullable(medicalSuppliesInventory.get(supplyId)).orElseThrow(MedicalDrugNotFoundException::new);
        
        return new ResponseEntity<>(drug, HttpStatus.OK);
    }



    /**
     * Saves an supply. Will create a new supply if one does not exist.
     *
     * @param m the medicalDrug Object
     * @return
     */
    @Timed("candidate2043drugpost")
    @PostMapping(path = "/medical-supply", consumes = "application/json",
            produces = "application/json")
    public ResponseEntity<MedicalSupplies> updateSupply(@RequestBody MedicalSupplies m) {
        meterRegistry.counter("update_supply").increment();
        MedicalSupplies medicalSupply = getOrCreateSupply(m.getId());
        medicalSupply.setSupplyBalance(m.getSupplyBalance());
        medicalSupply.setDrugName(m.getDrugName());
        medicalSuppliesInventory.put(m.getId(), m);
        return new ResponseEntity<>(m, HttpStatus.OK);
    }

    
    
    

    /**
     * This endpoint takes an S3 bucket name in as an argument, scans all the
     * Files in the bucket for Protective Gear Violations.
     * <p>
     *
     * @param bucketName
     * @return
     */
     

    @GetMapping(value = "/scan-ppe", consumes = "*/*", produces = "application/json")
    @ResponseBody
    @Timed(value = "ppeget")
    public ResponseEntity<PPEResponse> scanForPPE(@RequestParam String bucketName) {
        
        Timer mytimer = Timer.builder("ppescanduration")
            .register(meterRegistry);
        
        // List all objects in the S3 bucket
        ListObjectsV2Result imageList = s3Client.listObjectsV2(bucketName);

        // This will hold all of our classifications
        List<PPEClassificationResponse> classificationResponses = new ArrayList<>();

        // This is all the images in the bucket
        List<S3ObjectSummary> images = imageList.getObjectSummaries();
        
        
        mytimer.record(() -> {
        // Iterate over each object and scan for PPE
        for (S3ObjectSummary image : images) {
            logger.info("scanning " + image.getKey());
            

            // This is where the magic happens, use AWS rekognition to detect PPE
            DetectProtectiveEquipmentRequest request = new DetectProtectiveEquipmentRequest()
                    .withImage(new Image()
                            .withS3Object(new S3Object()
                                    .withBucket(bucketName)
                                    .withName(image.getKey())))
                    .withSummarizationAttributes(new ProtectiveEquipmentSummarizationAttributes()
                            .withMinConfidence(80f)
                            .withRequiredEquipmentTypes("FACE_COVER"));

            DetectProtectiveEquipmentResult result = rekognitionClient.detectProtectiveEquipment(request);

            // If any person on an image lacks PPE on the face, it's a violation of regulations
            boolean violation = isViolation(result);

            logger.info("scanning " + image.getKey() + ", violation result " + violation);
            // Categorize the current image as a violation or not.
            int personCount = result.getPersons().size();
            PPEClassificationResponse classification = new PPEClassificationResponse(image.getKey(), personCount, violation);
            classificationResponses.add(classification);
        }
        });
        
        PPEResponse ppeResponse = new PPEResponse(bucketName, classificationResponses);
        return ResponseEntity.ok(ppeResponse);
    }

    /**
     * Detects if the image has a protective gear violation for the FACE bodypart-
     * It does so by iterating over all persons in a picture, and then again over
     * each body part of the person. If the body part is a FACE and there is no
     * protective gear on it, a violation is recorded for the picture.
     *
     * @param result
     * @return
     */
    private static boolean isViolation(DetectProtectiveEquipmentResult result) {
        return result.getPersons().stream()
                .flatMap(p -> p.getBodyParts().stream())
                .anyMatch(bodyPart -> bodyPart.getName().equals("FACE")
                        && bodyPart.getEquipmentDetections().isEmpty());
    }




    @GetMapping(value = "/scan-ppe-hand-cover", consumes = "*/*", produces = "application/json")
    @ResponseBody
    @Timed("handcoverget")
    public ResponseEntity<PPEResponse> scanForHandCover(@RequestParam String bucketName) {
        
        Timer mytimer = Timer.builder("handcoverscanduration")
            .register(meterRegistry);
        
        // List all objects in the S3 bucket
        ListObjectsV2Result imageList = s3Client.listObjectsV2(bucketName);

        // This will hold all of our classifications
        List<PPEClassificationResponse> classificationResponses = new ArrayList<>();

        // This is all the images in the bucket
        List<S3ObjectSummary> images = imageList.getObjectSummaries();


        mytimer.record(() -> {
        // Iterate over each object and scan for PPE
        for (S3ObjectSummary image : images) {
            logger.info("scanning " + image.getKey());

            // This is where the magic happens, use AWS rekognition to detect PPE hand covers
            DetectProtectiveEquipmentRequest request = new DetectProtectiveEquipmentRequest()
                    .withImage(new Image()
                            .withS3Object(new S3Object()
                                    .withBucket(bucketName)
                                    .withName(image.getKey())))
                    .withSummarizationAttributes(new ProtectiveEquipmentSummarizationAttributes()
                            .withMinConfidence(80f)
                            .withRequiredEquipmentTypes("HAND_COVER"));

            DetectProtectiveEquipmentResult result = rekognitionClient.detectProtectiveEquipment(request);

            // If any person on an image lacks PPE on the one of their hands, it's a violation of regulations
            boolean violation = isHandCoverViolation(result);

            logger.info("scanning " + image.getKey() + ", violation result " + violation);
            // Categorize the current image as a violation or not.
            int personCount = result.getPersons().size();
            PPEClassificationResponse classification = new PPEClassificationResponse(image.getKey(), personCount, violation);
            classificationResponses.add(classification);
        }
        
        });
        PPEResponse ppeResponse = new PPEResponse(bucketName, classificationResponses);
        return ResponseEntity.ok(ppeResponse);
    }

    private static boolean isHandCoverViolation(DetectProtectiveEquipmentResult result) {
        return result.getPersons().stream()
                .allMatch(person -> {
                    boolean rightHandCovered = person.getBodyParts().stream()
                            .anyMatch(bodyPart -> bodyPart.getName().equals("RIGHT_HAND") &&
                                    bodyPart.getEquipmentDetections().stream()
                                            .anyMatch(equipmentDetection ->
                                                    equipmentDetection.getType().equals("HAND_COVER") &&
                                                            equipmentDetection.getCoversBodyPart().getValue()));

                    boolean leftHandCovered = person.getBodyParts().stream()
                            .anyMatch(bodyPart -> bodyPart.getName().equals("LEFT_HAND") &&
                                    bodyPart.getEquipmentDetections().stream()
                                            .anyMatch(equipmentDetection ->
                                                    equipmentDetection.getType().equals("HAND_COVER") &&
                                                            equipmentDetection.getCoversBodyPart().getValue()));

                    // Return true if both hands are not covered
                    return !(rightHandCovered && leftHandCovered);
                });
    }
    

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        
        Gauge.builder("drug_count", medicalSuppliesInventory,
                m -> m.values().size()).register(meterRegistry);

        // Denne meter-typen "Gauge" rapporterer hvor mange supplies som totalt eksisterer
        Gauge.builder("total_drug_inventory", medicalSuppliesInventory,
                        m -> m.values()
                                .stream()
                                .map(MedicalSupplies::getSupplyBalance)
                                .mapToDouble(BigDecimal::doubleValue)
                                .sum())
                .register(meterRegistry);
    }
    
    @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "medical drug not found")
    public static class MedicalDrugNotFoundException extends RuntimeException {
    }
}