package com.astralimit.dogfit.data

import android.util.Log
import com.astralimit.dogfit.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

private fun parseCalibrationFeatures(json: String): CalibrationFeatures {
    if (json.isEmpty()) return CalibrationFeatures()
    return try {
        val obj = JSONObject(json)
        CalibrationFeatures(
            mean = obj.optDouble("mean", 0.0).toFloat(),
            std = obj.optDouble("std", 0.0).toFloat(),
            variance = obj.optDouble("variance", 0.0).toFloat(),
            rms = obj.optDouble("rms", 0.0).toFloat(),
            peak = obj.optDouble("peak", 0.0).toFloat(),
            energy = obj.optDouble("energy", 0.0).toFloat()
        )
    } catch (e: Exception) {
        CalibrationFeatures()
    }
}

private fun serializeCalibrationFeatures(features: CalibrationFeatures): String {
    val obj = JSONObject()
    obj.put("mean", features.mean.toDouble())
    obj.put("std", features.std.toDouble())
    obj.put("variance", features.variance.toDouble())
    obj.put("rms", features.rms.toDouble())
    obj.put("peak", features.peak.toDouble())
    obj.put("energy", features.energy.toDouble())
    return obj.toString()
}

class DogFitRepository(
    private val profileDao: DogProfileDao,
    private val vaccinationDao: VaccinationDao,
    private val dewormingDao: DewormingDao,
    private val activityDao: DailyActivityDao
) {
    val dogProfile: Flow<DogProfile?> = profileDao.getProfile().map { entity ->
        entity?.toDogProfile()
    }

    val dailyActivity: Flow<List<DailyActivityEntity>> = activityDao.getAllActivity()

    suspend fun getActivityForDate(date: String): DailyActivityEntity? {
        val result = activityDao.getActivityForDate(date)
        Log.d("DogFitRepository", "getActivityForDate($date) = ${result?.activeMinutes ?: "null"} min activos")
        return result
    }

    suspend fun saveDailyActivity(activity: DailyActivityEntity) {
        try {
            activityDao.insertActivity(activity)
            Log.d("DogFitRepository", "Actividad guardada: ${activity.date} - ${activity.activeMinutes} min activos")
        } catch (e: Exception) {
            Log.e("DogFitRepository", "Error guardando actividad: ${e.message}", e)
        }
    }

    val vaccinations: Flow<List<Vaccination>> = vaccinationDao.getAllVaccinations().map { list ->
        list.map { it.toVaccination() }
    }

    val dewormings: Flow<List<Deworming>> = dewormingDao.getAllDewormings().map { list ->
        list.map { it.toDeworming() }
    }

    suspend fun getProfileSync(): DogProfile? {
        return profileDao.getProfileSync()?.toDogProfile()
    }

    suspend fun saveProfile(profile: DogProfile) {
        val entity = profile.toEntity()
        Log.d("DogFitRepository", "saveProfile: ${profile.name}, vetVisits=${profile.vetVisits.size}, vetVisitsJson=${entity.vetVisitsJson}")
        profileDao.insertProfile(entity)
    }

    suspend fun addVaccination(vaccination: Vaccination) {
        vaccinationDao.insertVaccination(vaccination.toEntity())
    }

    suspend fun updateVaccination(vaccination: Vaccination) {
        vaccinationDao.updateVaccination(vaccination.toEntity())
    }

    suspend fun addDeworming(deworming: Deworming) {
        dewormingDao.insertDeworming(deworming.toEntity())
    }

    suspend fun updateDeworming(deworming: Deworming) {
        dewormingDao.updateDeworming(deworming.toEntity())
    }

    private fun DogProfileEntity.toDogProfile(): DogProfile {
        return DogProfile(
            id = id,
            name = name,
            petType = try { PetType.valueOf(petType) } catch (e: Exception) { PetType.DOG },
            breed = breed,
            birthDate = birthDate?.let { Date(it) },
            age = age,
            weight = weight,
            weightHistory = parseWeightHistory(weightHistory),
            height = height,
            targetActiveMinutes = targetActiveMinutes,
            dailyCalories = dailyCalories,
            imageUrl = imageUrl,
            color = color,
            microchipNumber = microchipNumber,
            medicalRecord = PetMedicalRecord(),
            vetVisits = parseVetVisits(vetVisitsJson),
            activitySensitivity = activitySensitivity,
            isCalibrated = isCalibrated == 1,
            calibrationRestValue = calibrationRestValue,
            calibrationWalkValue = calibrationWalkValue,
            calibrationRunValue = calibrationRunValue,
            calibrationRestFeatures = parseCalibrationFeatures(calibrationRestFeaturesJson),
            calibrationWalkFeatures = parseCalibrationFeatures(calibrationWalkFeaturesJson),
            calibrationRunFeatures = parseCalibrationFeatures(calibrationRunFeaturesJson)
        )
    }

    private fun DogProfile.toEntity(): DogProfileEntity {
        return DogProfileEntity(
            id = 1,
            name = name,
            petType = petType.name,
            breed = breed,
            birthDate = birthDate?.time,
            age = age,
            weight = weight,
            weightHistory = serializeWeightHistory(weightHistory),
            height = height,
            targetActiveMinutes = targetActiveMinutes,
            dailyCalories = dailyCalories,
            imageUrl = imageUrl,
            color = color,
            microchipNumber = microchipNumber,
            vetVisitsJson = serializeVetVisits(vetVisits),
            activitySensitivity = activitySensitivity,
            isCalibrated = if (isCalibrated) 1 else 0,
            calibrationRestValue = calibrationRestValue,
            calibrationWalkValue = calibrationWalkValue,
            calibrationRunValue = calibrationRunValue,
            calibrationRestFeaturesJson = serializeCalibrationFeatures(calibrationRestFeatures),
            calibrationWalkFeaturesJson = serializeCalibrationFeatures(calibrationWalkFeatures),
            calibrationRunFeaturesJson = serializeCalibrationFeatures(calibrationRunFeatures)
        )
    }

    private fun parseVetVisits(jsonStr: String): List<VetVisit> {
        Log.d("DogFitRepository", "parseVetVisits input: $jsonStr")
        if (jsonStr.isEmpty() || jsonStr == "[]") return emptyList()
        return try {
            val json = JSONArray(jsonStr)
            val list = mutableListOf<VetVisit>()
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                list.add(VetVisit(
                    id = obj.getLong("id"),
                    date = Date(obj.getLong("date")),
                    reason = obj.getString("reason"),
                    clinicName = obj.optString("clinicName", ""),
                    prescriptionImageUrl = obj.optString("prescriptionImageUrl", ""),
                    notes = obj.optString("notes", "")
                ))
            }
            Log.d("DogFitRepository", "parseVetVisits result: ${list.size} visitas")
            list
        } catch (e: Exception) {
            Log.e("DogFitRepository", "parseVetVisits error: ${e.message}")
            emptyList()
        }
    }

    private fun serializeVetVisits(visits: List<VetVisit>): String {
        val jsonArray = JSONArray()
        visits.forEach { visit ->
            val obj = JSONObject()
            obj.put("id", visit.id)
            obj.put("date", visit.date.time)
            obj.put("reason", visit.reason)
            obj.put("clinicName", visit.clinicName)
            obj.put("prescriptionImageUrl", visit.prescriptionImageUrl)
            obj.put("notes", visit.notes)
            jsonArray.put(obj)
        }
        val result = jsonArray.toString()
        Log.d("DogFitRepository", "serializeVetVisits: ${visits.size} visitas -> $result")
        return result
    }

    private fun parseWeightHistory(json: String): List<WeightRecord> {
        if (json.isEmpty() || json == "[]") return emptyList()
        return try {
            val list = mutableListOf<WeightRecord>()
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(WeightRecord(
                    timestamp = obj.getLong("timestamp"),
                    weight = obj.getDouble("weight").toFloat()
                ))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeWeightHistory(list: List<WeightRecord>): String {
        val jsonArray = JSONArray()
        list.forEach { record ->
            val obj = JSONObject()
            obj.put("timestamp", record.timestamp)
            obj.put("weight", record.weight.toDouble())
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun VaccinationEntity.toVaccination(): Vaccination {
        return Vaccination(
            id = id,
            name = name,
            applicationDate = applicationDate,
            nextDueDate = nextDueDate,
            veterinarianName = veterinarianName,
            clinic = clinic,
            batchNumber = batchNumber,
            notes = notes
        )
    }

    private fun Vaccination.toEntity(): VaccinationEntity {
        return VaccinationEntity(
            id = id,
            name = name,
            applicationDate = applicationDate,
            nextDueDate = nextDueDate,
            veterinarianName = veterinarianName,
            clinic = clinic,
            batchNumber = batchNumber,
            notes = notes
        )
    }

    private fun DewormingEntity.toDeworming(): Deworming {
        return Deworming(
            id = id,
            productName = productName,
            applicationDate = applicationDate,
            nextDueDate = nextDueDate,
            dosage = dosage,
            weight = weight,
            veterinarianName = veterinarianName,
            notes = notes
        )
    }

    private fun Deworming.toEntity(): DewormingEntity {
        return DewormingEntity(
            id = id,
            productName = productName,
            applicationDate = applicationDate,
            nextDueDate = nextDueDate,
            dosage = dosage,
            weight = weight,
            veterinarianName = veterinarianName,
            notes = notes
        )
    }
}
