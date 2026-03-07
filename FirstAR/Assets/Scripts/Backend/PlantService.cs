using System;
using System.Collections.Generic;
using UnityEngine;

/// <summary>
/// Handles plant recommendations, planting questions, and "My Crops" features.
/// </summary>
public class PlantService : MonoBehaviour
{
    public static PlantService Instance { get; private set; }

    private void Awake()
    {
        if (Instance != null && Instance != this) { Destroy(gameObject); return; }
        Instance = this;
    }

    // ──────────── Plant Recommendations ────────────

    /// <summary>Get the planting questions to display to the user.</summary>
    public void GetQuestions(Action<PlantQuestionsResponse> onSuccess, Action<string> onError = null)
    {
        ApiClient.Instance.Get(ApiClient.Instance.Config.plantQuestions, response =>
        {
            if (response.isSuccess)
                onSuccess?.Invoke(response.Parse<PlantQuestionsResponse>());
            else
                onError?.Invoke(response.error ?? "Failed to get questions");
        });
    }

    /// <summary>Submit answers and get plant recommendations.</summary>
    public void SubmitAnswers(Dictionary<string, string> answers, int minMatch,
        Action<RecommendationResponse> onSuccess, Action<string> onError = null)
    {
        // JsonUtility doesn't support Dictionary, so we build JSON manually
        var parts = new List<string>();
        foreach (var kv in answers)
            parts.Add($"\"{kv.Key}\":\"{kv.Value}\"");

        string json = $"{{\"answers\":{{{string.Join(",", parts)}}},\"minMatchPercentage\":{minMatch}}}";

        ApiClient.Instance.PostRaw(ApiClient.Instance.Config.plantSubmitAnswers, json, response =>
        {
            if (response.isSuccess)
                onSuccess?.Invoke(response.Parse<RecommendationResponse>());
            else
                onError?.Invoke(response.error ?? "Failed to get recommendations");
        });
    }

    // ──────────── My Crops ────────────

    /// <summary>Get overview (planned/planted/harvested counts).</summary>
    public void GetCropsOverview(Action<CropsOverview> onSuccess, Action<string> onError = null)
    {
        ApiClient.Instance.Get(ApiClient.Instance.Config.myCropsOverview, response =>
        {
            if (response.isSuccess)
                onSuccess?.Invoke(response.Parse<CropsOverview>());
            else
                onError?.Invoke(response.error ?? "Failed to get crops");
        });
    }

    /// <summary>Get planned crops list.</summary>
    public void GetPlannedCrops(Action<UserPlantListResponse> onSuccess, Action<string> onError = null)
    {
        ApiClient.Instance.Get(ApiClient.Instance.Config.myCropsPlanned, response =>
        {
            if (response.isSuccess)
                onSuccess?.Invoke(response.Parse<UserPlantListResponse>());
            else
                onError?.Invoke(response.error ?? "Failed to get planned crops");
        });
    }

    /// <summary>Get planted crops list.</summary>
    public void GetPlantedCrops(Action<UserPlantListResponse> onSuccess, Action<string> onError = null)
    {
        ApiClient.Instance.Get(ApiClient.Instance.Config.myCropsPlanted, response =>
        {
            if (response.isSuccess)
                onSuccess?.Invoke(response.Parse<UserPlantListResponse>());
            else
                onError?.Invoke(response.error ?? "Failed to get planted crops");
        });
    }

    /// <summary>Add a plant to My Crops (planned).</summary>
    public void AddPlantToCrops(long plantId, Action<bool, string> callback)
    {
        string endpoint = $"/api/user/my-crops/add/{plantId}";
        ApiClient.Instance.Post(endpoint, null, response =>
        {
            callback?.Invoke(response.isSuccess,
                response.isSuccess ? "Plant added" : response.error ?? "Failed");
        });
    }
}

// ──────────── DTOs ────────────

[Serializable]
public class PlantQuestionsResponse
{
    public PlantQuestion[] questions;
}

[Serializable]
public class PlantQuestion
{
    public long id;
    public string questionKey;
    public string questionTextAr;
    public string questionTextEn;
    public QuestionOption[] options;
}

[Serializable]
public class QuestionOption
{
    public long id;
    public string optionKey;
    public string optionTextAr;
    public string optionTextEn;
}

[Serializable]
public class RecommendationResponse
{
    public string sessionId;
    public PlantRecommendation[] recommendations;
}

[Serializable]
public class PlantRecommendation
{
    public long plantId;
    public string nameAr;
    public string nameEn;
    public float matchPercentage;
    public string matchLevel;
    public string summaryAr;
    public string imageUrl;
}

[Serializable]
public class CropsOverview
{
    public int plannedCount;
    public int plantedCount;
    public int harvestedCount;
    public int totalCount;
}

[Serializable]
public class UserPlantListResponse
{
    public UserPlantItem[] plants;
}

[Serializable]
public class UserPlantItem
{
    public long id;
    public long plantId;
    public string plantNameAr;
    public string plantNameEn;
    public string status;
    public string imageUrl;
}
